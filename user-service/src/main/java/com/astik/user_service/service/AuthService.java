package com.astik.user_service.service;

import com.astik.user_service.dto.AuthResponse;
import com.astik.user_service.dto.LoginRequest;
import com.astik.user_service.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {

    // ─── Register ─────────────────────────────────────────────────────────────
    AuthResponse register(RegisterRequest request, HttpServletRequest httpRequest);

    // ─── Login ────────────────────────────────────────────────────────────────
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);

    // ─── Logout ───────────────────────────────────────────────────────────────
    void logout(String refreshToken, HttpServletRequest httpRequest);

    // ─── Verify Email ─────────────────────────────────────────────────────────
    void verifyEmail(String token, HttpServletRequest httpRequest);

}