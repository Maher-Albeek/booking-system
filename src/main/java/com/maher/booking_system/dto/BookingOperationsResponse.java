package com.maher.booking_system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BookingOperationsResponse(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        Long resourceId,
        String resourceName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String status,
        LocalDateTime bookingTime,
        String customerName,
        String serviceName,
        String paymentMethod,
        String paymentStatus,
        Long payableAmountCents,
        String payableCurrency,
        Integer pickupOdometerKm,
        String pickupFuelLevel,
        String pickupNotes,
        List<String> pickupPhotoUrls,
        LocalDateTime checkedInAt,
        Integer returnOdometerKm,
        LocalDateTime actualReturnDateTime,
        String returnNotes,
        List<String> returnPhotoUrls,
        List<DamageReportResponse> damageReports,
        Long extraKmFeeCents,
        Long lateFeeCents,
        Long damageFeeCents,
        Long finalTotalAmountCents,
        String finalInvoiceNumber,
        LocalDateTime finalInvoiceIssuedAt
) {
    public record DamageReportResponse(String type, String notes, Long feeCents) {
    }
}
