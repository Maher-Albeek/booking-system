package com.maher.booking_system.model;

public class DamageReportItem {
    private String type;
    private String notes;
    private Long feeCents;

    public String getType() { return type; }
    public void setType(String type) { this.type = normalizeText(type); }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = normalizeText(notes); }

    public Long getFeeCents() { return feeCents; }
    public void setFeeCents(Long feeCents) { this.feeCents = feeCents == null || feeCents < 0 ? 0L : feeCents; }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
