package com.maher.booking_system.controller;

import com.maher.booking_system.model.LoyaltyAccount;
import com.maher.booking_system.model.LoyaltyTransaction;
import com.maher.booking_system.service.LoyaltyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {
    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/{userId}")
    public LoyaltyAccount getAccount(@PathVariable Long userId) {
        return loyaltyService.getAccount(userId);
    }

    @GetMapping("/{userId}/transactions")
    public List<LoyaltyTransaction> getTransactions(@PathVariable Long userId) {
        return loyaltyService.getTransactions(userId);
    }
}
