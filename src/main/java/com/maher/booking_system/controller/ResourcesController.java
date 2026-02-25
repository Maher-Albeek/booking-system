package com.maher.booking_system.controller;

import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.service.BookingService;
import com.maher.booking_system.service.ResourcesService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourcesController {
    private final ResourcesService resourcesService;
    private final BookingService bookingService;

    public ResourcesController(ResourcesService resourcesService, BookingService bookingService) {
        this.resourcesService = resourcesService;
        this.bookingService = bookingService;
    }

    // GET all resources
    @GetMapping
    public List<Resources> getAllResources() {
        return resourcesService.getAllResources();
    }

    @GetMapping("/cars")
    public List<Resources> getCars() {
        return resourcesService.getCars();
    }

    @GetMapping("/{id}/time-slots")
    public List<TimeSlot> getResourceTimeSlots(
            @PathVariable("id") @NonNull Long id,
            @RequestParam(required = false) Boolean available
    ) {
        Objects.requireNonNull(id, "id must not be null");
        return bookingService.getTimeSlotsByResource(id, available);
    }

    // POST create resource
    @PostMapping
    public @NonNull Resources createResource(@RequestBody @NonNull Resources resource) {
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        return resourcesService.createResource(safeResource);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        resourcesService.deleteResource(id);
    }
    
}
