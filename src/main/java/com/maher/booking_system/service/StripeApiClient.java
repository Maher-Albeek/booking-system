package com.maher.booking_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class StripeApiClient {
    private static final String STRIPE_BASE_URL = "https://api.stripe.com/v1";
    private static final String PROVIDER = "stripe";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final String secretKey;

    public StripeApiClient(
            ObjectMapper objectMapper,
            @Value("${app.payment.stripe.secret-key:}") String secretKey
    ) {
        this.objectMapper = objectMapper;
        this.secretKey = secretKey == null ? "" : secretKey.trim();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public CheckoutSession createCheckoutSession(CheckoutSessionRequest request, String idempotencyKey) {
        ensureConfigured();

        Map<String, String> form = new LinkedHashMap<>();
        form.put("mode", "payment");
        form.put("success_url", request.successUrl());
        form.put("cancel_url", request.cancelUrl());
        form.put("currency", request.currency());
        form.put("client_reference_id", String.valueOf(request.bookingId()));
        form.put("metadata[booking_id]", String.valueOf(request.bookingId()));
        form.put("metadata[user_id]", String.valueOf(request.userId()));
        form.put("payment_intent_data[metadata][booking_id]", String.valueOf(request.bookingId()));
        form.put("payment_intent_data[setup_future_usage]", request.savePaymentMethod() ? "off_session" : "");
        form.put("line_items[0][quantity]", "1");
        form.put("line_items[0][price_data][currency]", request.currency());
        form.put("line_items[0][price_data][unit_amount]", String.valueOf(request.amountCents()));
        form.put("line_items[0][price_data][product_data][name]", request.description());
        form.put("line_items[0][price_data][product_data][metadata][resource_id]", String.valueOf(request.resourceId()));

        String body = encodeForm(form);

        HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(STRIPE_BASE_URL + "/checkout/sessions"))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + secretKey)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Idempotency-Key", idempotencyKey)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            JsonNode json = objectMapper.readTree(response.body());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = json.path("error").path("message").asText("Stripe checkout session creation failed");
                throw new BadRequestException("Payment provider rejected request: " + message);
            }

            return new CheckoutSession(
                    json.path("id").asText(),
                    json.path("url").asText(),
                    json.path("payment_intent").asText(null)
            );
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BadRequestException("Unable to connect to payment provider");
        } catch (IOException ex) {
            throw new BadRequestException("Unable to connect to payment provider");
        }
    }

    public String providerName() {
        return PROVIDER;
    }

    private void ensureConfigured() {
        if (secretKey.isBlank()) {
            throw new BadRequestException("Payment provider is not configured");
        }
    }

    private String encodeForm(Map<String, String> values) {
        List<String> parts = new ArrayList<>();
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            parts.add(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8)
                    + "="
                    + URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return String.join("&", parts);
    }

    public record CheckoutSessionRequest(
            Long bookingId,
            Long userId,
            Long resourceId,
            long amountCents,
            String currency,
            String description,
            String successUrl,
            String cancelUrl,
            boolean savePaymentMethod
    ) {
    }

    public record CheckoutSession(String sessionId, String checkoutUrl, String paymentIntentId) {
    }
}
