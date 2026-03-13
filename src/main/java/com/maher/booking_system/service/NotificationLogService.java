package com.maher.booking_system.service;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.NotificationLog;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.NotificationLogRepository;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationLogService {
    private final NotificationLogRepository notificationLogRepository;
    private final UsersRepository usersRepository;
    private final BookingRepository bookingRepository;

    public NotificationLogService(
            NotificationLogRepository notificationLogRepository,
            UsersRepository usersRepository,
            BookingRepository bookingRepository
    ) {
        this.notificationLogRepository = notificationLogRepository;
        this.usersRepository = usersRepository;
        this.bookingRepository = bookingRepository;
    }

    public void sendBookingConfirmation(Booking booking) {
        Users user = usersRepository.findById(booking.getUserId()).orElse(null);
        String target = user == null ? "unknown" : user.getEmail();
        String bookingId = booking.getId() == null ? "N/A" : booking.getId().toString();
        String subject = "Booking confirmation #" + bookingId;
        String body = "Contract PDF: /api/bookings/" + bookingId + "/documents/contract\n"
                + "Receipt PDF: /api/bookings/" + bookingId + "/documents/receipt";
        createLog(booking, target, "BOOKING_CONFIRMATION", subject, body);
    }

    public int sendReturnReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime min = now.plusHours(6).minusMinutes(15);
        LocalDateTime max = now.plusHours(6).plusMinutes(15);
        int sent = 0;
        for (Booking booking : bookingRepository.findAll()) {
            if (booking.getEndDateTime() == null) {
                continue;
            }
            if (booking.getEndDateTime().isBefore(min) || booking.getEndDateTime().isAfter(max)) {
                continue;
            }
            Users user = usersRepository.findById(booking.getUserId()).orElse(null);
            String target = user == null ? "unknown" : user.getEmail();
            createLog(booking, target, "RETURN_REMINDER",
                    "Return reminder for booking #" + booking.getId(),
                    "Return at " + booking.getEndDateTime() + ". Late fee rules may apply.");
            sent++;
        }
        return sent;
    }

    public List<NotificationLog> getAllLogs() {
        return notificationLogRepository.findAll();
    }

    private void createLog(Booking booking, String target, String type, String subject, String body) {
        NotificationLog log = new NotificationLog();
        log.setBookingId(booking.getId());
        log.setUserId(booking.getUserId());
        log.setType(type);
        log.setTarget(target);
        log.setSubject(subject);
        log.setBody(body);
        log.setSentAt(LocalDateTime.now());
        notificationLogRepository.save(log);
    }
}
