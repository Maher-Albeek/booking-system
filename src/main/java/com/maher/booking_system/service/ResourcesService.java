package com.maher.booking_system.service;

import com.maher.booking_system.exception.NotFoundException;
import java.util.Objects;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.repository.ResourcesRepository;
@Service

public class ResourcesService {
    private final ResourcesRepository resourcesRepository;
    private final ResourcePhotoStorageService resourcePhotoStorageService;

    public ResourcesService(ResourcesRepository resourcesRepository, ResourcePhotoStorageService resourcePhotoStorageService) {
        this.resourcesRepository = resourcesRepository;
        this.resourcePhotoStorageService = resourcePhotoStorageService;
    }

    public @NonNull Resources createResource(@NonNull Resources resource) {
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");
        java.util.List<String> submittedPhotoUrls = safeResource.getPhotoUrls();

        safeResource.setPhotoUrls(submittedPhotoUrls.stream()
                .filter(photoUrl -> photoUrl != null && !photoUrl.startsWith("data:"))
                .toList());

        Resources savedResource = resourcesRepository.save(safeResource);
        savedResource.setPhotoUrls(resourcePhotoStorageService.resolvePhotoUrls(
                savedResource.getId(),
                submittedPhotoUrls,
                java.util.List.of()
        ));
        return resourcesRepository.save(savedResource);
    }

    public @NonNull Resources updateResource(@NonNull Long id, @NonNull Resources resource) {
        Objects.requireNonNull(id, "id must not be null");
        Resources safeResource = Objects.requireNonNull(resource, "resource must not be null");

        Resources existingResource = resourcesRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + id));

        safeResource.setId(id);
        safeResource.setPhotoUrls(resourcePhotoStorageService.resolvePhotoUrls(
                id,
                safeResource.getPhotoUrls(),
                existingResource.getPhotoUrls()
        ));
        return resourcesRepository.save(safeResource);
    }

    public Resources getResourceById(long id) {
        return resourcesRepository.findById(id).orElse(null);
    }

    public void deleteResource(long id) {
        resourcePhotoStorageService.deleteAllPhotos(id);
        resourcesRepository.deleteById(id);
    }
    public java.util.List<Resources> getAllResources() {
        return resourcesRepository.findAll();
    }
    public java.util.List<Resources> getCars() {
        return resourcesRepository.findByTypeAndActiveTrue("Car");
    }
}
