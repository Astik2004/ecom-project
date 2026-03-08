package com.astik.user_service.controller;

import com.astik.user_service.dto.AuthResponse;
import com.astik.user_service.dto.LoginRequest;
import com.astik.user_service.dto.RegisterRequest;
import com.astik.user_service.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and session management")
public class AuthController {

    private final AuthService authService;

    // ─── Register ─────────────────────────────────────────────────────────────
    @Operation(summary = "Register a new user", description = "Creates a new user account, assigns CUSTOMER role, and initiates email verification.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User successfully registered"),
            @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
            @ApiResponse(responseCode = "409", description = "Email or phone number already exists")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.register(request, httpRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ─── Login ────────────────────────────────────────────────────────────────
    @Operation(summary = "Authenticate user", description = "Authenticates a user using email and password, returning JWT access and refresh tokens.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully authenticated"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account is disabled or locked")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {

        AuthResponse response = authService.login(request, httpRequest);
        return ResponseEntity.ok(response);
    }

    // ─── Logout ───────────────────────────────────────────────────────────────
    @Operation(summary = "Logout user", description = "Revokes the active refresh token to securely end the user session.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully logged out")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(
            @RequestParam("refreshToken") String refreshToken,
            HttpServletRequest httpRequest) {

        authService.logout(refreshToken, httpRequest);
        return ResponseEntity.ok(Map.of("message", "User logged out successfully"));
    }

    // ─── Verify Email ─────────────────────────────────────────────────────────
    @Operation(summary = "Verify user email", description = "Activates the user account using the verification token sent to their email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Email successfully verified"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token")
    })
    @GetMapping("/verify-email")
    public ResponseEntity<Map<String, String>> verifyEmail(
            @RequestParam("token") String token,
            HttpServletRequest httpRequest) {

        authService.verifyEmail(token, httpRequest);
        return ResponseEntity.ok(Map.of("message", "Email verified successfully"));
    }
}