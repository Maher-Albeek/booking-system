package com.maher.booking_system.dto;

import java.time.LocalDateTime;

public record OfferAnalyticsResponse(
        Long offerId,
        String title,
        boolean enabled,
        boolean activeNow,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        int linkedCarsCount,
        long bookingsCount,
        long revenueCents
) {
}
