package com.maher.booking_system.controller;

import com.maher.booking_system.dto.CarAvailabilityResponse;
import com.maher.booking_system.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.service.BookingService;
import com.maher.booking_system.service.ResourcePhotoStorageService;
import com.maher.booking_system.service.ResourcesService;
import com.maher.booking_system.service.Time_slotsService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api/resources")
public class ResourcesController {
    private final ResourcesService resourcesService;
    private final BookingService bookingService;
    private final Time_slotsService timeSlotsService;
    private final ResourcePhotoStorageService resourcePhotoStorageService;

    public ResourcesController(
            ResourcesService resourcesService,
            BookingService bookingService,
            Time_slotsService timeSlotsService,
            ResourcePhotoStorageService resourcePhotoStorageService
    ) {
        this.resourcesService = resourcesService;
        this.bookingService = bookingService;
        this.timeSlotsService = timeSlotsService;
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

    @GetMapping("/cars/{id}/similar")
    public List<Resources> getSimilarCars(@PathVariable("id") Long id) {
        Resources base = resourcesService.getResourceById(id);
        if (base == null) {
            return List.of();
        }
        return resourcesService.getCars().stream()
                .filter(car -> !id.equals(car.getId()))
                .filter(car -> base.getCarType() == null || base.getCarType().equalsIgnoreCase(car.getCarType()))
                .limit(6)
                .toList();
    }

    @Operation(summary = "List available cars for a date-time range")
    @GetMapping("/cars/availability")
    public List<Resources> getAvailableCars(
            @RequestParam("startDateTime") String startDateTime,
            @RequestParam("endDateTime") String endDateTime
    ) {
        LocalDateTime from = parseDateTime(startDateTime, "startDateTime");
        LocalDateTime to = parseDateTime(endDateTime, "endDateTime");
        if (!from.isBefore(to)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }

        return resourcesService.getCars().stream()
                .filter(car -> !car.isInMaintenance())
                .filter(car -> bookingService.isCarAvailableForRange(car.getId(), from, to))
                .toList();
    }

    @Operation(summary = "Check a single car availability for a date-time range")
    @GetMapping("/{id}/availability")
    public CarAvailabilityResponse checkAvailability(
            @PathVariable("id") @NonNull Long id,
            @RequestParam("startDateTime") String startDateTime,
            @RequestParam("endDateTime") String endDateTime
    ) {
        LocalDateTime from = parseDateTime(startDateTime, "startDateTime");
        LocalDateTime to = parseDateTime(endDateTime, "endDateTime");
        if (!from.isBefore(to)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }

        Resources car = resourcesService.getResourceById(id);
        if (car == null || car.isInMaintenance()) {
            return new CarAvailabilityResponse(id, false);
        }
        return new CarAvailabilityResponse(id, bookingService.isCarAvailableForRange(id, from, to));
    }

    @GetMapping("/{id}/time-slots")
    public List<TimeSlot> getResourceTimeSlots(
            @PathVariable("id") @NonNull Long id,
            @RequestParam(required = false) Boolean available
    ) {
        Objects.requireNonNull(id, "id must not be null");
        List<TimeSlot> slots = timeSlotsService.getAllTimeSlots().stream()
                .filter(slot -> id.equals(slot.getResourceId()))
                .toList();
        if (available == null) {
            return slots;
        }
        return slots.stream()
                .filter(slot -> slot.isAvailable() == available)
                .toList();
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

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(fieldName + " must use yyyy-MM-ddTHH:mm format");
        }
    }
}
