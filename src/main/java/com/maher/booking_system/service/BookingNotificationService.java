package com.maher.booking_system.service;

import com.maher.booking_system.dto.BookingNotificationAttachmentResponse;
import com.maher.booking_system.dto.BookingNotificationResponse;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class BookingNotificationService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BookingRepository bookingRepository;
    private final UsersRepository usersRepository;
    private final ResourcesRepository resourcesRepository;
    private final BookingDocumentService bookingDocumentService;

    public BookingNotificationService(
            BookingRepository bookingRepository,
            UsersRepository usersRepository,
            ResourcesRepository resourcesRepository,
            BookingDocumentService bookingDocumentService
    ) {
        this.bookingRepository = bookingRepository;
        this.usersRepository = usersRepository;
        this.resourcesRepository = resourcesRepository;
        this.bookingDocumentService = bookingDocumentService;
    }

    public Booking sendBookingConfirmation(Booking booking) {
        if (booking == null || booking.getConfirmationEmailSentAt() != null) {
            return booking;
        }

        String recipient = resolveRecipient(booking);
        if (recipient == null) {
            return booking;
        }

        booking.setConfirmationEmailRecipient(recipient);
        booking.setConfirmationEmailSentAt(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    public void dispatchDueReturnReminders() {
        LocalDateTime now = LocalDateTime.now();
        for (Booking booking : bookingRepository.findAll()) {
            if (!isReminderDue(booking, now)) {
                continue;
            }

            String recipient = booking.getConfirmationEmailRecipient();
            if (recipient == null || recipient.isBlank()) {
                recipient = resolveRecipient(booking);
            }
            if (recipient == null) {
                continue;
            }

            booking.setConfirmationEmailRecipient(recipient);
            booking.setReturnReminderSentAt(now);
            bookingRepository.save(booking);
        }
    }

    public List<BookingNotificationResponse> listNotifications(Booking booking) {
        List<BookingNotificationResponse> notifications = new ArrayList<>();
        Resources resource = resourcesRepository.findById(booking.getResourceId()).orElse(null);
        String vehicleName = resource == null ? "Booked car" : safe(resource.getName());
        String recipient = safeRecipient(booking);

        if (booking.getConfirmationEmailSentAt() != null) {
            notifications.add(new BookingNotificationResponse(
                    "BOOKING_CONFIRMATION",
                    "Booking confirmation #" + booking.getId(),
                    recipient,
                    booking.getConfirmationEmailSentAt(),
                    "Booking " + booking.getId()
                            + " confirmed for " + vehicleName
                            + ". Contract and receipt are attached or available via the links below.",
                    List.of(
                            new BookingNotificationAttachmentResponse(
                                    "Contract PDF",
                                    "/api/bookings/" + booking.getId() + "/documents/contract"
                            ),
                            new BookingNotificationAttachmentResponse(
                                    "Receipt PDF",
                                    "/api/bookings/" + booking.getId() + "/documents/receipt"
                            )
                    )
            ));
        }

        if (booking.getReturnReminderSentAt() != null) {
            String lateFeeRule = resource == null || resource.getLateFeePerHour() == null
                    ? "Late fee rules: standard policy applies."
                    : "Late fee rules: " + String.format(java.util.Locale.ROOT, "%.2f", resource.getLateFeePerHour())
                    + " per hour after " + formatDateTime(booking.getEndDateTime()) + ".";
            notifications.add(new BookingNotificationResponse(
                    "RETURN_REMINDER",
                    "Return reminder for booking #" + booking.getId(),
                    recipient,
                    booking.getReturnReminderSentAt(),
                    "Reminder: return " + vehicleName
                            + " for booking " + booking.getId()
                            + " by " + formatDateTime(booking.getEndDateTime())
                            + ". " + lateFeeRule,
                    List.of()
            ));
        }

        return notifications;
    }

    private boolean isReminderDue(Booking booking, LocalDateTime now) {
        if (booking == null || booking.getEndDateTime() == null || booking.getReturnReminderSentAt() != null) {
            return false;
        }

        BookingStatus status = booking.getStatus() == null ? BookingStatus.PENDING : booking.getStatus().canonical();
        if (status != BookingStatus.PENDING && status != BookingStatus.ACTIVE) {
            return false;
        }

        return !now.isBefore(booking.getEndDateTime().minusHours(6));
    }

    private String resolveRecipient(Booking booking) {
        if (booking == null || booking.getUserId() == null) {
            return null;
        }

        return usersRepository.findById(booking.getUserId())
                .map(Users::getEmail)
                .map(email -> email == null ? null : email.trim())
                .filter(email -> email != null && !email.isBlank())
                .orElse(null);
    }

    private String safeRecipient(Booking booking) {
        String recipient = booking.getConfirmationEmailRecipient();
        return recipient == null || recipient.isBlank() ? "n/a" : recipient;
    }

    private String formatDateTime(LocalDateTime value) {
        return value == null ? "n/a" : DATE_TIME_FORMAT.format(value);
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "n/a" : value.trim();
    }
}
