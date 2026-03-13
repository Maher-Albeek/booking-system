package com.maher.booking_system.controller;

import com.maher.booking_system.dto.CheckInRequest;
import com.maher.booking_system.dto.CheckOutRequest;
import com.maher.booking_system.dto.ExtendBookingRequest;
import com.maher.booking_system.model.CancellationPolicyRule;
import com.maher.booking_system.model.CheckInOutRecord;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.service.RentalOperationsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/operations")
public class OperationsController {
    private final RentalOperationsService rentalOperationsService;

    public OperationsController(RentalOperationsService rentalOperationsService) {
        this.rentalOperationsService = rentalOperationsService;
    }

    @PatchMapping("/bookings/{bookingId}/extend")
    public Booking extendBooking(@PathVariable Long bookingId, @Valid @RequestBody ExtendBookingRequest request) {
        return rentalOperationsService.extendBooking(bookingId, request);
    }

    @PatchMapping("/bookings/{bookingId}/cancel-with-refund")
    public RentalOperationsService.RefundResponse cancelWithRefund(@PathVariable Long bookingId) {
        return rentalOperationsService.cancelWithRefund(bookingId);
    }

    @GetMapping("/cancellation-policy")
    public List<CancellationPolicyRule> getCancellationPolicy() {
        return rentalOperationsService.getCancellationPolicy();
    }

    @PutMapping("/admin/cancellation-policy")
    public List<CancellationPolicyRule> saveCancellationPolicy(@RequestBody List<CancellationPolicyRule> rules) {
        return rentalOperationsService.saveCancellationPolicy(rules);
    }

    @PostMapping("/admin/bookings/{bookingId}/check-in")
    public CheckInOutRecord checkIn(@PathVariable Long bookingId, @RequestBody CheckInRequest request) {
        return rentalOperationsService.checkIn(bookingId, request);
    }

    @PostMapping("/admin/bookings/{bookingId}/check-out")
    public RentalOperationsService.CheckOutSummary checkOut(@PathVariable Long bookingId, @RequestBody CheckOutRequest request) {
        return rentalOperationsService.checkOut(bookingId, request);
    }
}
