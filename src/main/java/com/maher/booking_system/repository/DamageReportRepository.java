package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.DamageReport;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class DamageReportRepository extends JsonRepositorySupport<DamageReport> {
    public DamageReportRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "damage-reports.json", DamageReport.class, DamageReport::getId, DamageReport::setId);
    }

    public List<DamageReport> findByBookingId(Long bookingId) {
        return findAll().stream().filter(d -> bookingId.equals(d.getBookingId())).toList();
    }
}
