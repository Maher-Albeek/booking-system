package com.maher.booking_system.service;

import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.ConflictException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.BookingRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class BookingService {
    private static final Set<BookingStatus> ACTIVE_BOOKING_STATUSES = EnumSet.of(
            BookingStatus.PENDING,
            BookingStatus.ACTIVE,
            BookingStatus.COMPLETED
    );

    private final BookingRepository bookingRepository;
    private final BranchService branchService;
    private final PromoCodeService promoCodeService;
    private final OfferCampaignService offerCampaignService;
    private final NotificationLogService notificationLogService;
    private final LoyaltyService loyaltyService;

    public BookingService(
            BookingRepository bookingRepository,
            BranchService branchService,
            PromoCodeService promoCodeService,
            OfferCampaignService offerCampaignService,
            NotificationLogService notificationLogService,
            LoyaltyService loyaltyService
    ) {
        this.bookingRepository = bookingRepository;
        this.branchService = branchService;
        this.promoCodeService = promoCodeService;
        this.offerCampaignService = offerCampaignService;
        this.notificationLogService = notificationLogService;
        this.loyaltyService = loyaltyService;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public @NonNull Booking getBookingById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
    }

    public @NonNull Booking createBookingAfterPaymentConfirmation(
            @NonNull CreateBookingRequest request,
            @NonNull String paymentProvider,
            @NonNull Long amountCents,
            @NonNull String currency
    ) {
        CreateBookingRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        String safeProvider = normalizeRequiredText(paymentProvider, "paymentProvider");
        String safeCurrency = normalizeRequiredText(currency, "currency").toUpperCase();
        if (amountCents <= 0) {
            throw new BadRequestException("payable amount must be greater than zero");
        }

        LocalDateTime requestedStart = normalizeDateTime(safeRequest.getStartDateTime(), "startDateTime");
        LocalDateTime requestedEnd = normalizeDateTime(safeRequest.getEndDateTime(), "endDateTime");

        if (!requestedStart.isBefore(requestedEnd)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }
        branchService.validateBookingWindow(safeRequest.getBranchId(), requestedStart, requestedEnd);

        boolean carAlreadyBookedInPeriod = bookingRepository.existsOverlapping(
                safeRequest.getResourceId(),
                requestedStart,
                requestedEnd,
                ACTIVE_BOOKING_STATUSES
        );
        if (carAlreadyBookedInPeriod || !isCarAvailableForRange(safeRequest.getResourceId(), requestedStart, requestedEnd)) {
            throw new ConflictException("Car is already booked for the selected period");
        }

        Booking booking = new Booking();
        booking.setUserId(safeRequest.getUserId());
        booking.setResourceId(safeRequest.getResourceId());
        booking.setBranchId(safeRequest.getBranchId());
        booking.setOfferId(safeRequest.getOfferId());
        booking.setPromoCode(safeRequest.getPromoCode());
        booking.setAirportPickup(safeRequest.isAirportPickup());
        booking.setAirportPickupFeeCents(0L);
        booking.setStartDateTime(requestedStart);
        booking.setEndDateTime(requestedEnd);
        booking.setFirstName(normalizeRequiredText(safeRequest.getFirstName(), "firstName"));
        booking.setLastName(normalizeRequiredText(safeRequest.getLastName(), "lastName"));
        booking.setAddress(normalizeRequiredText(safeRequest.getAddress(), "address"));
        booking.setBirthDate(normalizeBirthDate(safeRequest.getBirthDate()));
        booking.setPaymentMethod(PaymentMethodCatalog.normalizeRequired(safeRequest.getPaymentMethod(), "paymentMethod"));
        booking.setPaymentStatus(PaymentStatus.SUCCEEDED);
        booking.setPaymentProvider(safeProvider);
        booking.setPayableAmountCents(amountCents);
        booking.setPayableCurrency(safeCurrency);
        booking.setCustomerName(buildCustomerName(booking.getFirstName(), booking.getLastName()));
        booking.setServiceName(normalizeRequiredText(safeRequest.getServiceName(), "serviceName"));
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());

        Booking saved = bookingRepository.saveIfNoOverlap(booking);
        if (saved == null) {
            throw new ConflictException("Car was booked by another payment confirmation. Please choose another time.");
        }
        if (saved.getPromoCode() != null && !saved.getPromoCode().isBlank()) {
            promoCodeService.consume(saved.getPromoCode());
        }
        if (saved.getOfferId() != null) {
            offerCampaignService.recordAttributedBooking(saved.getOfferId(), saved.getPayableAmountCents());
        }
        notificationLogService.sendBookingConfirmation(saved);
        return saved;
    }

    public boolean isCarAvailableForRange(Long resourceId, LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        Objects.requireNonNull(requestedStart, "requestedStart must not be null");
        Objects.requireNonNull(requestedEnd, "requestedEnd must not be null");
        if (!requestedStart.isBefore(requestedEnd)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }
        return !bookingRepository.existsOverlapping(resourceId, requestedStart, requestedEnd, ACTIVE_BOOKING_STATUSES);
    }

    public List<Booking> getBookingsForAdmin(
            String fromIso,
            String toIso,
            String statusValue,
            Long resourceId,
            Long userId,
            String paymentStatusValue
    ) {
        LocalDateTime from = parseOptionalDateTime(fromIso, "from");
        LocalDateTime to = parseOptionalDateTime(toIso, "to");
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be before to");
        }

        BookingStatus status = parseOptionalBookingStatus(statusValue);
        PaymentStatus paymentStatus = parseOptionalPaymentStatus(paymentStatusValue);
        return bookingRepository.findByFilters(from, to, status, resourceId, userId, paymentStatus);
    }

    public void cancelBooking(@NonNull Long bookingId) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED || booking.getStatus() == BookingStatus.COMPLETED) {
            throw new BadRequestException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
    }

    public Booking updateStatus(Long bookingId, BookingStatus targetStatus) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");
        Booking booking = getBookingById(bookingId);

        if (!isValidTransition(booking.getStatus(), targetStatus)) {
            throw new BadRequestException("Invalid status transition: " + booking.getStatus() + " -> " + targetStatus);
        }
        booking.setStatus(targetStatus);
        Booking updated = bookingRepository.save(booking);
        if (updated.getStatus() == BookingStatus.COMPLETED) {
            loyaltyService.awardCompletedBookingPoints(updated);
        }
        return updated;
    }

    private boolean isValidTransition(BookingStatus current, BookingStatus target) {
        if (current == target) {
            return true;
        }
        return switch (current) {
            case PENDING -> target == BookingStatus.ACTIVE || target == BookingStatus.CANCELLED || target == BookingStatus.NO_SHOW;
            case ACTIVE -> target == BookingStatus.COMPLETED || target == BookingStatus.CANCELLED;
            case COMPLETED, CANCELLED, NO_SHOW -> false;
        };
    }

    private String normalizeRequiredText(String value, String fieldName) {
        if (value == null || value.trim().isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        return value.trim();
    }

    private String normalizeBirthDate(String value) {
        String normalized = normalizeRequiredText(value, "birthDate");
        try {
            return LocalDate.parse(normalized).toString();
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("birthDate must use the YYYY-MM-DD format");
        }
    }

    private LocalDateTime normalizeDateTime(String value, String fieldName) {
        String normalized = normalizeRequiredText(value, fieldName);
        try {
            return LocalDateTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(fieldName + " must use yyyy-MM-ddTHH:mm format");
        }
    }

    private String buildCustomerName(String firstName, String lastName) {
        return firstName + " " + lastName;
    }

    private LocalDateTime parseOptionalDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(fieldName + " must use yyyy-MM-ddTHH:mm format");
        }
    }

    private BookingStatus parseOptionalBookingStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return BookingStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported booking status: " + value);
        }
    }

    private PaymentStatus parseOptionalPaymentStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return PaymentStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported payment status: " + value);
        }
    }
}
