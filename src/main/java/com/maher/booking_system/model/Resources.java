package com.maher.booking_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Resources {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String location;
    private String model;
    private String carType;
    private String color;
    private Integer year;
    private Integer seats;
    private String transmission;
    private String fuelType;
    private Double dailyPrice;
    private String priceUnit;
    private Integer baggageBags;
    private Boolean hasAirConditioning;
    private Integer horsepower;
    private Integer kmPerDayLimit;
    private Double extraKmFeePerKm;
    private Double lateFeePerHour;
    private Double depositAmount;
    private LocalDateTime maintenanceStartDateTime;
    private LocalDateTime maintenanceEndDateTime;
    private String maintenanceNotes;
    private List<Long> favoriteUserIds = new ArrayList<>();
    private boolean active;
    private List<String> photoUrls = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = normalizeText(model); }

    public String getCarType() { return carType; }
    public void setCarType(String carType) { this.carType = normalizeText(carType); }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = normalizeText(color); }

    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year != null && year > 0 ? year : null; }

    public Integer getSeats() { return seats; }
    public void setSeats(Integer seats) { this.seats = seats != null && seats > 0 ? seats : null; }

    public String getTransmission() { return transmission; }
    public void setTransmission(String transmission) { this.transmission = normalizeText(transmission); }

    public String getFuelType() { return fuelType; }
    public void setFuelType(String fuelType) { this.fuelType = normalizeText(fuelType); }

    public Double getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(Double dailyPrice) {
        this.dailyPrice = dailyPrice != null && dailyPrice >= 0 ? dailyPrice : null;
    }

    public String getPriceUnit() { return priceUnit; }
    public void setPriceUnit(String priceUnit) { this.priceUnit = normalizeText(priceUnit); }

    public Integer getBaggageBags() { return baggageBags; }
    public void setBaggageBags(Integer baggageBags) {
        this.baggageBags = baggageBags != null && baggageBags >= 0 ? baggageBags : null;
    }

    public Boolean getHasAirConditioning() { return hasAirConditioning; }
    public void setHasAirConditioning(Boolean hasAirConditioning) {
        this.hasAirConditioning = hasAirConditioning;
    }

    public Integer getHorsepower() { return horsepower; }
    public void setHorsepower(Integer horsepower) {
        this.horsepower = horsepower != null && horsepower > 0 ? horsepower : null;
    }

    public Integer getKmPerDayLimit() { return kmPerDayLimit; }
    public void setKmPerDayLimit(Integer kmPerDayLimit) {
        this.kmPerDayLimit = kmPerDayLimit != null && kmPerDayLimit > 0 ? kmPerDayLimit : null;
    }

    public Double getExtraKmFeePerKm() { return extraKmFeePerKm; }
    public void setExtraKmFeePerKm(Double extraKmFeePerKm) {
        this.extraKmFeePerKm = extraKmFeePerKm != null && extraKmFeePerKm >= 0 ? extraKmFeePerKm : null;
    }

    public Double getLateFeePerHour() { return lateFeePerHour; }
    public void setLateFeePerHour(Double lateFeePerHour) {
        this.lateFeePerHour = lateFeePerHour != null && lateFeePerHour >= 0 ? lateFeePerHour : null;
    }

    public Double getDepositAmount() { return depositAmount; }
    public void setDepositAmount(Double depositAmount) {
        this.depositAmount = depositAmount != null && depositAmount >= 0 ? depositAmount : null;
    }

    public LocalDateTime getMaintenanceStartDateTime() { return maintenanceStartDateTime; }
    public void setMaintenanceStartDateTime(LocalDateTime maintenanceStartDateTime) {
        this.maintenanceStartDateTime = maintenanceStartDateTime;
    }

    public LocalDateTime getMaintenanceEndDateTime() { return maintenanceEndDateTime; }
    public void setMaintenanceEndDateTime(LocalDateTime maintenanceEndDateTime) {
        this.maintenanceEndDateTime = maintenanceEndDateTime;
    }

    public String getMaintenanceNotes() { return maintenanceNotes; }
    public void setMaintenanceNotes(String maintenanceNotes) { this.maintenanceNotes = normalizeText(maintenanceNotes); }

    public List<Long> getFavoriteUserIds() {
        return favoriteUserIds == null ? List.of() : favoriteUserIds;
    }

    public void setFavoriteUserIds(List<Long> favoriteUserIds) {
        this.favoriteUserIds = favoriteUserIds == null
                ? new ArrayList<>()
                : favoriteUserIds.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public List<String> getPhotoUrls() {
        return photoUrls == null ? List.of() : photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls == null
                ? new ArrayList<>()
                : photoUrls.stream()
                        .filter(url -> url != null && !url.isBlank())
                        .map(String::trim)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
