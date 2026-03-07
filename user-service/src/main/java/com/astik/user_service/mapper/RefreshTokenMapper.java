package com.astik.user_service.mapper;

import com.astik.user_service.dto.RefreshTokenResponse;
import com.astik.user_service.entity.RefreshToken;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {

    public RefreshTokenResponse toResponse(RefreshToken refreshToken) {
        if (refreshToken == null) return null;

        return new RefreshTokenResponse(
                refreshToken.getId(),
                maskToken(refreshToken.getToken()), // Token masking for security
                refreshToken.isRevoked(),
                refreshToken.isExpired(),
                refreshToken.isValid(), // entity ka method map kiya hai
                refreshToken.getExpiresAt(),
                refreshToken.getUser() != null ? refreshToken.getUser().getId() : null, // Safely fetching user UUID
                refreshToken.getCreatedAt()
        );
    }

    private String maskToken(String token) {
        if (token == null || token.length() <= 10) return token;
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}