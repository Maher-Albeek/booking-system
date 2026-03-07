package com.maher.booking_system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthResetPasswordRequest(
        @NotBlank(message = "identifier is required")
        String identifier,
        @NotBlank(message = "newPassword is required")
        @Size(min = 6, message = "password must be at least 6 characters long")
        String newPassword
) {
}
