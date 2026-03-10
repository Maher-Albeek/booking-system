package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.PaymentWebhookEvent;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;

@Repository
public class PaymentWebhookEventRepository extends JsonRepositorySupport<PaymentWebhookEvent> {
    public PaymentWebhookEventRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "payment-webhook-events.json", PaymentWebhookEvent.class, PaymentWebhookEvent::getId, PaymentWebhookEvent::setId);
    }

    public boolean existsByProviderAndEventId(String provider, String eventId) {
        return findAll().stream()
                .anyMatch(event -> provider.equals(event.getProvider()) && eventId.equals(event.getEventId()));
    }
}
