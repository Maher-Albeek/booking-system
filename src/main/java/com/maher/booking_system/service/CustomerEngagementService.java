package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.model.FavoriteCar;
import com.maher.booking_system.model.Review;
import com.maher.booking_system.repository.FavoriteCarRepository;
import com.maher.booking_system.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class CustomerEngagementService {
    private final FavoriteCarRepository favoriteCarRepository;
    private final ReviewRepository reviewRepository;

    public CustomerEngagementService(FavoriteCarRepository favoriteCarRepository, ReviewRepository reviewRepository) {
        this.favoriteCarRepository = favoriteCarRepository;
        this.reviewRepository = reviewRepository;
    }

    public List<FavoriteCar> getFavorites(Long userId) {
        return favoriteCarRepository.findByUserId(userId);
    }

    public FavoriteCar addFavorite(Long userId, Long carId) {
        if (userId == null || carId == null) {
            throw new BadRequestException("userId and carId are required");
        }
        return favoriteCarRepository.findByUserIdAndCarId(userId, carId).orElseGet(() -> {
            FavoriteCar favoriteCar = new FavoriteCar();
            favoriteCar.setUserId(userId);
            favoriteCar.setCarId(carId);
            return favoriteCarRepository.save(favoriteCar);
        });
    }

    public void removeFavorite(Long userId, Long carId) {
        favoriteCarRepository.findByUserIdAndCarId(userId, carId).ifPresent(entry -> favoriteCarRepository.deleteById(entry.getId()));
    }

    public Review createReview(Review review) {
        Review safe = Objects.requireNonNull(review, "review must not be null");
        if (safe.getBookingId() == null || safe.getUserId() == null || safe.getCarId() == null) {
            throw new BadRequestException("bookingId, userId and carId are required");
        }
        if (safe.getRating() == null || safe.getRating() < 1 || safe.getRating() > 5) {
            throw new BadRequestException("rating must be between 1 and 5");
        }
        safe.setComment(safe.getComment() == null ? "" : safe.getComment().trim());
        safe.setCreatedAt(LocalDateTime.now());
        return reviewRepository.save(safe);
    }

    public List<Review> getReviewsForCar(Long carId) {
        return reviewRepository.findByCarId(carId);
    }
}
