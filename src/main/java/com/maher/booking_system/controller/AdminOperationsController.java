package com.maher.booking_system.controller;

import com.maher.booking_system.dto.AdminBookingFilterRequest;
import com.maher.booking_system.dto.BookingCheckInRequest;
import com.maher.booking_system.dto.BookingCheckOutRequest;
import com.maher.booking_system.dto.BookingOperationsResponse;
import com.maher.booking_system.service.AdminOperationsService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/operations")
public class AdminOperationsController {
    private final AdminOperationsService adminOperationsService;

    public AdminOperationsController(AdminOperationsService adminOperationsService) {
        this.adminOperationsService = adminOperationsService;
    }

    @GetMapping("/bookings")
    public List<BookingOperationsResponse> listBookings(
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String paymentStatus
    ) {
        return adminOperationsService.findBookings(
                new AdminBookingFilterRequest(startDateTime, endDateTime, status, carId, userId, paymentStatus)
        );
    }

    @GetMapping("/bookings/{id}")
    public BookingOperationsResponse getBooking(@PathVariable Long id) {
        return adminOperationsService.getBooking(id);
    }

    @GetMapping(value = "/bookings/export", produces = "text/csv")
    public ResponseEntity<String> exportBookings(
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long carId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String paymentStatus
    ) {
        String csv = adminOperationsService.exportBookingsCsv(
                new AdminBookingFilterRequest(startDateTime, endDateTime, status, carId, userId, paymentStatus)
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bookings-report.csv")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @PatchMapping("/bookings/{id}/check-in")
    public BookingOperationsResponse checkIn(@PathVariable Long id, @RequestBody BookingCheckInRequest request) {
        return adminOperationsService.checkIn(id, request);
    }

    @PatchMapping("/bookings/{id}/check-out")
    public BookingOperationsResponse checkOut(@PathVariable Long id, @RequestBody BookingCheckOutRequest request) {
        return adminOperationsService.checkOut(id, request);
    }
}
