package com.astik.user_service.service.impl;

import com.astik.user_service.config.AccountProperties;
import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.enums.UserStatus;
import com.astik.user_service.exception.*;
import com.astik.user_service.kafkaevent.UserEventProducer;
import com.astik.user_service.kafkaevent.UserRegisteredEvent;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository            userRepository;
    private final RefreshTokenRepository    refreshTokenRepository;
    private final UserMapper                userMapper;
    private final PasswordEncoder           passwordEncoder;
    private final JwtUtils                  jwtUtils;           // YOUR JwtUtils
    private final AccountProperties accountProperties;
    private final AuthenticationManager     authenticationManager;
    private final UserEventProducer userEventProducer;
    private final AuditService  auditService;


    // ─────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        // 1. Uniqueness checks
        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "Email already registered: " + request.email());
        }
        if (request.phoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException(
                    "Phone number already registered");
        }

        // 2. Build entity from DTO (mapper ignores password — set manually)
        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);
        User saved = userRepository.save(user);

        // 3. Wrap in UserPrincipal — YOUR token generator needs this
        UserPrincipal principal = new UserPrincipal(saved);

        // 4. Generate both tokens using YOUR JwtUtils
        String accessToken  = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr = jwtUtils.generateRefreshToken(principal);

        // 5. Persist refresh token
        persistRefreshToken(saved, refreshTokenStr);

        // 6. Publish Kafka event  (fires after DB commit — @Transactional)
        userEventProducer.publishUserRegistered(
                UserRegisteredEvent.of(
                        saved.getId(),
                        saved.getFirstName(),
                        saved.getLastName(),
                        saved.getEmail(),
                        saved.getRole().name()
                )
        );

        // 7. Async audit log
        auditService.log(
                AuditAction.USER_REGISTERED, "User",
                saved.getId(), saved.getEmail(),
                null, null, true, null
        );

        log.info("User registered | userId={} email={} role={}",
                saved.getId(), saved.getEmail(), saved.getRole());

        return buildAuthResponse(accessToken, refreshTokenStr, principal);
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request,
                              String ipAddress, String userAgent) {

        // 1. Load user — fail fast with generic message (don't leak existence)
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        // 2. Manual lock check before Spring Security (gives us a better error)
        if (user.isAccountLocked()) {
            throw new AccountLockedException(
                    "Account locked until " + user.getAccountLockedUntil());
        }

        // 3. Delegate to Spring Security — this triggers UserPrincipal.isEnabled()
        //    and isAccountNonLocked() as well
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email(), request.password())
            );
        } catch (BadCredentialsException ex) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid email or password");
        } catch (LockedException ex) {
            throw new AccountLockedException(
                    "Account locked until " + user.getAccountLockedUntil());
        } catch (DisabledException ex) {
            throw new AccountInactiveException("Account is not active");
        }

        // 4. Auth passed — reset failure counter
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        // 5. Revoke all previous refresh tokens for this user
        refreshTokenRepository.revokeAllUserTokens(user);

        // 6. Build principal and issue NEW tokens
        UserPrincipal principal   = new UserPrincipal(user);
        String accessToken        = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr    = jwtUtils.generateRefreshToken(principal);
        persistRefreshToken(user, refreshTokenStr);

        auditService.log(AuditAction.USER_LOGIN, "User",
                user.getId(), user.getEmail(),
                ipAddress, userAgent, true, null);

        log.info("User logged in | userId={} email={}", user.getId(), user.getEmail());

        return buildAuthResponse(accessToken, refreshTokenStr, principal);
    }

    // ─────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse refreshToken(String incomingRefreshToken) {

        // 1. Find stored token
        RefreshToken stored = refreshTokenRepository
                .findByToken(incomingRefreshToken)
                .orElseThrow(() ->
                        new InvalidTokenException("Refresh token not found"));

        // 2. Validate stored state (not revoked, not expired)
        if (!stored.isValid()) {
            throw new InvalidTokenException(
                    "Refresh token is expired or revoked");
        }

        // 3. Load user and validate JWT signature + type using YOUR JwtUtils
        User user = stored.getUser();
        UserPrincipal principal = new UserPrincipal(user);

        if (!jwtUtils.isRefreshTokenValid(incomingRefreshToken, principal)) {
            // Token was tampered — revoke everything for this user
            refreshTokenRepository.revokeAllUserTokens(user);
            throw new InvalidTokenException("Refresh token is invalid");
        }

        // 4. Rotate — revoke old, issue new
        stored.setRevoked(true);
        stored.setExpired(true);

        String newAccessToken  = jwtUtils.generateAccessToken(principal);
        String newRefreshToken = jwtUtils.generateRefreshToken(principal);
        persistRefreshToken(user, newRefreshToken);

        auditService.log(AuditAction.TOKEN_REFRESHED, "User",
                user.getId(), user.getEmail(),
                null, null, true, null);

        log.info("Token refreshed | userId={}", user.getId());

        return buildAuthResponse(newAccessToken, newRefreshToken, principal);
    }

    // ─────────────────────────────────────────────────────────────
    // LOGOUT
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setExpired(true);

                    auditService.log(AuditAction.USER_LOGOUT, "User",
                            token.getUser().getId(),
                            token.getUser().getEmail(),
                            null, null, true, null);

                    log.info("User logged out | userId={}", token.getUser().getId());
                });
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private void persistRefreshToken(User user, String tokenStr) {
        RefreshToken token = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                // convert ms → seconds → LocalDateTime
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtUtils.getRefreshExpiration() / 1000))
                .revoked(false)
                .expired(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private void handleFailedLogin(User user,
                                   String ipAddress, String userAgent) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);

        if (attempts >= accountProperties.getMaxFailedAttempts()) {
            user.setAccountLockedUntil(
                    LocalDateTime.now().plusMinutes(accountProperties.getMaxFailedAttempts()));
            log.warn("Account locked after {} attempts | email={}",
                    attempts, user.getEmail());
            auditService.log(AuditAction.ACCOUNT_LOCKED, "User",
                    user.getId(), user.getEmail(),
                    ipAddress, userAgent, false,
                    "Account locked after " + attempts + " failed attempts");
        } else {
            auditService.log(AuditAction.USER_LOGIN_FAILED, "User",
                    user.getId(), user.getEmail(),
                    ipAddress, userAgent, false,
                    "Bad credentials — attempt " + attempts);
        }
        userRepository.save(user);
    }

    // Centralise AuthResponse construction — uses YOUR JwtUtils for expiry
    private AuthResponse buildAuthResponse(String accessToken,
                                           String refreshToken,
                                           UserPrincipal principal) {
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtUtils.getAccessExpiration() / 1000,   // ms → seconds
                userMapper.toResponse(principal.getUser())
        );
    }
}