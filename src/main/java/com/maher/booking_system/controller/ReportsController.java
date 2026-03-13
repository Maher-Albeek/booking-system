package com.maher.booking_system.controller;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.service.BookingService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reports")
public class ReportsController {
    private final BookingService bookingService;

    public ReportsController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping(value = "/bookings.csv", produces = "text/csv")
    public ResponseEntity<String> exportBookingsCsv(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String paymentStatus
    ) {
        List<Booking> bookings = bookingService.getBookingsForAdmin(from, to, status, resourceId, userId, paymentStatus);
        StringBuilder csv = new StringBuilder();
        csv.append("bookingId,userId,carId,startDateTime,endDateTime,totalCents,status,paymentStatus\n");
        for (Booking booking : bookings) {
            csv.append(booking.getId()).append(",")
                    .append(booking.getUserId()).append(",")
                    .append(booking.getResourceId()).append(",")
                    .append(booking.getStartDateTime()).append(",")
                    .append(booking.getEndDateTime()).append(",")
                    .append(booking.getPayableAmountCents() == null ? 0 : booking.getPayableAmountCents()).append(",")
                    .append(booking.getStatus()).append(",")
                    .append(booking.getPaymentStatus()).append("\n");
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookings.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
