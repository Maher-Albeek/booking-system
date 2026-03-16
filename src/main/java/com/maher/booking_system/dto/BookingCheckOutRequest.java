package com.maher.booking_system.dto;

import java.util.List;

public record BookingCheckOutRequest(
        Integer returnOdometerKm,
        String actualReturnDateTime,
        String returnNotes,
        List<String> returnPhotoUrls,
        List<DamageReportRequest> damageReports
) {
    public record DamageReportRequest(String type, String notes, Long feeCents) {
    }
}
