package com.maher.booking_system.dto;

import java.util.List;

public record UpdateUserRequest(
        String firstName,
        String lastName,
        String address,
        String birthDate,
        String avatarUrl,
        List<String> paymentMethods
) {
}
