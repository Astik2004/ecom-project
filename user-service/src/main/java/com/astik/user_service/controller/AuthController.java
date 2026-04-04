package com.astik.user_service.controller;

import com.astik.user_service.dto.request.ForgotPasswordRequest;
import com.astik.user_service.dto.request.LoginRequest;
import com.astik.user_service.dto.request.RegisterRequest;
import com.astik.user_service.dto.request.ResetPasswordRequest;
import com.astik.user_service.dto.response.ApiResponse;
import com.astik.user_service.dto.response.AuthResponse;
import com.astik.user_service.service.usecase.AccountRecoveryUseCase;
import com.astik.user_service.service.usecase.LoginUserUseCase;
import com.astik.user_service.service.usecase.RegisterUserUseCase;
import com.astik.user_service.service.usecase.TokenManagementUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUserUseCase loginUserUseCase;
    private final TokenManagementUseCase tokenManagementUseCase;
    private final AccountRecoveryUseCase accountRecoveryUseCase;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request) {

        AuthResponse data = registerUserUseCase.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", data));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse data = loginUserUseCase.login(
                request,
                httpRequest.getRemoteAddr(),
                httpRequest.getHeader("User-Agent")
        );
        return ResponseEntity.ok(ApiResponse.success("Login successful", data));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(
            @RequestHeader("Refresh-Token") String refreshToken) {

        return ResponseEntity.ok(
                ApiResponse.success("Token refreshed",
                        tokenManagementUseCase.refreshToken(refreshToken)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader("Refresh-Token") String refreshToken) {

        tokenManagementUseCase.logout(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @RequestParam String token) {
        accountRecoveryUseCase.verifyEmail(token);
        return ResponseEntity.ok(
                ApiResponse.success("Email verified successfully"));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @RequestParam String email) {
        accountRecoveryUseCase.resendVerificationEmail(email);
        return ResponseEntity.ok(
                ApiResponse.success("Verification email sent"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        accountRecoveryUseCase.forgotPassword(request.email());
        return ResponseEntity.ok(
                ApiResponse.success(
                        "If this email exists, a reset link has been sent"));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        accountRecoveryUseCase.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Password reset successfully"));
    }
}