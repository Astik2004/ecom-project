package com.astik.user_service.dto;

import com.astik.user_service.enums.Role;
import com.astik.user_service.enums.UserStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record UserResponse(
        UUID id,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        UserStatus status,
        String firstName,
        String lastName,
        String fullName, // from getFullName()
        String email,
        String phoneNumber,
        Role role,
        String profilePictureUrl,
        Boolean isEmailVerified,
        Boolean isPhoneVerified,
        LocalDateTime lastLoginAt,
        Boolean accountLocked, // from isAccountLocked()
        Integer failedLoginAttempts,
        LocalDateTime accountLockedUntil
) {}
