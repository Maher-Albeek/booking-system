package com.maher.booking_system.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CheckInOutRecord {
    private Long id;
    private Long bookingId;
    private Long employeeUserId;
    private Integer odometerStart;
    private Integer odometerEnd;
    private String fuelLevelStart;
    private String fuelLevelEnd;
    private String internalNotes;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
    private List<String> checkInPhotoUrls = new ArrayList<>();
    private List<String> checkOutPhotoUrls = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }
    public Long getEmployeeUserId() { return employeeUserId; }
    public void setEmployeeUserId(Long employeeUserId) { this.employeeUserId = employeeUserId; }
    public Integer getOdometerStart() { return odometerStart; }
    public void setOdometerStart(Integer odometerStart) { this.odometerStart = odometerStart; }
    public Integer getOdometerEnd() { return odometerEnd; }
    public void setOdometerEnd(Integer odometerEnd) { this.odometerEnd = odometerEnd; }
    public String getFuelLevelStart() { return fuelLevelStart; }
    public void setFuelLevelStart(String fuelLevelStart) { this.fuelLevelStart = fuelLevelStart; }
    public String getFuelLevelEnd() { return fuelLevelEnd; }
    public void setFuelLevelEnd(String fuelLevelEnd) { this.fuelLevelEnd = fuelLevelEnd; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
    public LocalDateTime getCheckInTime() { return checkInTime; }
    public void setCheckInTime(LocalDateTime checkInTime) { this.checkInTime = checkInTime; }
    public LocalDateTime getCheckOutTime() { return checkOutTime; }
    public void setCheckOutTime(LocalDateTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public List<String> getCheckInPhotoUrls() { return checkInPhotoUrls == null ? List.of() : checkInPhotoUrls; }
    public void setCheckInPhotoUrls(List<String> checkInPhotoUrls) { this.checkInPhotoUrls = checkInPhotoUrls == null ? new ArrayList<>() : new ArrayList<>(checkInPhotoUrls); }
    public List<String> getCheckOutPhotoUrls() { return checkOutPhotoUrls == null ? List.of() : checkOutPhotoUrls; }
    public void setCheckOutPhotoUrls(List<String> checkOutPhotoUrls) { this.checkOutPhotoUrls = checkOutPhotoUrls == null ? new ArrayList<>() : new ArrayList<>(checkOutPhotoUrls); }
}
