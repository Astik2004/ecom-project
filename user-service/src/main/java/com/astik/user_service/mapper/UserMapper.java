package com.astik.user_service.mapper;

import com.astik.user_service.dto.UserResponse;
import com.astik.user_service.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    // Normal User Response (Sensitive info hidden/null)
    public UserResponse toResponse(User user) {
        if (user == null) return null;

        return new UserResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getStatus(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(), // derived from method
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getProfilePictureUrl(),
                user.getIsEmailVerified(),
                user.getIsPhoneVerified(),
                user.getEmailVerificationToken(),
                user.getLastLoginAt(),

                // Admin fields left null for regular users
                null,
                null,
                null
        );
    }

    // Admin Response (Exposes lockout and attempt info)
    public UserResponse toAdminResponse(User user) {
        if (user == null) return null;

        return new UserResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getStatus(),
                user.getFirstName(),
                user.getLastName(),
                user.getFullName(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRole(),
                user.getProfilePictureUrl(),
                user.getIsEmailVerified(),
                user.getIsPhoneVerified(),
                user.getEmailVerificationToken(),
                user.getLastLoginAt(),

                // Exposing Admin fields
                user.isAccountLocked(), // derived from method
                user.getFailedLoginAttempts(),
                user.getAccountLockedUntil()
        );
    }
}