package com.maher.booking_system.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateCheckoutSessionRequest {
    @NotNull(message = "booking is required")
    @Valid
    private CreateBookingRequest booking;

    @NotBlank(message = "successUrl is required")
    private String successUrl;

    @NotBlank(message = "cancelUrl is required")
    private String cancelUrl;

    private boolean savePaymentMethod;

    public CreateBookingRequest getBooking() { return booking; }
    public void setBooking(CreateBookingRequest booking) { this.booking = booking; }

    public String getSuccessUrl() { return successUrl; }
    public void setSuccessUrl(String successUrl) { this.successUrl = successUrl; }

    public String getCancelUrl() { return cancelUrl; }
    public void setCancelUrl(String cancelUrl) { this.cancelUrl = cancelUrl; }

    public boolean isSavePaymentMethod() { return savePaymentMethod; }
    public void setSavePaymentMethod(boolean savePaymentMethod) { this.savePaymentMethod = savePaymentMethod; }
}
