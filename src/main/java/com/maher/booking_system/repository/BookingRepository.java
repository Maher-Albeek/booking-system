package com.maher.booking_system.repository;

import com.maher.booking_system.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.maher.booking_system.model.enums.BookingStatus;


@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

boolean existsByTimeSlotIdAndStatus(Long timeSlotId, BookingStatus status);}
