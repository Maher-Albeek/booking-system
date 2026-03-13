package com.maher.booking_system.controller;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.dto.BookingStatusUpdateRequest;
import com.maher.booking_system.mapper.BookingMapper;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/admin/bookings")
@SecurityRequirement(name = "bearerAuth")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @Operation(summary = "Admin list of all bookings with filters")
    @GetMapping
    public List<BookingResponse> getAllBookings(
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long resourceId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String paymentStatus
    ) {
        return bookingService.getBookingsForAdmin(from, to, status, resourceId, userId, paymentStatus).stream()
                .map(BookingMapper::toResponse)
                .toList();
    }

    @Operation(summary = "Admin booking details")
    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return BookingMapper.toResponse(bookingService.getBookingById(id));
    }

    @Operation(summary = "Admin booking status update")
    @PatchMapping("/{id}/status")
    public BookingResponse updateStatus(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody BookingStatusUpdateRequest request
    ) {
        Objects.requireNonNull(id, "id must not be null");
        BookingStatus status = BookingStatus.valueOf(request.getStatus().trim().toUpperCase());
        return BookingMapper.toResponse(bookingService.updateStatus(id, status));
    }
}
