package com.maher.booking_system.dto;

import jakarta.validation.constraints.NotNull;

public record ResourceFavoriteRequest(
        @NotNull(message = "userId is required")
        Long userId
) {
}
