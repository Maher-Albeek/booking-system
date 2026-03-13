package com.maher.booking_system.dto;

import java.util.ArrayList;
import java.util.List;

public class CheckInRequest {
    private Long employeeUserId;
    private Integer odometerStart;
    private String fuelLevel;
    private String internalNotes;
    private List<String> photoUrls = new ArrayList<>();

    public Long getEmployeeUserId() { return employeeUserId; }
    public void setEmployeeUserId(Long employeeUserId) { this.employeeUserId = employeeUserId; }
    public Integer getOdometerStart() { return odometerStart; }
    public void setOdometerStart(Integer odometerStart) { this.odometerStart = odometerStart; }
    public String getFuelLevel() { return fuelLevel; }
    public void setFuelLevel(String fuelLevel) { this.fuelLevel = fuelLevel; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public List<String> getPhotoUrls() { return photoUrls == null ? List.of() : photoUrls; }
    public void setPhotoUrls(List<String> photoUrls) { this.photoUrls = photoUrls == null ? new ArrayList<>() : new ArrayList<>(photoUrls); }
}
