package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.FavoriteCar;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public class FavoriteCarRepository extends JsonRepositorySupport<FavoriteCar> {
    public FavoriteCarRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "favorites.json", FavoriteCar.class, FavoriteCar::getId, FavoriteCar::setId);
    }

    public List<FavoriteCar> findByUserId(Long userId) {
        return findAll().stream().filter(f -> userId.equals(f.getUserId())).toList();
    }

    public Optional<FavoriteCar> findByUserIdAndCarId(Long userId, Long carId) {
        return findAll().stream().filter(f -> userId.equals(f.getUserId()) && carId.equals(f.getCarId())).findFirst();
    }
}
