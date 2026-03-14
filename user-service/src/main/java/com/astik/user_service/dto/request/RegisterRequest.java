package com.astik.user_service.dto.request;

import com.astik.user_service.enums.Role;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "First name is required")
        @Size(min = 2, max = 50, message = "First name must be 2-50 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(min = 2, max = 50, message = "Last name must be 2-50 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email format")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 8, message = "Password must be at least 8 characters")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&]).+$",
                message = "Password must contain uppercase, lowercase, digit and special character"
        )
        String password,

        @Pattern(regexp = "^[+]?[0-9]{10,15}$", message = "Invalid phone number")
        String phoneNumber,

        @NotNull(message = "Role is required")
        Role role
) {}