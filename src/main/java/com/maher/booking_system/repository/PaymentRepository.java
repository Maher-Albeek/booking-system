package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.PaymentRecord;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.Optional;

@Repository
public class PaymentRepository extends JsonRepositorySupport<PaymentRecord> {
    public PaymentRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "payments.json", PaymentRecord.class, PaymentRecord::getId, PaymentRecord::setId);
    }

    public Optional<PaymentRecord> findByIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(payment -> idempotencyKey.equals(payment.getIdempotencyKey()))
                .findFirst();
    }

    public Optional<PaymentRecord> findByProviderSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(payment -> sessionId.equals(payment.getProviderSessionId()))
                .findFirst();
    }

    public Optional<PaymentRecord> findByProviderPaymentIntentId(String paymentIntentId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            return Optional.empty();
        }
        return findAll().stream()
                .filter(payment -> paymentIntentId.equals(payment.getProviderPaymentIntentId()))
                .findFirst();
    }
}
