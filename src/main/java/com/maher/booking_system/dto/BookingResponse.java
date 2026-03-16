package com.maher.booking_system.dto;

import java.time.LocalDateTime;

public class BookingResponse {

    private Long id;
    private Long userId;
    private Long resourceId;
    private Long offerId;
    private Long timeSlotId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String status;
    private LocalDateTime bookingTime;
    private String customerName;
    private String serviceName;
    private String firstName;
    private String lastName;
    private String address;
    private String birthDate;
    private String paymentMethod;
    private String paymentStatus;
    private Long payableAmountCents;
    private String payableCurrency;
    private String paymentProvider;
    private Boolean agreedToCancellationPolicy;
    private String cancellationPolicyVersion;
    private Integer cancellationRefundPercentage;
    private Long refundedAmountCents;
    private String refundReason;
    private LocalDateTime cancelledAt;
    private String depositHoldStatus;
    private Long depositHoldAmountCents;
    private String depositHoldProvider;
    private LocalDateTime depositHoldCreatedAt;
    private LocalDateTime depositHoldReleasedAt;
    private String confirmationEmailRecipient;
    private LocalDateTime confirmationEmailSentAt;
    private LocalDateTime returnReminderSentAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public Long getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Long timeSlotId) { this.timeSlotId = timeSlotId; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBookingTime() { return bookingTime; }
    public void setBookingTime(LocalDateTime bookingTime) { this.bookingTime = bookingTime; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public Long getPayableAmountCents() { return payableAmountCents; }
    public void setPayableAmountCents(Long payableAmountCents) { this.payableAmountCents = payableAmountCents; }

    public String getPayableCurrency() { return payableCurrency; }
    public void setPayableCurrency(String payableCurrency) { this.payableCurrency = payableCurrency; }

    public String getPaymentProvider() { return paymentProvider; }
    public void setPaymentProvider(String paymentProvider) { this.paymentProvider = paymentProvider; }

    public Boolean getAgreedToCancellationPolicy() { return agreedToCancellationPolicy; }
    public void setAgreedToCancellationPolicy(Boolean agreedToCancellationPolicy) {
        this.agreedToCancellationPolicy = agreedToCancellationPolicy;
    }

    public String getCancellationPolicyVersion() { return cancellationPolicyVersion; }
    public void setCancellationPolicyVersion(String cancellationPolicyVersion) {
        this.cancellationPolicyVersion = cancellationPolicyVersion;
    }

    public Integer getCancellationRefundPercentage() { return cancellationRefundPercentage; }
    public void setCancellationRefundPercentage(Integer cancellationRefundPercentage) {
        this.cancellationRefundPercentage = cancellationRefundPercentage;
    }

    public Long getRefundedAmountCents() { return refundedAmountCents; }
    public void setRefundedAmountCents(Long refundedAmountCents) { this.refundedAmountCents = refundedAmountCents; }

    public String getRefundReason() { return refundReason; }
    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }

    public String getDepositHoldStatus() { return depositHoldStatus; }
    public void setDepositHoldStatus(String depositHoldStatus) { this.depositHoldStatus = depositHoldStatus; }

    public Long getDepositHoldAmountCents() { return depositHoldAmountCents; }
    public void setDepositHoldAmountCents(Long depositHoldAmountCents) { this.depositHoldAmountCents = depositHoldAmountCents; }

    public String getDepositHoldProvider() { return depositHoldProvider; }
    public void setDepositHoldProvider(String depositHoldProvider) { this.depositHoldProvider = depositHoldProvider; }

    public LocalDateTime getDepositHoldCreatedAt() { return depositHoldCreatedAt; }
    public void setDepositHoldCreatedAt(LocalDateTime depositHoldCreatedAt) {
        this.depositHoldCreatedAt = depositHoldCreatedAt;
    }

    public LocalDateTime getDepositHoldReleasedAt() { return depositHoldReleasedAt; }
    public void setDepositHoldReleasedAt(LocalDateTime depositHoldReleasedAt) {
        this.depositHoldReleasedAt = depositHoldReleasedAt;
    }

    public String getConfirmationEmailRecipient() { return confirmationEmailRecipient; }
    public void setConfirmationEmailRecipient(String confirmationEmailRecipient) {
        this.confirmationEmailRecipient = confirmationEmailRecipient;
    }

    public LocalDateTime getConfirmationEmailSentAt() { return confirmationEmailSentAt; }
    public void setConfirmationEmailSentAt(LocalDateTime confirmationEmailSentAt) {
        this.confirmationEmailSentAt = confirmationEmailSentAt;
    }

    public LocalDateTime getReturnReminderSentAt() { return returnReminderSentAt; }
    public void setReturnReminderSentAt(LocalDateTime returnReminderSentAt) {
        this.returnReminderSentAt = returnReminderSentAt;
    }
}
