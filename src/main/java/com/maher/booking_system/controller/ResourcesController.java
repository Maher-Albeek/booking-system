package com.maher.booking_system.controller;

import com.maher.booking_system.model.Resources;
import com.maher.booking_system.service.ResourcesService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
public class ResourcesController {
    private final ResourcesService resourcesService;

    public ResourcesController(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    // GET all resources
    @GetMapping
    public List<Resources> getAllResources() {
        return resourcesService.getAllResources();
    }

    // POST create resource
    @PostMapping
    public Resources createResource(@RequestBody Resources resource) {
        return resourcesService.createResource(resource);
    }

    @DeleteMapping("/{id}")
    public void deleteResource(@PathVariable Long id) {
        resourcesService.deleteResource(id);
    }
    
}
