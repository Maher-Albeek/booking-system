package com.maher.booking_system.repository;

import com.maher.booking_system.model.TimeSlot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TimeSlotRepository extends JpaRepository<TimeSlot, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select ts from TimeSlot ts where ts.id = :id")
    Optional<TimeSlot> findByIdForUpdate(@Param("id") Long id);

    List<TimeSlot> findByResourceId(Long resourceId);

    List<TimeSlot> findByResourceIdAndAvailable(Long resourceId, boolean available);
}
