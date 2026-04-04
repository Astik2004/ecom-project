package com.astik.user_service.service.usecase.impl;

import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.exception.AccountInactiveException;
import com.astik.user_service.exception.AccountLockedException;
import com.astik.user_service.exception.InvalidCredentialsException;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.usecase.AuthHelper;
import com.astik.user_service.service.usecase.LoginUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoginUserUseCaseImpl implements LoginUserUseCase {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final AuthHelper authHelper;

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        log.debug("Login attempt | email={} attempts={}", user.getEmail(), user.getFailedLoginAttempts());

        if (user.isAccountLocked()) {
            log.warn("Account locked | email={} until={}", user.getEmail(), user.getAccountLockedUntil());
            throw new AccountLockedException("Account locked until " + user.getAccountLockedUntil());
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            authHelper.handleFailedLogin(user, ipAddress, userAgent);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new AccountInactiveException("Account is not active");
        }

        userRepository.updateFailedAttempts(user.getId(), 0, null);
        userRepository.updateLastLogin(user.getId(), LocalDateTime.now());

        refreshTokenRepository.revokeAllUserTokens(user);

        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtUtils.generateAccessToken(principal);
        String refreshTokenStr = jwtUtils.generateRefreshToken(principal);
        authHelper.persistRefreshToken(user, refreshTokenStr);

        auditService.log(AuditAction.USER_LOGIN, "User", user.getId(), user.getEmail(), ipAddress, userAgent, true, null);

        log.info("Login successful | userId={}", user.getId());
        return authHelper.buildAuthResponse(accessToken, refreshTokenStr, principal);
    }
}
