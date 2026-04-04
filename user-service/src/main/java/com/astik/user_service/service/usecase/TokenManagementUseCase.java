package com.astik.user_service.service.usecase;

import com.astik.user_service.dto.response.AuthResponse;

public interface TokenManagementUseCase {
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}
