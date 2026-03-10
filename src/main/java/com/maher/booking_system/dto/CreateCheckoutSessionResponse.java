package com.maher.booking_system.dto;

public record CreateCheckoutSessionResponse(
        Long bookingId,
        String paymentStatus,
        String checkoutSessionId,
        String checkoutUrl
) {
}
