package com.maher.booking_system.dto;

public record AdminBookingFilterRequest(
        String startDateTime,
        String endDateTime,
        String status,
        Long carId,
        Long userId,
        String paymentStatus
) {
}
