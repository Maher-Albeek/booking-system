package com.maher.booking_system.controller;

import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.service.Time_slotsService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/time_slots")

public class Time_slotsController {
    private final Time_slotsService time_slotsService;

    public Time_slotsController(Time_slotsService time_slotsService) {
        this.time_slotsService = time_slotsService;
    }

    // GET all time slots
    @GetMapping
    public List<TimeSlot> getAllTimeSlots() {
        return time_slotsService.getAllTimeSlots();
    }

    // POST create time slot
    @PostMapping
    public @NonNull TimeSlot createTimeSlot(@RequestBody @NonNull TimeSlot time_slot) {
        TimeSlot safeTimeSlot = Objects.requireNonNull(time_slot, "time_slot must not be null");
        return time_slotsService.createTimeSlot(safeTimeSlot);
    }

    @PutMapping("/{id}")
    public @NonNull TimeSlot updateTimeSlot(
            @PathVariable @NonNull Long id,
            @RequestBody @NonNull TimeSlot time_slot
    ) {
        Objects.requireNonNull(id, "id must not be null");
        TimeSlot safeTimeSlot = Objects.requireNonNull(time_slot, "time_slot must not be null");
        return time_slotsService.updateTimeSlot(id, safeTimeSlot);
    }

    @DeleteMapping("/{id}")
    public void deleteTimeSlot(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        time_slotsService.deleteTimeSlot(id);
    }
}
