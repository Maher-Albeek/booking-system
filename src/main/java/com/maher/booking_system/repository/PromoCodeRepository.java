package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.PromoCode;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public class PromoCodeRepository extends JsonRepositorySupport<PromoCode> {
    public PromoCodeRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "promo-codes.json", PromoCode.class, PromoCode::getId, PromoCode::setId);
    }

    public Optional<PromoCode> findByCode(String code) {
        if (code == null || code.isBlank()) {
            return Optional.empty();
        }
        String normalized = code.trim().toUpperCase();
        return findAll().stream().filter(p -> normalized.equals(p.getCode())).findFirst();
    }
}
