package com.maher.booking_system.repository;

import com.maher.booking_system.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    boolean existsByResourceIdAndTimeSlotId(int resourceId, int timeSlotId);
    // هنا تقدر تضيف استعلامات مخصصة لو بدك
    // مثال: List<Booking> findByStatus(String status);
}