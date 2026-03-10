package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.repository.support.LegalContentFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
public class LegalDraftRepository extends LegalContentFileRepository {

    public LegalDraftRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "legal-content-draft.json");
    }
}
