package com.maher.booking_system.controller;

import java.util.List;
import java.util.Objects;

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
    public List<Booking> getAllBookings() {
        return bookingService.getAllBookings();
    }

    @GetMapping("/{id}")
    public Booking getBookingById(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return bookingService.getBookingById(id);
    }

    @PostMapping
    public @NonNull Booking createBooking(@RequestBody @NonNull Booking booking) {
        Booking safeBooking = Objects.requireNonNull(booking, "booking must not be null");
        return bookingService.createBooking(safeBooking);
    }

    @DeleteMapping("/{id}")
    public void deleteBooking(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        bookingService.deleteBooking(id);
    }
}
