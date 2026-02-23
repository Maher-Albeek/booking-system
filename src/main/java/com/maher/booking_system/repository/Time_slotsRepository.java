package com.maher.booking_system.repository;

import com.maher.booking_system.model.Time_slots;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface Time_slotsRepository extends JpaRepository<Time_slots, Long> {
    
}
