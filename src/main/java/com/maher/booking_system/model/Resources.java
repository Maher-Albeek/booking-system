package com.maher.booking_system.model;

import java.util.ArrayList;
import java.util.List;

public class Resources {
    private Long id;
    private String name;
    private String description;
    private String type;
    private String location;
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
}
