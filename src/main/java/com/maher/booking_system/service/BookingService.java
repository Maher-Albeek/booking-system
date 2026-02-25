package com.maher.booking_system.service;

import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.ConflictException;
import com.maher.booking_system.exception.NotFoundException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.repository.TimeSlotRepository;
import org.springframework.lang.NonNull;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.model.enums.BookingStatus;

@Service
public class BookingService {

    private static final BookingStatus CONFIRMED_STATUS = BookingStatus.CONFIRMED;

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;

    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public @NonNull Booking getBookingById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
    }

    @Transactional
    public @NonNull Booking createBooking(@NonNull CreateBookingRequest request) {
        CreateBookingRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(safeRequest.getTimeSlotId())
                .orElseThrow(() -> new NotFoundException("Time slot not found with id: " + safeRequest.getTimeSlotId()));

        if (!slot.getResourceId().equals(safeRequest.getResourceId())) {
            throw new BadRequestException("Time slot does not belong to resource id: " + safeRequest.getResourceId());
        }

        if (!slot.isAvailable()) {
            throw new ConflictException("Time slot is not available");
        }

        boolean alreadyBooked = bookingRepository.existsByTimeSlotIdAndStatus(safeRequest.getTimeSlotId(), CONFIRMED_STATUS);
        if (alreadyBooked) {
            throw new ConflictException("Time slot already booked");
        }

        Booking booking = new Booking();
        booking.setUserId(safeRequest.getUserId());
        booking.setResourceId(safeRequest.getResourceId());
        booking.setTimeSlotId(safeRequest.getTimeSlotId());
        booking.setCustomerName(safeRequest.getCustomerName());
        booking.setServiceName(safeRequest.getServiceName());
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingTime(LocalDateTime.now());

        slot.setAvailable(false);
        timeSlotRepository.save(slot);

        try {
            return bookingRepository.save(booking);
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("Time slot already booked");
        }
    }

    public List<TimeSlot> getTimeSlotsByResource(@NonNull Long resourceId, Boolean available) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        if (available == null) {
            return timeSlotRepository.findByResourceId(resourceId);
        }
        return timeSlotRepository.findByResourceIdAndAvailable(resourceId, available);
    }

    @Transactional
    public void cancelBooking(@NonNull Long bookingId) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking already cancelled");
        }

        TimeSlot slot = timeSlotRepository.findByIdForUpdate(booking.getTimeSlotId())
                .orElseThrow(() -> new NotFoundException("Slot not found"));

        booking.setStatus(BookingStatus.CANCELLED);
        slot.setAvailable(true);

        bookingRepository.save(booking);
        timeSlotRepository.save(slot);
    }
}
