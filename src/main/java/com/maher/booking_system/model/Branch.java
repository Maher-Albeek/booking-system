package com.maher.booking_system.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Branch {
    private Long id;
    private String name;
    private String city;
    private String address;
    private boolean airportPickupSupported;
    private Long airportPickupFeeCents;
    private Map<String, String> workingHours = new LinkedHashMap<>();
    private List<String> closedDates = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public boolean isAirportPickupSupported() { return airportPickupSupported; }
    public void setAirportPickupSupported(boolean airportPickupSupported) { this.airportPickupSupported = airportPickupSupported; }
    public Long getAirportPickupFeeCents() { return airportPickupFeeCents; }
    public void setAirportPickupFeeCents(Long airportPickupFeeCents) { this.airportPickupFeeCents = airportPickupFeeCents; }
    public Map<String, String> getWorkingHours() { return workingHours == null ? Map.of() : workingHours; }
    public void setWorkingHours(Map<String, String> workingHours) { this.workingHours = workingHours == null ? new LinkedHashMap<>() : new LinkedHashMap<>(workingHours); }
    public List<String> getClosedDates() { return closedDates == null ? List.of() : closedDates; }
    public void setClosedDates(List<String> closedDates) { this.closedDates = closedDates == null ? new ArrayList<>() : new ArrayList<>(closedDates); }
}
