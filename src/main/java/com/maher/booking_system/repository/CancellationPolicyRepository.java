package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.CancellationPolicy;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
public class CancellationPolicyRepository extends JsonRepositorySupport<CancellationPolicy> {
    public CancellationPolicyRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(
                objectMapper,
                Path.of(storageDirectory),
                "cancellation-policies.json",
                CancellationPolicy.class,
                CancellationPolicy::getId,
                CancellationPolicy::setId
        );
    }
}
