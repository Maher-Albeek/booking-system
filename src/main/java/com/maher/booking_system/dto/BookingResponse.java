package com.maher.booking_system.dto;

import java.time.LocalDateTime;

public class BookingResponse {

    private Long id;
    private Long userId;
    private Long resourceId;
    private Long branchId;
    private Long offerId;
    private String promoCode;
    private boolean airportPickup;
    private Long airportPickupFeeCents;
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

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getResourceId() { return resourceId; }
    public void setResourceId(Long resourceId) { this.resourceId = resourceId; }

    public Long getBranchId() { return branchId; }
    public void setBranchId(Long branchId) { this.branchId = branchId; }

    public Long getOfferId() { return offerId; }
    public void setOfferId(Long offerId) { this.offerId = offerId; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }

    public boolean isAirportPickup() { return airportPickup; }
    public void setAirportPickup(boolean airportPickup) { this.airportPickup = airportPickup; }

    public Long getAirportPickupFeeCents() { return airportPickupFeeCents; }
    public void setAirportPickupFeeCents(Long airportPickupFeeCents) { this.airportPickupFeeCents = airportPickupFeeCents; }

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
}
