package com.maher.booking_system.service;

import com.maher.booking_system.dto.ResourceCatalogItemResponse;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.TimeSlot;
import java.util.Objects;

import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.TimeSlotRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.repository.ResourcesRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

@Service

public class ResourcesService {
    private final ResourcesRepository resourcesRepository;
    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final ResourcePhotoStorageService resourcePhotoStorageService;

    public ResourcesService(
            ResourcesRepository resourcesRepository,
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            ResourcePhotoStorageService resourcePhotoStorageService
    ) {
        this.resourcesRepository = resourcesRepository;
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.resourcePhotoStorageService = resourcePhotoStorageService;
    }

    public @NonNull Resources createResource(@NonNull Resources resource) {
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        java.util.List<String> submittedPhotoUrls = safeResource.getPhotoUrls();

        safeResource.setPhotoUrls(submittedPhotoUrls.stream()
                .filter(photoUrl -> photoUrl != null && !photoUrl.startsWith("data:"))
                .toList());

        Resources savedResource = resourcesRepository.save(safeResource);
        savedResource.setPhotoUrls(resourcePhotoStorageService.resolvePhotoUrls(
                savedResource.getId(),
                submittedPhotoUrls,
                java.util.List.of()
        ));
        return resourcesRepository.save(savedResource);
    }

    public @NonNull Resources updateResource(@NonNull Long id, @NonNull Resources resource) {
        Objects.requireNonNull(id, "id must not be null");
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");

        Resources existingResource = resourcesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + id));

        safeResource.setId(id);
        safeResource.setPhotoUrls(resourcePhotoStorageService.resolvePhotoUrls(
                id,
                safeResource.getPhotoUrls(),
                existingResource.getPhotoUrls()
        ));
        return resourcesRepository.save(safeResource);
    }

    public Resources getResourceById(long id) {
        return resourcesRepository.findById(id).orElse(null);
    }

    public void deleteResource(long id) {
        resourcePhotoStorageService.deleteAllPhotos(id);
        resourcesRepository.deleteById(id);
    }
    public java.util.List<Resources> getAllResources() {
        return resourcesRepository.findAll();
    }
    public java.util.List<Resources> getCars() {
        return resourcesRepository.findByTypeAndActiveTrue("Car");
    }

    public @NonNull List<ResourceCatalogItemResponse> searchCatalog(
            String location,
            String pickupDateTime,
            String returnDateTime,
            Long userId
    ) {
        LocalDateTime pickup = normalizeOptionalDateTime(pickupDateTime, "pickupDateTime");
        LocalDateTime dropoff = normalizeOptionalDateTime(returnDateTime, "returnDateTime");

        if ((pickup == null) != (dropoff == null)) {
            throw new IllegalArgumentException("pickupDateTime and returnDateTime must be provided together");
        }

        if (pickup != null && !pickup.isBefore(dropoff)) {
            throw new IllegalArgumentException("pickupDateTime must be before returnDateTime");
        }

        String normalizedLocation = normalizeText(location);

        return getCars().stream()
                .filter(car -> normalizedLocation == null || normalizeText(car.getLocation()).toLowerCase().contains(normalizedLocation.toLowerCase()))
                .map(car -> toCatalogItem(car, pickup, dropoff, userId))
                .filter(item -> pickup == null || item.available())
                .sorted(Comparator.comparing(ResourceCatalogItemResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public @NonNull List<ResourceCatalogItemResponse> getSimilarCars(
            @NonNull Long id,
            String pickupDateTime,
            String returnDateTime,
            Long userId
    ) {
        Objects.requireNonNull(id, "id must not be null");
        Resources referenceCar = resourcesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + id));
        LocalDateTime pickup = normalizeOptionalDateTime(pickupDateTime, "pickupDateTime");
        LocalDateTime dropoff = normalizeOptionalDateTime(returnDateTime, "returnDateTime");

        return getCars().stream()
                .filter(car -> !Objects.equals(car.getId(), id))
                .sorted(Comparator.comparingInt(car -> similarityScore(referenceCar, car)))
                .limit(3)
                .map(car -> toCatalogItem(car, pickup, dropoff, userId))
                .toList();
    }

    public @NonNull List<ResourceCatalogItemResponse> getFavorites(@NonNull Long userId) {
        Objects.requireNonNull(userId, "userId must not be null");
        return getCars().stream()
                .filter(car -> car.getFavoriteUserIds().contains(userId))
                .map(car -> toCatalogItem(car, null, null, userId))
                .sorted(Comparator.comparing(ResourceCatalogItemResponse::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public @NonNull Resources addFavorite(@NonNull Long resourceId, @NonNull Long userId) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Resources resource = resourcesRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + resourceId));
        List<Long> updatedFavorites = new java.util.ArrayList<>(resource.getFavoriteUserIds());
        if (!updatedFavorites.contains(userId)) {
            updatedFavorites.add(userId);
            resource.setFavoriteUserIds(updatedFavorites);
            return resourcesRepository.save(resource);
        }
        return resource;
    }

    public @NonNull Resources removeFavorite(@NonNull Long resourceId, @NonNull Long userId) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Resources resource = resourcesRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + resourceId));
        List<Long> updatedFavorites = new java.util.ArrayList<>(resource.getFavoriteUserIds());
        if (updatedFavorites.remove(userId)) {
            resource.setFavoriteUserIds(updatedFavorites);
            return resourcesRepository.save(resource);
        }
        return resource;
    }

    private ResourceCatalogItemResponse toCatalogItem(
            Resources car,
            LocalDateTime pickup,
            LocalDateTime dropoff,
            Long userId
    ) {
        boolean available = pickup == null || isCarAvailable(car, pickup, dropoff);
        boolean favorite = userId != null && car.getFavoriteUserIds().contains(userId);

        return new ResourceCatalogItemResponse(
                car.getId(),
                car.getName(),
                car.getDescription(),
                car.getType(),
                car.getLocation(),
                car.getModel(),
                car.getCarType(),
                car.getColor(),
                car.getYear(),
                car.getSeats(),
                car.getTransmission(),
                car.getFuelType(),
                car.getDailyPrice(),
                car.getPriceUnit(),
                car.getBaggageBags(),
                car.getHasAirConditioning(),
                car.getHorsepower(),
                car.getKmPerDayLimit(),
                car.getExtraKmFeePerKm(),
                car.getLateFeePerHour(),
                car.getDepositAmount(),
                car.getMaintenanceStartDateTime(),
                car.getMaintenanceEndDateTime(),
                car.getMaintenanceNotes(),
                car.isActive(),
                available,
                favorite,
                car.getPhotoUrls()
        );
    }

    private boolean isCarAvailable(Resources car, LocalDateTime pickup, LocalDateTime dropoff) {
        if (!car.isActive()) {
            return false;
        }

        if (isInMaintenance(car, pickup, dropoff)) {
            return false;
        }

        return bookingRepository.findByResourceIdAndStatus(car.getId(), BookingStatus.CONFIRMED).stream()
                .noneMatch(booking -> bookingOverlaps(booking, pickup, dropoff));
    }

    private boolean isInMaintenance(Resources car, LocalDateTime pickup, LocalDateTime dropoff) {
        if (car.getMaintenanceStartDateTime() == null || car.getMaintenanceEndDateTime() == null) {
            return false;
        }
        return pickup.isBefore(car.getMaintenanceEndDateTime()) && dropoff.isAfter(car.getMaintenanceStartDateTime());
    }

    private boolean bookingOverlaps(Booking booking, LocalDateTime pickup, LocalDateTime dropoff) {
        BookingRange range = resolveBookingRange(booking);
        return range != null && range.start().isBefore(dropoff) && range.end().isAfter(pickup);
    }

    private int similarityScore(Resources referenceCar, Resources candidate) {
        int score = 100;
        if (Objects.equals(normalizeText(referenceCar.getCarType()), normalizeText(candidate.getCarType()))) {
            score -= 35;
        }
        if (Objects.equals(normalizeText(referenceCar.getTransmission()), normalizeText(candidate.getTransmission()))) {
            score -= 20;
        }
        if (Objects.equals(normalizeText(referenceCar.getFuelType()), normalizeText(candidate.getFuelType()))) {
            score -= 15;
        }
        if (referenceCar.getSeats() != null && candidate.getSeats() != null) {
            score -= Math.max(0, 10 - Math.abs(referenceCar.getSeats() - candidate.getSeats()) * 3);
        }
        if (referenceCar.getDailyPrice() != null && candidate.getDailyPrice() != null) {
            score -= Math.max(0, 20 - (int) Math.min(20, Math.abs(referenceCar.getDailyPrice() - candidate.getDailyPrice())));
        }
        return score;
    }

    private LocalDateTime normalizeOptionalDateTime(String value, String fieldName) {
        String normalized = normalizeText(value);
        if (normalized == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + " must use yyyy-MM-ddTHH:mm format");
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private BookingRange resolveBookingRange(Booking booking) {
        if (booking.getStartDateTime() != null && booking.getEndDateTime() != null) {
            if (booking.getStartDateTime().isBefore(booking.getEndDateTime())) {
                return new BookingRange(booking.getStartDateTime(), booking.getEndDateTime());
            }
            return null;
        }

        if (booking.getTimeSlotId() == null) {
            return null;
        }

        return timeSlotRepository.findById(booking.getTimeSlotId())
                .filter(this::hasValidTimeSlotRange)
                .map(slot -> new BookingRange(slot.getStartTime(), slot.getEndTime()))
                .orElse(null);
    }

    private boolean hasValidTimeSlotRange(TimeSlot slot) {
        return slot.getStartTime() != null
                && slot.getEndTime() != null
                && slot.getStartTime().isBefore(slot.getEndTime());
    }

    private record BookingRange(LocalDateTime start, LocalDateTime end) {
    }
}
