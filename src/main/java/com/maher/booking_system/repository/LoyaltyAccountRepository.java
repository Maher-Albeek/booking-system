package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.LoyaltyAccount;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public class LoyaltyAccountRepository extends JsonRepositorySupport<LoyaltyAccount> {
    public LoyaltyAccountRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "loyalty-accounts.json", LoyaltyAccount.class, LoyaltyAccount::getId, LoyaltyAccount::setId);
    }

    public Optional<LoyaltyAccount> findByUserId(Long userId) {
        return findAll().stream().filter(a -> userId.equals(a.getUserId())).findFirst();
    }
}
