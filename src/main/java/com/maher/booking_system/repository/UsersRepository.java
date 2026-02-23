package com.maher.booking_system.repository;

import com.maher.booking_system.model.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsersRepository extends JpaRepository<Users, Long> {
    // هنا تقدر تضيف استعلامات مخصصة لو بدك
    // مثال: List<users> findByStatus(String status);
}