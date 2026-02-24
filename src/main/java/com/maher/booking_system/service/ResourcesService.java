package com.maher.booking_system.service;

import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.repository.ResourcesRepository;
@Service

public class ResourcesService {
    private final ResourcesRepository resourcesRepository;

    public ResourcesService(ResourcesRepository resourcesRepository) {
        this.resourcesRepository = resourcesRepository;
    }

    public @NonNull Resources createResource(@NonNull Resources resource) {
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        return resourcesRepository.save(safeResource);
    }

    public Resources getResourceById(long id) {
        return resourcesRepository.findById(id).orElse(null);
    }

    public void deleteResource(long id) {
        resourcesRepository.deleteById(id);
    }
    public java.util.List<Resources> getAllResources() {
        return resourcesRepository.findAll();
    }
}
