package com.maher.booking_system.dto;

import java.util.List;

public record UserResponse(
        Long id,
        String name,
        String email,
        String role,
        String firstName,
        String lastName,
        String address,
        String addressStreet,
        String addressHouseNumber,
        String addressPostalCode,
        String addressCity,
        String addressCountry,
        String birthDate,
        String avatarUrl,
        List<String> paymentMethods
) {
}
