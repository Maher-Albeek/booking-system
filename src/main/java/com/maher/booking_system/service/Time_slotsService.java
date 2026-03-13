package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.repository.TimeSlotRepository;
import java.util.EnumSet;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import com.maher.booking_system.model.enums.BookingStatus;

@Service
public class Time_slotsService {
    private final TimeSlotRepository timeSlotRepository;
    private final BookingRepository bookingRepository;

    public Time_slotsService(TimeSlotRepository timeSlotRepository, BookingRepository bookingRepository) {
        this.timeSlotRepository = timeSlotRepository;
        this.bookingRepository = bookingRepository;
    }

    public @NonNull TimeSlot createTimeSlot(@NonNull TimeSlot timeSlot) {
        TimeSlot safeTimeSlot = Objects.requireNonNull(timeSlot, "timeSlot must not be null");
        return timeSlotRepository.save(safeTimeSlot);
    }

    public @NonNull TimeSlot updateTimeSlot(@NonNull Long id, @NonNull TimeSlot timeSlot) {
        Objects.requireNonNull(id, "id must not be null");
        TimeSlot safeTimeSlot = Objects.requireNonNull(timeSlot, "timeSlot must not be null");

        TimeSlot existingTimeSlot = timeSlotRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Time slot not found with id: " + id));

        boolean hasBlockingBooking = hasBlockingBooking(existingTimeSlot);
        if (hasBlockingBooking && !Objects.equals(existingTimeSlot.getResourceId(), safeTimeSlot.getResourceId())) {
            throw new BadRequestException("A booked time slot cannot be moved to another car.");
        }

        if (hasBlockingBooking && safeTimeSlot.isAvailable()) {
            throw new BadRequestException("A booked time slot cannot be marked as available.");
        }

        safeTimeSlot.setId(id);
        return timeSlotRepository.save(safeTimeSlot);
    }

    public void deleteTimeSlot(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        timeSlotRepository.deleteById(id);
    }

    public TimeSlot getTimeSlotById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return timeSlotRepository.findById(id).orElse(null);
    }

    public java.util.List<TimeSlot> getAllTimeSlots() {
        return timeSlotRepository.findAll();
    }

    private boolean hasBlockingBooking(TimeSlot slot) {
        if (slot.getResourceId() == null || slot.getStartTime() == null || slot.getEndTime() == null) {
            return false;
        }
        return bookingRepository.existsOverlapping(
                slot.getResourceId(),
                slot.getStartTime(),
                slot.getEndTime(),
                EnumSet.of(BookingStatus.PENDING, BookingStatus.ACTIVE, BookingStatus.COMPLETED)
        );
    }
}
