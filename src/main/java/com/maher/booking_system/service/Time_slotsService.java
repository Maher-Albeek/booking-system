package com.maher.booking_system.service;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.lang.NonNull;
import com.maher.booking_system.model.Time_slots;
import com.maher.booking_system.repository.Time_slotsRepository;

@Service
public class Time_slotsService {
    private final Time_slotsRepository time_slotsRepository;

    public Time_slotsService(Time_slotsRepository time_slotsRepository) {
        this.time_slotsRepository = time_slotsRepository;
    }

    public @NonNull Time_slots createTimeSlot(@NonNull Time_slots time_slot) {
        Time_slots safeTimeSlot = Objects.requireNonNull(time_slot, "time_slot must not be null");
        return time_slotsRepository.save(safeTimeSlot);
    }

    public void deleteTimeSlot(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        time_slotsRepository.deleteById(id);
    }

    public Time_slots getTimeSlotById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return time_slotsRepository.findById(id).orElse(null);
    }

    public java.util.List<Time_slots> getAllTimeSlots() {
        return time_slotsRepository.findAll();
    }
}
