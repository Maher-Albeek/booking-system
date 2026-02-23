package com.maher.booking_system.model;

import jakarta.persistence.*;
@Entity
@Table(name = "time_slots")

public class Time_slots {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String start_time;
    private String end_time;
    private Long resource_id;
    private boolean available;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getStart_time() { return start_time; }
    public void setStart_time(String start_time) { this.start_time = start_time; }

    public String getEnd_time() { return end_time; }
    public void setEnd_time(String end_time) { this.end_time = end_time; }

    public Long getResource_id() { return resource_id; }
    public void setResource_id(Long resource_id) { this.resource_id = resource_id; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    
}
