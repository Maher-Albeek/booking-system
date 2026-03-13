package com.maher.booking_system.dto;

import jakarta.validation.constraints.NotBlank;

public class BookingStatusUpdateRequest {

    @NotBlank(message = "status is required")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
