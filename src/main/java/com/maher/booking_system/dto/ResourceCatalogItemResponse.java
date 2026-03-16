package com.maher.booking_system.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ResourceCatalogItemResponse(
        Long id,
        String name,
        String description,
        String type,
        String location,
        String model,
        String carType,
        String color,
        Integer year,
        Integer seats,
        String transmission,
        String fuelType,
        Double dailyPrice,
        String priceUnit,
        Integer baggageBags,
        Boolean hasAirConditioning,
        Integer horsepower,
        Integer kmPerDayLimit,
        Double extraKmFeePerKm,
        Double lateFeePerHour,
        Double depositAmount,
        LocalDateTime maintenanceStartDateTime,
        LocalDateTime maintenanceEndDateTime,
        String maintenanceNotes,
        boolean active,
        boolean available,
        boolean favorite,
        List<String> photoUrls
) {
}
