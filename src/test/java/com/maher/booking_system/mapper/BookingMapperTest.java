package com.maher.booking_system.mapper;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.enums.BookingStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingMapperTest {

    @Test
    void toResponse_mapsLegacyConfirmedToPending() {
        Booking booking = new Booking();
        booking.setStatus(BookingStatus.CONFIRMED);

        BookingResponse response = BookingMapper.toResponse(booking);

        assertEquals("PENDING", response.getStatus());
    }

    @Test
    void toResponse_allowsNullStatus() {
        Booking booking = new Booking();
        booking.setStatus(null);

        BookingResponse response = BookingMapper.toResponse(booking);

        assertNull(response.getStatus());
    }
}
