package com.maher.booking_system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BookingNotificationResponse(
        String type,
        String subject,
        String recipient,
        LocalDateTime sentAt,
        String message,
        List<BookingNotificationAttachmentResponse> attachments
) {
}
