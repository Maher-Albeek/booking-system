package com.maher.booking_system.dto;

import jakarta.validation.constraints.NotBlank;

public class ExtendBookingRequest {
    @NotBlank(message = "newEndDateTime is required")
    private String newEndDateTime;
    private boolean paymentConfirmed;

    public String getNewEndDateTime() { return newEndDateTime; }
    public void setNewEndDateTime(String newEndDateTime) { this.newEndDateTime = newEndDateTime; }
    public boolean isPaymentConfirmed() { return paymentConfirmed; }
    public void setPaymentConfirmed(boolean paymentConfirmed) { this.paymentConfirmed = paymentConfirmed; }
}
