package com.maher.booking_system.model;

import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.DepositHoldStatus;
import com.maher.booking_system.model.enums.PaymentStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Booking {

    private Long id;
    private Long userId;
    private Long resourceId;
    private Long timeSlotId;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BookingStatus status;
    private LocalDateTime bookingTime;
    private String customerName;
    private String serviceName;
    private String firstName;
    private String lastName;
    private String address;
    private String birthDate;
    private String paymentMethod;
    private PaymentStatus paymentStatus;
    private Long payableAmountCents;
    private String payableCurrency;
    private String paymentProvider;
    private Boolean agreedToCancellationPolicy;
    private String cancellationPolicyVersion;
    private Integer cancellationRefundPercentage;
    private Long refundedAmountCents;
    private String refundReason;
    private LocalDateTime cancelledAt;
    private Long offerId;
    private DepositHoldStatus depositHoldStatus;
    private Long depositHoldAmountCents;
    private String depositHoldProvider;
    private LocalDateTime depositHoldCreatedAt;
    private LocalDateTime depositHoldReleasedAt;
    private Integer pickupOdometerKm;
    private String pickupFuelLevel;
    private String pickupNotes;
    private List<String> pickupPhotoUrls = new ArrayList<>();
    private LocalDateTime checkedInAt;
    private Integer returnOdometerKm;
    private LocalDateTime actualReturnDateTime;
    private String returnNotes;
    private List<String> returnPhotoUrls = new ArrayList<>();
    private List<DamageReportItem> damageReports = new ArrayList<>();
    private Long extraKmFeeCents;
    private Long lateFeeCents;
    private Long damageFeeCents;
    private Long finalTotalAmountCents;
    private String finalInvoiceNumber;
    private LocalDateTime finalInvoiceIssuedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getTimeSlotId() { return timeSlotId; }
    public void setTimeSlotId(Long timeSlotId) { this.timeSlotId = timeSlotId; }

    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }

    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }

    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }

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

    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }

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

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public DepositHoldStatus getDepositHoldStatus() { return depositHoldStatus; }
    public void setDepositHoldStatus(DepositHoldStatus depositHoldStatus) { this.depositHoldStatus = depositHoldStatus; }

    public Long getDepositHoldAmountCents() { return depositHoldAmountCents; }
    public void setDepositHoldAmountCents(Long depositHoldAmountCents) {
        this.depositHoldAmountCents = depositHoldAmountCents;
    }

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

    public Integer getPickupOdometerKm() { return pickupOdometerKm; }
    public void setPickupOdometerKm(Integer pickupOdometerKm) { this.pickupOdometerKm = pickupOdometerKm; }

    public String getPickupFuelLevel() { return pickupFuelLevel; }
    public void setPickupFuelLevel(String pickupFuelLevel) { this.pickupFuelLevel = normalizeText(pickupFuelLevel); }

    public String getPickupNotes() { return pickupNotes; }
    public void setPickupNotes(String pickupNotes) { this.pickupNotes = normalizeText(pickupNotes); }

    public List<String> getPickupPhotoUrls() {
        return pickupPhotoUrls == null ? List.of() : pickupPhotoUrls;
    }

    public void setPickupPhotoUrls(List<String> pickupPhotoUrls) {
        this.pickupPhotoUrls = normalizeUrls(pickupPhotoUrls);
    }

    public LocalDateTime getCheckedInAt() { return checkedInAt; }
    public void setCheckedInAt(LocalDateTime checkedInAt) { this.checkedInAt = checkedInAt; }

    public Integer getReturnOdometerKm() { return returnOdometerKm; }
    public void setReturnOdometerKm(Integer returnOdometerKm) { this.returnOdometerKm = returnOdometerKm; }

    public LocalDateTime getActualReturnDateTime() { return actualReturnDateTime; }
    public void setActualReturnDateTime(LocalDateTime actualReturnDateTime) { this.actualReturnDateTime = actualReturnDateTime; }

    public String getReturnNotes() { return returnNotes; }
    public void setReturnNotes(String returnNotes) { this.returnNotes = normalizeText(returnNotes); }

    public List<String> getReturnPhotoUrls() {
        return returnPhotoUrls == null ? List.of() : returnPhotoUrls;
    }

    public void setReturnPhotoUrls(List<String> returnPhotoUrls) {
        this.returnPhotoUrls = normalizeUrls(returnPhotoUrls);
    }

    public List<DamageReportItem> getDamageReports() {
        return damageReports == null ? List.of() : damageReports;
    }

    public void setDamageReports(List<DamageReportItem> damageReports) {
        this.damageReports = damageReports == null ? new ArrayList<>() : new ArrayList<>(damageReports);
    }

    public Long getExtraKmFeeCents() { return extraKmFeeCents; }
    public void setExtraKmFeeCents(Long extraKmFeeCents) { this.extraKmFeeCents = extraKmFeeCents; }

    public Long getLateFeeCents() { return lateFeeCents; }
    public void setLateFeeCents(Long lateFeeCents) { this.lateFeeCents = lateFeeCents; }

    public Long getDamageFeeCents() { return damageFeeCents; }
    public void setDamageFeeCents(Long damageFeeCents) { this.damageFeeCents = damageFeeCents; }

    public Long getFinalTotalAmountCents() { return finalTotalAmountCents; }
    public void setFinalTotalAmountCents(Long finalTotalAmountCents) { this.finalTotalAmountCents = finalTotalAmountCents; }

    public String getFinalInvoiceNumber() { return finalInvoiceNumber; }
    public void setFinalInvoiceNumber(String finalInvoiceNumber) { this.finalInvoiceNumber = normalizeText(finalInvoiceNumber); }

    public LocalDateTime getFinalInvoiceIssuedAt() { return finalInvoiceIssuedAt; }
    public void setFinalInvoiceIssuedAt(LocalDateTime finalInvoiceIssuedAt) { this.finalInvoiceIssuedAt = finalInvoiceIssuedAt; }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<String> normalizeUrls(List<String> values) {
        if (values == null) {
            return new ArrayList<>();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }
}
