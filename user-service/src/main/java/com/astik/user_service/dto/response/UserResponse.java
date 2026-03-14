package com.astik.user_service.dto.response;

import com.astik.user_service.enums.Role;
import com.astik.user_service.enums.UserStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        Role role,
        UserStatus status,
        String profilePictureUrl,
        Boolean isEmailVerified,
        Boolean isPhoneVerified,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {}