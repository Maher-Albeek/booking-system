package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.FeeRecord;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class FeeRecordRepository extends JsonRepositorySupport<FeeRecord> {
    public FeeRecordRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "fees.json", FeeRecord.class, FeeRecord::getId, FeeRecord::setId);
    }

    public List<FeeRecord> findByBookingId(Long bookingId) {
        return findAll().stream().filter(f -> bookingId.equals(f.getBookingId())).toList();
    }
}
