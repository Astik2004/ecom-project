package com.astik.user_service.service.usecase.impl;

import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.entity.RefreshToken;
import com.astik.user_service.entity.User;
import com.astik.user_service.enums.AuditAction;
import com.astik.user_service.exception.InvalidTokenException;
import com.astik.user_service.repository.RefreshTokenRepository;
import com.astik.user_service.security.JwtUtils;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.AuditService;
import com.astik.user_service.service.usecase.AuthHelper;
import com.astik.user_service.service.usecase.TokenManagementUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenManagementUseCaseImpl implements TokenManagementUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtils jwtUtils;
    private final AuditService auditService;
    private final AuthHelper authHelper;

    @Override
    @Transactional
    public AuthResponse refreshToken(String incomingRefreshToken) {

        RefreshToken stored = refreshTokenRepository
                .findByToken(incomingRefreshToken)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

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

        String newAccessToken = jwtUtils.generateAccessToken(principal);
        String newRefreshToken = jwtUtils.generateRefreshToken(principal);
        authHelper.persistRefreshToken(user, newRefreshToken);

        auditService.log(AuditAction.TOKEN_REFRESHED, "User", user.getId(), user.getEmail(), null, null, true, null);

        log.info("Token refreshed | userId={}", user.getId());
        return authHelper.buildAuthResponse(newAccessToken, newRefreshToken, principal);
    }

    @Override
    @Transactional
    public void logout(String refreshTokenStr) {
        refreshTokenRepository.findByToken(refreshTokenStr)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    token.setExpired(true);
                    auditService.log(AuditAction.USER_LOGOUT, "User", token.getUser().getId(), token.getUser().getEmail(), null, null, true, null);
                    log.info("User logged out | userId={}", token.getUser().getId());
                });
    }
}
