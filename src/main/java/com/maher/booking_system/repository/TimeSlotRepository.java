package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public class TimeSlotRepository extends JsonRepositorySupport<TimeSlot> {

    public TimeSlotRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "time-slots.json", TimeSlot.class, TimeSlot::getId, TimeSlot::setId);
    }

    public Optional<TimeSlot> findByIdForUpdate(Long id) {
        return findById(id);
    }

    public List<TimeSlot> findByResourceId(Long resourceId) {
        return findAll().stream()
                .filter(slot -> resourceId.equals(slot.getResourceId()))
                .toList();
    }

    public List<TimeSlot> findByResourceIdAndAvailable(Long resourceId, boolean available) {
        return findAll().stream()
                .filter(slot -> resourceId.equals(slot.getResourceId()) && slot.isAvailable() == available)
                .toList();
    }
}
