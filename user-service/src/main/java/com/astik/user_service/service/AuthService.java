// service/AuthService.java
package com.astik.user_service.service;

import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request, String ipAddress, String userAgent);
    AuthResponse refreshToken(String refreshToken);
    void logout(String refreshToken);
}