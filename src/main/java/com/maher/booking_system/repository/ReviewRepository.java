package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.Review;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class ReviewRepository extends JsonRepositorySupport<Review> {
    public ReviewRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "reviews.json", Review.class, Review::getId, Review::setId);
    }

    public List<Review> findByCarId(Long carId) {
        return findAll().stream().filter(r -> carId.equals(r.getCarId())).toList();
    }
}
