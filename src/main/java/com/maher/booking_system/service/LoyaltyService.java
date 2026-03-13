package com.maher.booking_system.service;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.LoyaltyAccount;
import com.maher.booking_system.model.LoyaltyTransaction;
import com.maher.booking_system.repository.LoyaltyAccountRepository;
import com.maher.booking_system.repository.LoyaltyTransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class LoyaltyService {
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final LoyaltyTransactionRepository loyaltyTransactionRepository;
    private final long pointsPerEuro;

    public LoyaltyService(
            LoyaltyAccountRepository loyaltyAccountRepository,
            LoyaltyTransactionRepository loyaltyTransactionRepository,
            @Value("${app.loyalty.points-per-euro:1}") long pointsPerEuro
    ) {
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.loyaltyTransactionRepository = loyaltyTransactionRepository;
        this.pointsPerEuro = Math.max(1L, pointsPerEuro);
    }

    public void awardCompletedBookingPoints(Booking booking) {
        if (booking.getUserId() == null || booking.getPayableAmountCents() == null) {
            return;
        }
        boolean alreadyAwarded = loyaltyTransactionRepository.findByUserId(booking.getUserId()).stream()
                .anyMatch(t -> booking.getId().equals(t.getBookingId()));
        if (alreadyAwarded) {
            return;
        }

        long points = Math.max(1L, (booking.getPayableAmountCents() / 100L) * pointsPerEuro);
        LoyaltyAccount account = loyaltyAccountRepository.findByUserId(booking.getUserId()).orElseGet(() -> {
            LoyaltyAccount a = new LoyaltyAccount();
            a.setUserId(booking.getUserId());
            a.setPointsBalance(0L);
            return loyaltyAccountRepository.save(a);
        });
        account.setPointsBalance((account.getPointsBalance() == null ? 0L : account.getPointsBalance()) + points);
        loyaltyAccountRepository.save(account);

        LoyaltyTransaction tx = new LoyaltyTransaction();
        tx.setUserId(booking.getUserId());
        tx.setBookingId(booking.getId());
        tx.setPoints(points);
        tx.setReason("Completed booking");
        tx.setCreatedAt(LocalDateTime.now());
        loyaltyTransactionRepository.save(tx);
    }

    public LoyaltyAccount getAccount(Long userId) {
        return loyaltyAccountRepository.findByUserId(userId).orElseGet(() -> {
            LoyaltyAccount account = new LoyaltyAccount();
            account.setUserId(userId);
            account.setPointsBalance(0L);
            return loyaltyAccountRepository.save(account);
        });
    }

    public List<LoyaltyTransaction> getTransactions(Long userId) {
        return loyaltyTransactionRepository.findByUserId(userId);
    }
}
