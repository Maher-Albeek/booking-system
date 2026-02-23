package com.maher.booking_system.service;

import org.springframework.stereotype.Service;
import com.maher.booking_system.model.Time_slots;
import com.maher.booking_system.repository.Time_slotsRepository;

@Service
public class Time_slotsService {
    private final Time_slotsRepository time_slotsRepository;

    public Time_slotsService(Time_slotsRepository time_slotsRepository) {
        this.time_slotsRepository = time_slotsRepository;
    }

    public Time_slots createTimeSlot(Time_slots time_slot) {
        return time_slotsRepository.save(time_slot);
    }

    public void deleteTimeSlot(Long id) {
        time_slotsRepository.deleteById(id);
    }
    public Time_slots getTimeSlotById(Integer id) {
            return time_slotsRepository.findById(id.longValue()).orElse(null);
    }

    public java.util.List<Time_slots> getAllTimeSlots() {
        return time_slotsRepository.findAll();
    }
}
