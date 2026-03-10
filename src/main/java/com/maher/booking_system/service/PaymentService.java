package com.maher.booking_system.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.dto.CreateCheckoutSessionRequest;
import com.maher.booking_system.dto.CreateCheckoutSessionResponse;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.PaymentRecord;
import com.maher.booking_system.model.PaymentWebhookEvent;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.PaymentRepository;
import com.maher.booking_system.repository.PaymentWebhookEventRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {
    private static final String PROVIDER = "stripe";

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentWebhookEventRepository paymentWebhookEventRepository;
    private final ResourcesRepository resourcesRepository;
    private final StripeApiClient stripeApiClient;
    private final ObjectMapper objectMapper;
    private final String webhookSecret;
    private final String publishableKey;
    private final String defaultCurrency;

    public PaymentService(
            BookingRepository bookingRepository,
            PaymentRepository paymentRepository,
            PaymentWebhookEventRepository paymentWebhookEventRepository,
            ResourcesRepository resourcesRepository,
            StripeApiClient stripeApiClient,
            ObjectMapper objectMapper,
            @Value("${app.payment.stripe.webhook-secret:}") String webhookSecret,
            @Value("${app.payment.stripe.publishable-key:}") String publishableKey,
            @Value("${app.payment.currency:eur}") String defaultCurrency
    ) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.paymentWebhookEventRepository = paymentWebhookEventRepository;
        this.resourcesRepository = resourcesRepository;
        this.stripeApiClient = stripeApiClient;
        this.objectMapper = objectMapper;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
        this.publishableKey = publishableKey == null ? "" : publishableKey.trim();
        this.defaultCurrency = (defaultCurrency == null ? "eur" : defaultCurrency.trim()).toLowerCase(Locale.ROOT);
    }

    public synchronized CreateCheckoutSessionResponse createCheckoutSession(CreateCheckoutSessionRequest request, String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BadRequestException("Idempotency-Key header is required");
        }

        PaymentRecord existingPayment = paymentRepository.findByIdempotencyKey(idempotencyKey).orElse(null);
        if (existingPayment != null) {
            return new CreateCheckoutSessionResponse(
                    existingPayment.getBookingId(),
                    existingPayment.getStatus().name(),
                    existingPayment.getProviderSessionId(),
                    null
            );
        }

        CreateBookingRequest bookingRequest = Objects.requireNonNull(request.getBooking(), "booking is required");
        Pricing pricing = calculatePricing(bookingRequest.getResourceId(), bookingRequest.getStartDateTime(), bookingRequest.getEndDateTime());
        Booking booking = createPendingBooking(bookingRequest, pricing);

        StripeApiClient.CheckoutSession checkoutSession;
        try {
            checkoutSession = stripeApiClient.createCheckoutSession(
                    new StripeApiClient.CheckoutSessionRequest(
                            booking.getId(),
                            booking.getUserId(),
                            booking.getResourceId(),
                            pricing.amountCents(),
                            pricing.currency(),
                            "Car booking #" + booking.getId(),
                            request.getSuccessUrl(),
                            request.getCancelUrl(),
                            request.isSavePaymentMethod()
                    ),
                    idempotencyKey
            );
        } catch (RuntimeException ex) {
            booking.setPaymentStatus(PaymentStatus.FAILED);
            bookingRepository.save(booking);
            throw ex;
        }

        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setBookingId(booking.getId());
        paymentRecord.setUserId(booking.getUserId());
        paymentRecord.setProvider(PROVIDER);
        paymentRecord.setProviderSessionId(checkoutSession.sessionId());
        paymentRecord.setProviderPaymentIntentId(checkoutSession.paymentIntentId());
        paymentRecord.setIdempotencyKey(idempotencyKey);
        paymentRecord.setStatus(PaymentStatus.PENDING);
        paymentRecord.setAmountCents(pricing.amountCents());
        paymentRecord.setCurrency(pricing.currency());
        paymentRecord.setRefundedAmountCents(0L);
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(paymentRecord);

        booking.setPaymentProvider(PROVIDER);
        bookingRepository.save(booking);

        return new CreateCheckoutSessionResponse(
                booking.getId(),
                booking.getPaymentStatus().name(),
                checkoutSession.sessionId(),
                checkoutSession.checkoutUrl()
        );
    }

    public synchronized void processStripeWebhook(String rawBody, String signatureHeader) {
        verifyWebhookSignature(rawBody, signatureHeader);

        JsonNode event;
        try {
            event = objectMapper.readTree(rawBody);
        } catch (Exception ex) {
            throw new BadRequestException("Invalid webhook payload");
        }

        String eventId = event.path("id").asText();
        if (eventId.isBlank()) {
            throw new BadRequestException("Missing webhook event id");
        }
        if (paymentWebhookEventRepository.existsByProviderAndEventId(PROVIDER, eventId)) {
            return;
        }

        String type = event.path("type").asText();
        JsonNode dataObject = event.path("data").path("object");
        if (dataObject.isMissingNode()) {
            throw new BadRequestException("Webhook payload missing data.object");
        }

        switch (type) {
            case "checkout.session.completed" -> handleCheckoutCompleted(dataObject);
            case "checkout.session.expired" -> handleCheckoutExpired(dataObject);
            case "payment_intent.payment_failed" -> handlePaymentFailed(dataObject);
            case "charge.refunded" -> handleChargeRefunded(dataObject);
            default -> {
                // Ignore unknown events.
            }
        }

        PaymentWebhookEvent webhookEvent = new PaymentWebhookEvent();
        webhookEvent.setProvider(PROVIDER);
        webhookEvent.setEventId(eventId);
        webhookEvent.setProcessedAt(LocalDateTime.now());
        paymentWebhookEventRepository.save(webhookEvent);
    }

    public Map<String, String> clientConfig() {
        return Map.of(
                "provider", PROVIDER,
                "publishableKey", publishableKey,
                "currency", defaultCurrency
        );
    }

    private Pricing calculatePricing(Long resourceId, String startDateTime, String endDateTime) {
        if (resourceId == null) {
            throw new BadRequestException("resourceId is required");
        }
        Resources resource = resourcesRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + resourceId));
        if (resource.getDailyPrice() == null || resource.getDailyPrice() <= 0) {
            throw new BadRequestException("Resource does not have a valid daily price");
        }

        LocalDateTime start;
        LocalDateTime end;
        try {
            start = LocalDateTime.parse(startDateTime);
            end = LocalDateTime.parse(endDateTime);
        } catch (Exception ex) {
            throw new BadRequestException("startDateTime and endDateTime must use yyyy-MM-ddTHH:mm format");
        }
        if (!start.isBefore(end)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }

        long hours = ChronoUnit.HOURS.between(start, end);
        long days = Math.max(1L, (long) Math.ceil(hours / 24.0d));
        long amountCents = Math.round(resource.getDailyPrice() * 100.0d) * days;
        return new Pricing(amountCents, defaultCurrency, start, end);
    }

    private Booking createPendingBooking(CreateBookingRequest bookingRequest, Pricing pricing) {
        Booking booking = new Booking();
        booking.setUserId(bookingRequest.getUserId());
        booking.setResourceId(bookingRequest.getResourceId());
        booking.setTimeSlotId(bookingRequest.getTimeSlotId());
        booking.setStartDateTime(pricing.start());
        booking.setEndDateTime(pricing.end());
        booking.setFirstName(normalizeRequired(bookingRequest.getFirstName(), "firstName"));
        booking.setLastName(normalizeRequired(bookingRequest.getLastName(), "lastName"));
        booking.setAddress(normalizeRequired(bookingRequest.getAddress(), "address"));
        booking.setBirthDate(normalizeRequired(bookingRequest.getBirthDate(), "birthDate"));
        booking.setPaymentMethod(PaymentMethodCatalog.normalizeRequired(bookingRequest.getPaymentMethod(), "paymentMethod"));
        booking.setServiceName(normalizeRequired(bookingRequest.getServiceName(), "serviceName"));
        booking.setCustomerName(booking.getFirstName() + " " + booking.getLastName());
        booking.setStatus(BookingStatus.PENDING);
        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking.setPayableAmountCents(pricing.amountCents());
        booking.setPayableCurrency(pricing.currency());
        booking.setPaymentProvider(PROVIDER);
        booking.setBookingTime(LocalDateTime.now());
        return bookingRepository.save(booking);
    }

    private String normalizeRequired(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value.trim();
    }

    private void handleCheckoutCompleted(JsonNode session) {
        String sessionId = session.path("id").asText();
        PaymentRecord payment = paymentRepository.findByProviderSessionId(sessionId).orElse(null);
        if (payment == null) {
            return;
        }
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElse(null);
        if (booking == null) {
            return;
        }

        String paymentIntentId = session.path("payment_intent").asText("");
        payment.setProviderPaymentIntentId(paymentIntentId.isBlank() ? payment.getProviderPaymentIntentId() : paymentIntentId);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setPaymentStatus(PaymentStatus.SUCCEEDED);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
    }

    private void handleCheckoutExpired(JsonNode session) {
        String sessionId = session.path("id").asText();
        PaymentRecord payment = paymentRepository.findByProviderSessionId(sessionId).orElse(null);
        if (payment == null) {
            return;
        }
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElse(null);
        if (booking == null) {
            return;
        }

        payment.setStatus(PaymentStatus.FAILED);
        payment.setLastError("Checkout session expired");
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setPaymentStatus(PaymentStatus.FAILED);
        bookingRepository.save(booking);
    }

    private void handlePaymentFailed(JsonNode paymentIntent) {
        String paymentIntentId = paymentIntent.path("id").asText();
        PaymentRecord payment = paymentRepository.findByProviderPaymentIntentId(paymentIntentId).orElse(null);
        if (payment == null) {
            return;
        }
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElse(null);
        if (booking == null) {
            return;
        }

        String message = paymentIntent.path("last_payment_error").path("message").asText("Payment failed");
        payment.setStatus(PaymentStatus.FAILED);
        payment.setLastError(message);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setPaymentStatus(PaymentStatus.FAILED);
        bookingRepository.save(booking);
    }

    private void handleChargeRefunded(JsonNode charge) {
        String paymentIntentId = charge.path("payment_intent").asText();
        PaymentRecord payment = paymentRepository.findByProviderPaymentIntentId(paymentIntentId).orElse(null);
        if (payment == null) {
            return;
        }
        Booking booking = bookingRepository.findById(payment.getBookingId()).orElse(null);
        if (booking == null) {
            return;
        }

        long refunded = charge.path("amount_refunded").asLong(0L);
        payment.setStatus(PaymentStatus.REFUNDED);
        payment.setRefundedAmountCents(refunded);
        payment.setUpdatedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        booking.setPaymentStatus(PaymentStatus.REFUNDED);
        bookingRepository.save(booking);
    }

    private void verifyWebhookSignature(String payload, String signatureHeader) {
        if (webhookSecret.isBlank()) {
            throw new BadRequestException("Webhook secret is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new BadRequestException("Missing Stripe-Signature header");
        }

        String timestamp = null;
        String signature = null;
        for (String part : signatureHeader.split(",")) {
            String[] pair = part.split("=", 2);
            if (pair.length != 2) {
                continue;
            }
            if ("t".equals(pair[0])) {
                timestamp = pair[1];
            } else if ("v1".equals(pair[0])) {
                signature = pair[1];
            }
        }
        if (timestamp == null || signature == null) {
            throw new BadRequestException("Invalid Stripe-Signature header");
        }

        String signedPayload = timestamp + "." + payload;
        String expected = hmacSha256(webhookSecret, signedPayload);
        if (!expected.equalsIgnoreCase(signature)) {
            throw new BadRequestException("Invalid webhook signature");
        }
    }

    private String hmacSha256(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new BadRequestException("Failed to validate webhook signature");
        }
    }

    private record Pricing(long amountCents, String currency, LocalDateTime start, LocalDateTime end) {
    }
}
