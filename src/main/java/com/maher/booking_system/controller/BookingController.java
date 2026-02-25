package com.maher.booking_system.controller;

import com.maher.booking_system.mapper.BookingMapper;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.dto.CreateBookingRequest;
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

    @GetMapping
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings()
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        Booking booking = bookingService.getBookingById(id);
        return BookingMapper.toResponse(booking);
    }

    @PostMapping
    public BookingResponse create(@Valid @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return BookingMapper.toResponse(booking);
    }

    @PatchMapping("/{id}/cancel")
    public void cancel(@PathVariable Long id) {
        bookingService.cancelBooking(id);
    }
}
