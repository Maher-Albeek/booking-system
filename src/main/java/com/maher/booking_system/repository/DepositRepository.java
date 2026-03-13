package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.DepositRecord;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public class DepositRepository extends JsonRepositorySupport<DepositRecord> {
    public DepositRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "deposits.json", DepositRecord.class, DepositRecord::getId, DepositRecord::setId);
    }

    public Optional<DepositRecord> findByBookingId(Long bookingId) {
        return findAll().stream().filter(d -> bookingId.equals(d.getBookingId())).findFirst();
    }
}
