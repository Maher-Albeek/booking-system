package com.maher.booking_system.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckOutRequest {
    private Integer odometerEnd;
    private String fuelLevel;
    private String internalNotes;
    private List<String> photoUrls = new ArrayList<>();
    private List<DamageFeeRequest> damageFees = new ArrayList<>();

    public Integer getOdometerEnd() { return odometerEnd; }
    public void setOdometerEnd(Integer odometerEnd) { this.odometerEnd = odometerEnd; }
    public String getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(String fuelLevel) { this.fuelLevel = fuelLevel; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public List<String> getPhotoUrls() { return photoUrls == null ? List.of() : photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls == null ? new ArrayList<>() : new ArrayList<>(photoUrls); }
    public List<DamageFeeRequest> getDamageFees() { return damageFees == null ? List.of() : damageFees; }
    public void setDamageFees(List<DamageFeeRequest> damageFees) { this.damageFees = damageFees == null ? new ArrayList<>() : new ArrayList<>(damageFees); }

    public static class DamageFeeRequest {
        private String feeType;
        private String notes;
        private Long amountCents;

        public String getFeeType() { return feeType; }
        public void setFeeType(String feeType) { this.feeType = feeType; }
        public String getNotes() { return notes; }
        public void setNotes(String notes) { this.notes = notes; }
        public Long getAmountCents() { return amountCents; }
        public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }
    }
}
