package com.maher.booking_system.model;

import com.maher.booking_system.model.enums.PaymentStatus;

import java.time.LocalDateTime;

public class PaymentRecord {
    private Long id;
    private Long bookingId;
    private Long userId;
    private String provider;
    private String providerSessionId;
    private String providerPaymentIntentId;
    private String idempotencyKey;
    private PaymentStatus status;
    private Long amountCents;
    private String currency;
    private Long refundedAmountCents;
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getBookingId() { return bookingId; }
    public void setBookingId(Long bookingId) { this.bookingId = bookingId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderSessionId() { return providerSessionId; }
    public void setProviderSessionId(String providerSessionId) { this.providerSessionId = providerSessionId; }

    public String getProviderPaymentIntentId() { return providerPaymentIntentId; }
    public void setProviderPaymentIntentId(String providerPaymentIntentId) { this.providerPaymentIntentId = providerPaymentIntentId; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }

    public Long getAmountCents() { return amountCents; }
    public void setAmountCents(Long amountCents) { this.amountCents = amountCents; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public Long getRefundedAmountCents() { return refundedAmountCents; }
    public void setRefundedAmountCents(Long refundedAmountCents) { this.refundedAmountCents = refundedAmountCents; }

    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
