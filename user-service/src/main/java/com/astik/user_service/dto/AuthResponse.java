package com.astik.user_service.dto;

import lombok.Builder;

@Builder
public record AuthResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        UserResponse user,
        String message
) {}
