package com.astik.user_service.controller;

import com.astik.user_service.dto.request.UpdateProfileRequest;
import com.astik.user_service.dto.response.ApiResponse;
import com.astik.user_service.dto.response.UserResponse;
import com.astik.user_service.security.UserPrincipal;
import com.astik.user_service.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            // Spring injects UserPrincipal because your filter sets it in context
            @AuthenticationPrincipal UserPrincipal principal) {

        return ResponseEntity.ok(
                ApiResponse.success("Profile fetched",
                        userService.getMyProfile(principal)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Profile updated",
                        userService.updateMyProfile(principal, request)));
    }

    // Example of role-based access — only ADMIN can view any user
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @PathVariable String id) {
        // implement when needed
        return ResponseEntity.ok(ApiResponse.success("ok", null));
    }
}