package com.maher.booking_system.controller;

import com.maher.booking_system.mapper.BookingMapper;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.service.BookingService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "List all bookings (legacy endpoint; admin should use /api/admin/bookings)")
    @GetMapping
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings()
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get booking details by id")
    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        Booking booking = bookingService.getBookingById(id);
        return BookingMapper.toResponse(booking);
    }

    @Operation(summary = "Cancel a booking")
    @PatchMapping("/{id}/cancel")
    public void cancel(@PathVariable @NonNull Long id) {
        bookingService.cancelBooking(id);
    }

    @Operation(summary = "Booking documents")
    @GetMapping("/{id}/documents")
    public Map<String, String> getDocuments(@PathVariable @NonNull Long id) {
        bookingService.getBookingById(id);
        return Map.of(
                "contractPdfUrl", "/api/bookings/" + id + "/documents/contract",
                "receiptPdfUrl", "/api/bookings/" + id + "/documents/receipt"
        );
    }
}
