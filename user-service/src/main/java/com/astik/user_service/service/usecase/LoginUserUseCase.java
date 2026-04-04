package com.astik.user_service.service.usecase;

import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.response.AuthResponse;

public interface LoginUserUseCase {
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
}
