package com.maher.booking_system.controller;

import com.maher.booking_system.model.FavoriteCar;
import com.maher.booking_system.model.Review;
import com.maher.booking_system.service.CustomerEngagementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customer")
public class CustomerController {
    private final CustomerEngagementService customerEngagementService;

    public CustomerController(CustomerEngagementService customerEngagementService) {
        this.customerEngagementService = customerEngagementService;
    }

    @GetMapping("/{userId}/favorites")
    public List<FavoriteCar> getFavorites(@PathVariable Long userId) {
        return customerEngagementService.getFavorites(userId);
    }

    @PostMapping("/{userId}/favorites/{carId}")
    public FavoriteCar addFavorite(@PathVariable Long userId, @PathVariable Long carId) {
        return customerEngagementService.addFavorite(userId, carId);
    }

    @DeleteMapping("/{userId}/favorites/{carId}")
    public void removeFavorite(@PathVariable Long userId, @PathVariable Long carId) {
        customerEngagementService.removeFavorite(userId, carId);
    }

    @PostMapping("/reviews")
    public Review createReview(@RequestBody Review review) {
        return customerEngagementService.createReview(review);
    }

    @GetMapping("/cars/{carId}/reviews")
    public List<Review> getReviews(@PathVariable Long carId) {
        return customerEngagementService.getReviewsForCar(carId);
    }
}
