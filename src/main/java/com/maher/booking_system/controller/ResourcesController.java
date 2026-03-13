package com.maher.booking_system.controller;

import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.service.BookingService;
import com.maher.booking_system.service.ResourcePhotoStorageService;
import com.maher.booking_system.service.ResourcesService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/resources")
public class ResourcesController {
    private final ResourcesService resourcesService;
    private final BookingService bookingService;
    private final ResourcePhotoStorageService resourcePhotoStorageService;

    public ResourcesController(
            ResourcesService resourcesService,
            BookingService bookingService,
            ResourcePhotoStorageService resourcePhotoStorageService
    ) {
        this.resourcesService = resourcesService;
        this.bookingService = bookingService;
        this.resourcePhotoStorageService = resourcePhotoStorageService;
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

    @GetMapping("/{id}/photos/{fileName:.+}")
    public ResponseEntity<org.springframework.core.io.Resource> getResourcePhoto(
            @PathVariable("id") @NonNull Long id,
            @PathVariable("fileName") @NonNull String fileName
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        return resourcePhotoStorageService.readPhoto(id, fileName);
    }

    // POST create resource
    @PostMapping
    public @NonNull Resources createResource(@RequestBody @NonNull Resources resource) {
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        return resourcesService.createResource(safeResource);
    }

    @PutMapping("/{id}")
    public @NonNull Resources updateResource(
            @PathVariable @NonNull Long id,
            @RequestBody @NonNull Resources resource
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        return resourcesService.updateResource(id, safeResource);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        resourcesService.deleteResource(id);
    }
    
}
