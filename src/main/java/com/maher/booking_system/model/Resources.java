package com.maher.booking_system.model;
import jakarta.persistence.*;

@Entity
@Table(name = "resources")

public class Resources {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String type;
    private String location;
    private boolean active;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }   
    
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
