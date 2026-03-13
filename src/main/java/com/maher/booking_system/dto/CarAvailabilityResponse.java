package com.maher.booking_system.dto;

public record CarAvailabilityResponse(
        Long resourceId,
        boolean available
) {
}
