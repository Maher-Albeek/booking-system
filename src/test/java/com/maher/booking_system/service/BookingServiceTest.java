package com.maher.booking_system.service;

import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.exception.ConflictException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.repository.BookingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BranchService branchService;
    @Mock
    private PromoCodeService promoCodeService;
    @Mock
    private OfferCampaignService offerCampaignService;
    @Mock
    private NotificationLogService notificationLogService;
    @Mock
    private LoyaltyService loyaltyService;

    @InjectMocks
    private BookingService bookingService;

    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        request = new CreateBookingRequest();
        request.setUserId(1L);
        request.setResourceId(10L);
        request.setStartDateTime("2026-04-01T10:00");
        request.setEndDateTime("2026-04-03T10:00");
        request.setFirstName("Jane");
        request.setLastName("Doe");
        request.setAddress("Street 1");
        request.setBirthDate("1990-06-15");
        request.setPaymentMethod("visa");
        request.setServiceName("Car rental");
    }

    @Test
    void createBookingAfterPaymentConfirmation_ShouldCreatePendingBooking_WhenRangeIsAvailable() {
        when(bookingRepository.existsOverlapping(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), any(Set.class)))
                .thenReturn(false);
        when(bookingRepository.saveIfNoOverlap(any(Booking.class)))
                .thenAnswer(invocation -> {
                    Booking booking = invocation.getArgument(0);
                    booking.setId(99L);
                    return booking;
                });

        Booking result = bookingService.createBookingAfterPaymentConfirmation(request, "stripe", 25000L, "eur");

        assertEquals(99L, result.getId());
        assertEquals(BookingStatus.PENDING, result.getStatus());
        assertEquals("EUR", result.getPayableCurrency());
        assertEquals(25000L, result.getPayableAmountCents());
    }

    @Test
    void createBookingAfterPaymentConfirmation_ShouldThrowConflict_WhenRangeAlreadyBooked() {
        when(bookingRepository.existsOverlapping(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), any(Set.class)))
                .thenReturn(true);

        assertThrows(
                ConflictException.class,
                () -> bookingService.createBookingAfterPaymentConfirmation(request, "stripe", 25000L, "eur")
        );
    }

    @Test
    void createBookingAfterPaymentConfirmation_ShouldThrowConflict_WhenConcurrentInsertLosesRace() {
        when(bookingRepository.existsOverlapping(eq(10L), any(LocalDateTime.class), any(LocalDateTime.class), any(Set.class)))
                .thenReturn(false);
        when(bookingRepository.saveIfNoOverlap(any(Booking.class))).thenReturn(null);

        ConflictException conflict = assertThrows(
                ConflictException.class,
                () -> bookingService.createBookingAfterPaymentConfirmation(request, "stripe", 25000L, "eur")
        );
        assertTrue(conflict.getMessage().contains("another payment confirmation"));
    }
}
