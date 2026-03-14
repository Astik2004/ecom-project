package com.astik.user_service.dto.response;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        Long expiresIn,       // seconds
        UserResponse user
) {
    public static AuthResponse of(String accessToken, String refreshToken,
                                  Long expiresIn, UserResponse user) {
        return new AuthResponse(accessToken, refreshToken, "Bearer", expiresIn, user);
    }
}