package com.maher.booking_system.controller;

import com.maher.booking_system.dto.CreateCheckoutSessionRequest;
import com.maher.booking_system.dto.CreateCheckoutSessionResponse;
import com.maher.booking_system.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping("/config")
    public Map<String, String> getClientConfig() {
        return paymentService.clientConfig();
    }

    @PostMapping("/checkout-session")
    public CreateCheckoutSessionResponse createCheckoutSession(
            @Valid @RequestBody @NonNull CreateCheckoutSessionRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return paymentService.createCheckoutSession(request, idempotencyKey);
    }

    @PostMapping("/webhook/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String rawBody,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature
    ) {
        paymentService.processStripeWebhook(rawBody, signature);
        return ResponseEntity.ok().build();
    }
}
