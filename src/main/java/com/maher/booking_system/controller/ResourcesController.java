package com.maher.booking_system.controller;

import com.maher.booking_system.dto.ResourceCatalogItemResponse;
import com.maher.booking_system.dto.ResourceFavoriteRequest;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.service.BookingService;
import com.maher.booking_system.service.ResourcePhotoStorageService;
import com.maher.booking_system.service.ResourcesService;
import jakarta.validation.Valid;
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

    @GetMapping("/catalog")
    public List<ResourceCatalogItemResponse> getCatalog(
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String pickupDateTime,
            @RequestParam(required = false) String returnDateTime,
            @RequestParam(required = false) Long userId
    ) {
        return resourcesService.searchCatalog(location, pickupDateTime, returnDateTime, userId);
    }

    @GetMapping("/favorites")
    public List<ResourceCatalogItemResponse> getFavorites(@RequestParam Long userId) {
        return resourcesService.getFavorites(userId);
    }

    @GetMapping("/{id}/similar")
    public List<ResourceCatalogItemResponse> getSimilarCars(
            @PathVariable("id") @NonNull Long id,
            @RequestParam(required = false) String pickupDateTime,
            @RequestParam(required = false) String returnDateTime,
            @RequestParam(required = false) Long userId
    ) {
        Objects.requireNonNull(id, "id must not be null");
        return resourcesService.getSimilarCars(id, pickupDateTime, returnDateTime, userId);
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

    @PostMapping("/{id}/favorites")
    public @NonNull Resources addFavorite(
            @PathVariable("id") @NonNull Long id,
            @Valid @RequestBody @NonNull ResourceFavoriteRequest request
    ) {
        Objects.requireNonNull(id, "id must not be null");
        ResourceFavoriteRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        return resourcesService.addFavorite(id, safeRequest.userId());
    }

    @DeleteMapping("/{id}/favorites/{userId}")
    public @NonNull Resources removeFavorite(
            @PathVariable("id") @NonNull Long id,
            @PathVariable("userId") @NonNull Long userId
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        return resourcesService.removeFavorite(id, userId);
    }
    
}
