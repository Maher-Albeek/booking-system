package com.maher.booking_system.mapper;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.model.Booking;

public final class BookingMapper {

    private BookingMapper() {
    }

    public static BookingResponse toResponse(Booking booking) {
        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setUserId(booking.getUserId());
        response.setResourceId(booking.getResourceId());
        response.setTimeSlotId(booking.getTimeSlotId());
        response.setStartDateTime(booking.getStartDateTime());
        response.setEndDateTime(booking.getEndDateTime());
        response.setStatus(booking.getStatus() == null ? null : booking.getStatus().canonical().name());
        response.setBookingTime(booking.getBookingTime());
        response.setCustomerName(booking.getCustomerName());
        response.setServiceName(booking.getServiceName());
        response.setFirstName(booking.getFirstName());
        response.setLastName(booking.getLastName());
        response.setAddress(booking.getAddress());
        response.setBirthDate(booking.getBirthDate());
        response.setPaymentMethod(booking.getPaymentMethod());
        response.setPaymentStatus(booking.getPaymentStatus() == null ? null : booking.getPaymentStatus().name());
        response.setPayableAmountCents(booking.getPayableAmountCents());
        response.setPayableCurrency(booking.getPayableCurrency());
        response.setPaymentProvider(booking.getPaymentProvider());
        response.setAgreedToCancellationPolicy(booking.getAgreedToCancellationPolicy());
        response.setCancellationPolicyVersion(booking.getCancellationPolicyVersion());
        response.setCancellationRefundPercentage(booking.getCancellationRefundPercentage());
        response.setRefundedAmountCents(booking.getRefundedAmountCents());
        response.setRefundReason(booking.getRefundReason());
        response.setCancelledAt(booking.getCancelledAt());
        response.setDepositHoldStatus(booking.getDepositHoldStatus() == null ? null : booking.getDepositHoldStatus().name());
        response.setDepositHoldAmountCents(booking.getDepositHoldAmountCents());
        response.setDepositHoldProvider(booking.getDepositHoldProvider());
        response.setDepositHoldCreatedAt(booking.getDepositHoldCreatedAt());
        response.setDepositHoldReleasedAt(booking.getDepositHoldReleasedAt());
        return response;
    }
}
