package com.maher.booking_system.model;

import java.time.LocalDate;

public class SeasonalPricingRule {
    private String label;
    private LocalDate startDate;
    private LocalDate endDate;
    private Double dailyPrice;
    private Double hourlyPrice;

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = normalizeText(label); }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public Double getDailyPrice() { return dailyPrice; }
    public void setDailyPrice(Double dailyPrice) {
        this.dailyPrice = dailyPrice != null && dailyPrice >= 0 ? dailyPrice : null;
    }

    public Double getHourlyPrice() { return hourlyPrice; }
    public void setHourlyPrice(Double hourlyPrice) {
        this.hourlyPrice = hourlyPrice != null && hourlyPrice >= 0 ? hourlyPrice : null;
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
