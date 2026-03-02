package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
public class BookingRepository extends JsonRepositorySupport<Booking> {

    public BookingRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "bookings.json", Booking.class, Booking::getId, Booking::setId);
    }

    public boolean existsByTimeSlotIdAndStatus(Long timeSlotId, BookingStatus status) {
        return findAll().stream()
                .anyMatch(booking -> timeSlotId.equals(booking.getTimeSlotId()) && status == booking.getStatus());
    }
}
