package com.maher.booking_system.controller;

import com.maher.booking_system.model.Time_slots;
import com.maher.booking_system.service.Time_slotsService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

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
    public Time_slots createTimeSlot(@RequestBody Time_slots time_slot) {
        return time_slotsService.createTimeSlot(time_slot);
    }

    @DeleteMapping("/{id}")
    public void deleteTimeSlot(@PathVariable Long id) {
        time_slotsService.deleteTimeSlot(id);
    }
}