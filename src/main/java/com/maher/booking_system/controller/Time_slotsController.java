package com.maher.booking_system.controller;

import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.Time_slots;
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
    public List<Time_slots> getAllTimeSlots() {
        return time_slotsService.getAllTimeSlots();
    }

    // POST create time slot
    @PostMapping
    public @NonNull Time_slots createTimeSlot(@RequestBody @NonNull Time_slots time_slot) {
        Time_slots safeTimeSlot = Objects.requireNonNull(time_slot, "time_slot must not be null");
        return time_slotsService.createTimeSlot(safeTimeSlot);
    }

    @DeleteMapping("/{id}")
    public void deleteTimeSlot(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        time_slotsService.deleteTimeSlot(id);
    }
}
