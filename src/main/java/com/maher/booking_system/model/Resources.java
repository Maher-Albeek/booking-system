package com.maher.booking_system.model;

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
