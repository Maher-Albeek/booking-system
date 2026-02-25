package com.maher.booking_system.service;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.repository.TimeSlotRepository;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;

@Service
public class Time_slotsService {
    private final TimeSlotRepository timeSlotRepository;

    public Time_slotsService(TimeSlotRepository timeSlotRepository) {
        this.timeSlotRepository = timeSlotRepository;
    }

    public @NonNull TimeSlot createTimeSlot(@NonNull TimeSlot timeSlot) {
        TimeSlot safeTimeSlot = Objects.requireNonNull(timeSlot, "timeSlot must not be null");
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
}
