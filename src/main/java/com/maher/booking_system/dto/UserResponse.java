package com.maher.booking_system.dto;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role
) {
}
