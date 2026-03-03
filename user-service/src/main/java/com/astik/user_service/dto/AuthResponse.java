package com.astik.user_service.dto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        Long userId,
        String email,
        String role
) {}
