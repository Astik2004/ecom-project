package com.astik.user_service.service.usecase;

import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.response.AuthResponse;

public interface RegisterUserUseCase {
    AuthResponse register(RegisterRequest request);
}
