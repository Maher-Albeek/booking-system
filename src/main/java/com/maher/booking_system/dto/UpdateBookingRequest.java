package com.maher.booking_system.dto;

public class UpdateBookingRequest {
    private String status;
    private String endDateTime;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEndDateTime() { return endDateTime; }
    public void setEndDateTime(String endDateTime) { this.endDateTime = endDateTime; }
}
