package com.maher.booking_system.dto;

public record AuthLoginRequest(
        String identifier,
        String password
) {
}
