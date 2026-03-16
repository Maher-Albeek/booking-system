package com.maher.booking_system.dto;

import java.util.List;

public record BookingCheckInRequest(
        Integer pickupOdometerKm,
        String pickupFuelLevel,
        String pickupNotes,
        List<String> pickupPhotoUrls
) {
}
