package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class ResourcesRepository extends JsonRepositorySupport<Resources> {

    public ResourcesRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "resources.json", Resources.class, Resources::getId, Resources::setId);
    }

    public List<Resources> findByTypeAndActiveTrue(String type) {
        return findAll().stream()
                .filter(resource -> resource.isActive())
                .filter(resource -> resource.getType() != null && resource.getType().equalsIgnoreCase(type))
                .toList();
    }
}
