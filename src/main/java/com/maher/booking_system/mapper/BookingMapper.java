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
        response.setStatus(booking.getStatus());
        response.setBookingTime(booking.getBookingTime());
        response.setCustomerName(booking.getCustomerName());
        response.setServiceName(booking.getServiceName());
        return response;
    }
}
