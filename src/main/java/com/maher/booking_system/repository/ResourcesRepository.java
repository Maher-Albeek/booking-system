package com.maher.booking_system.repository;

import com.maher.booking_system.model.Resources;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository

public interface ResourcesRepository extends JpaRepository<Resources, Long> {
    List<Resources> findByTypeAndActiveTrue(String type);
    
}
