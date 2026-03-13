package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.LoyaltyTransaction;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.List;

@Repository
public class LoyaltyTransactionRepository extends JsonRepositorySupport<LoyaltyTransaction> {
    public LoyaltyTransactionRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "loyalty-transactions.json", LoyaltyTransaction.class, LoyaltyTransaction::getId, LoyaltyTransaction::setId);
    }

    public List<LoyaltyTransaction> findByUserId(Long userId) {
        return findAll().stream().filter(t -> userId.equals(t.getUserId())).toList();
    }
}
