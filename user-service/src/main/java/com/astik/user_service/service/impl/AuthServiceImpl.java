package com.astik.user_service.service.impl;

import com.astik.user_service.config.AccountProperties;
import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.request.ResetPasswordRequest;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.enums.UserStatus;
import com.astik.user_service.exception.*;
import com.astik.user_service.kafkaevent.EmailVerificationEvent;
import com.astik.user_service.kafkaevent.PasswordResetEvent;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    private final UserRepository         userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper             userMapper;
    private final PasswordEncoder        passwordEncoder;
    private final JwtUtils               jwtUtils;
    private final AccountProperties      accountProperties;
    private final UserEventProducer      userEventProducer;
    private final AuditService           auditService;

    @Value("${application.account.email-token-expiry-hours:24}")
    private int emailTokenExpiryHours;

    @Value("${application.account.password-reset-token-expiry-minutes:15}")
    private int passwordResetTokenExpiryMinutes;

    // ─────────────────────────────────────────────────────────────
    // REGISTER
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException(
                    "Email already registered: " + request.email());
        }
        if (request.phoneNumber() != null &&
                userRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new UserAlreadyExistsException("Phone number already registered");
        }

        User user = userMapper.toEntity(request);
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.ACTIVE);

        String verificationToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(verificationToken);
        user.setEmailVerificationTokenExpiry(
                LocalDateTime.now().plusHours(emailTokenExpiryHours));

        User saved = userRepository.save(user);
        UserPrincipal principal = new UserPrincipal(saved);

        String accessToken     = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr = jwtUtils.generateRefreshToken(principal);
        persistRefreshToken(saved, refreshTokenStr);

        userEventProducer.publishUserRegistered(
                UserRegisteredEvent.of(
                        saved.getId(), saved.getFirstName(),
                        saved.getLastName(), saved.getEmail(),
                        saved.getRole().name()));

        userEventProducer.publishEmailVerification(
                EmailVerificationEvent.of(
                        saved.getId(), saved.getEmail(),
                        saved.getFirstName(), verificationToken,
                        saved.getEmailVerificationTokenExpiry()));

        auditService.log(AuditAction.USER_REGISTERED, "User",
                saved.getId(), saved.getEmail(), null, null, true, null);

        log.info("User registered | userId={} email={}",
                saved.getId(), saved.getEmail());

        return buildAuthResponse(accessToken, refreshTokenStr, principal);
    }

    // ─────────────────────────────────────────────────────────────
    // LOGIN
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse login(LoginRequest request,
                              String ipAddress, String userAgent) {

        // 1. User load — fresh from DB
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() ->
                        new InvalidCredentialsException("Invalid email or password"));

        log.debug("Login attempt | email={} attempts={}",
                user.getEmail(), user.getFailedLoginAttempts());

        // 2. Lock check — DB se fresh data
        if (user.isAccountLocked()) {
            log.warn("Account locked | email={} until={}",
                    user.getEmail(), user.getAccountLockedUntil());
            throw new AccountLockedException(
                    "Account locked until " + user.getAccountLockedUntil());
        }

        // 3. Password check
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // 4. Active check
        if (!user.isActive()) {
            throw new AccountInactiveException("Account is not active");
        }

        // 5. Success — atomic reset + last login update
        userRepository.updateFailedAttempts(user.getId(), 0, null);
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        // 6. Revoke old tokens + issue new
        refreshTokenRepository.revokeAllUserTokens(user);

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken      = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr  = jwtUtils.generateRefreshToken(principal);
        persistRefreshToken(user, refreshTokenStr);

        auditService.log(AuditAction.USER_LOGIN, "User",
                user.getId(), user.getEmail(),
                ipAddress, userAgent, true, null);

        log.info("Login successful | userId={}", user.getId());
        return buildAuthResponse(accessToken, refreshTokenStr, principal);
    }

    // ─────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public AuthResponse refreshToken(String incomingRefreshToken) {

        RefreshToken stored = refreshTokenRepository
                .findByToken(incomingRefreshToken)
                .orElseThrow(() ->
                        new InvalidTokenException("Refresh token not found"));

        if (!stored.isValid()) {
            throw new InvalidTokenException("Refresh token is expired or revoked");
        }

        User user = stored.getUser();
        UserPrincipal principal = new UserPrincipal(user);

        if (!jwtUtils.isRefreshTokenValid(incomingRefreshToken, principal)) {
            refreshTokenRepository.revokeAllUserTokens(user);
            throw new InvalidTokenException("Refresh token is invalid");
        }

        stored.setRevoked(true);
        stored.setExpired(true);

        String newAccessToken  = jwtUtils.generateAccessToken(principal);
        String newRefreshToken = jwtUtils.generateRefreshToken(principal);
        persistRefreshToken(user, newRefreshToken);

        auditService.log(AuditAction.TOKEN_REFRESHED, "User",
                user.getId(), user.getEmail(), null, null, true, null);

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
                    log.info("User logged out | userId={}",
                            token.getUser().getId());
                });
    }

    // ─────────────────────────────────────────────────────────────
    // VERIFY EMAIL
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByEmailVerificationToken(token)
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid verification token"));

        if (user.getEmailVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Verification token expired");
        }

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email already verified");
        }

        user.setIsEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiry(null);
        userRepository.save(user);

        auditService.log(AuditAction.EMAIL_VERIFIED, "User",
                user.getId(), user.getEmail(), null, null, true, null);

        log.info("Email verified | userId={}", user.getId());
    }

    // ─────────────────────────────────────────────────────────────
    // RESEND VERIFICATION
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found"));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new IllegalStateException("Email already verified");
        }

        String newToken = UUID.randomUUID().toString();
        user.setEmailVerificationToken(newToken);
        user.setEmailVerificationTokenExpiry(
                LocalDateTime.now().plusHours(emailTokenExpiryHours));
        userRepository.save(user);

        userEventProducer.publishEmailVerification(
                EmailVerificationEvent.of(
                        user.getId(), user.getEmail(),
                        user.getFirstName(), newToken,
                        user.getEmailVerificationTokenExpiry()));

        log.info("Verification email resent | userId={}", user.getId());
    }

    // ─────────────────────────────────────────────────────────────
    // FORGOT PASSWORD
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void forgotPassword(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setPasswordResetToken(resetToken);
            user.setPasswordResetTokenExpiry(
                    LocalDateTime.now().plusMinutes(passwordResetTokenExpiryMinutes));
            userRepository.save(user);

            userEventProducer.publishPasswordReset(
                    PasswordResetEvent.of(
                            user.getId(), user.getEmail(),
                            user.getFirstName(), resetToken,
                            user.getPasswordResetTokenExpiry()));

            auditService.log(AuditAction.PASSWORD_RESET_REQUESTED, "User",
                    user.getId(), user.getEmail(), null, null, true, null);

            log.info("Password reset requested | userId={}", user.getId());
        });
    }

    // ─────────────────────────────────────────────────────────────
    // RESET PASSWORD
    // ─────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() ->
                        new InvalidTokenException("Invalid password reset token"));

        if (user.getPasswordResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Password reset token expired");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiry(null);
        user.setPasswordChangedAt(LocalDateTime.now());
        user.setFailedLoginAttempts(0);
        user.setAccountLockedUntil(null);

        refreshTokenRepository.revokeAllUserTokens(user);
        userRepository.save(user);

        auditService.log(AuditAction.PASSWORD_CHANGED, "User",
                user.getId(), user.getEmail(), null, null, true, null);

        log.info("Password reset successful | userId={}", user.getId());
    }

    // ─────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────

    private void handleFailedLogin(User user,
                                   String ipAddress, String userAgent) {

        LocalDateTime lockUntil = LocalDateTime.now()
                .plusMinutes(accountProperties.getLockDurationMinutes());

        // Atomic DB increment — race condition bilkul nahi hogi
        userRepository.incrementFailedAttempts(
                user.getId(),
                accountProperties.getMaxFailedAttempts(),
                lockUntil);

        // DB se fresh value read karo
        int currentAttempts = userRepository.getFailedAttempts(user.getId());

        log.warn("Failed login {}/{} | email={}",
                currentAttempts,
                accountProperties.getMaxFailedAttempts(),
                user.getEmail());

        if (currentAttempts >= accountProperties.getMaxFailedAttempts()) {
            log.warn("Account LOCKED | email={} until={} after {} attempts",
                    user.getEmail(), lockUntil, currentAttempts);
            auditService.log(AuditAction.ACCOUNT_LOCKED, "User",
                    user.getId(), user.getEmail(),
                    ipAddress, userAgent, false,
                    "Locked after " + currentAttempts + " failed attempts");
        } else {
            auditService.log(AuditAction.USER_LOGIN_FAILED, "User",
                    user.getId(), user.getEmail(),
                    ipAddress, userAgent, false,
                    "Bad credentials attempt " + currentAttempts + "/" +
                            accountProperties.getMaxFailedAttempts());
        }
    }

    private void persistRefreshToken(User user, String tokenStr) {
        RefreshToken token = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(
                        jwtUtils.getRefreshExpiration() / 1000))
                .revoked(false)
                .expired(false)
                .build();
        refreshTokenRepository.save(token);
    }

    private AuthResponse buildAuthResponse(String accessToken,
                                           String refreshToken,
                                           UserPrincipal principal) {
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtUtils.getAccessExpiration() / 1000,
                userMapper.toResponse(principal.getUser()));
    }
}