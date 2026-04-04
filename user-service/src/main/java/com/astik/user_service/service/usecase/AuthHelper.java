package com.astik.user_service.service.usecase;

import com.astik.user_service.config.AccountProperties;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.mapper.UserMapper;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.repository.UserRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthHelper {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    private final UserMapper userMapper;
    private final AccountProperties accountProperties;
    private final AuditService auditService;

    public void persistRefreshToken(User user, String tokenStr) {
        RefreshToken token = RefreshToken.builder()
                .token(tokenStr)
                .user(user)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtils.getRefreshExpiration() / 1000))
                .revoked(false)
                .expired(false)
                .build();
        refreshTokenRepository.save(token);
    }

    public AuthResponse buildAuthResponse(String accessToken, String refreshToken, UserPrincipal principal) {
        return AuthResponse.of(
                accessToken,
                refreshToken,
                jwtUtils.getAccessExpiration() / 1000,
                userMapper.toResponse(principal.getUser())
        );
    }

    public void handleFailedLogin(User user, String ipAddress, String userAgent) {
        LocalDateTime lockUntil = LocalDateTime.now()
                .plusMinutes(accountProperties.getLockDurationMinutes());

        userRepository.incrementFailedAttempts(
                user.getId(),
                accountProperties.getMaxFailedAttempts(),
                lockUntil);

        int currentAttempts = userRepository.getFailedAttempts(user.getId());

        log.warn("Failed login {}/{} | email={}", currentAttempts, accountProperties.getMaxFailedAttempts(), user.getEmail());

        if (currentAttempts >= accountProperties.getMaxFailedAttempts()) {
            log.warn("Account LOCKED | email={} until={} after {} attempts", user.getEmail(), lockUntil, currentAttempts);
            auditService.log(AuditAction.ACCOUNT_LOCKED, "User", user.getId(), user.getEmail(), ipAddress, userAgent, false, "Locked after " + currentAttempts + " failed attempts");
        } else {
            auditService.log(AuditAction.USER_LOGIN_FAILED, "User", user.getId(), user.getEmail(), ipAddress, userAgent, false, "Bad credentials attempt " + currentAttempts + "/" + accountProperties.getMaxFailedAttempts());
        }
    }
}
