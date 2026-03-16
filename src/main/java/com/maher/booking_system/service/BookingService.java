package com.maher.booking_system.service;

import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.dto.UpdateBookingRequest;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.ConflictException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.TimeSlot;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.TimeSlotRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;

@Service
public class BookingService {

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;
    private final PaymentService paymentService;
    private final OfferPageService offerPageService;
    private final BookingNotificationService bookingNotificationService;

    public BookingService(
            BookingRepository bookingRepository,
            TimeSlotRepository timeSlotRepository,
            PaymentService paymentService,
            OfferPageService offerPageService,
            BookingNotificationService bookingNotificationService
    ) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
        this.paymentService = paymentService;
        this.offerPageService = offerPageService;
        this.bookingNotificationService = bookingNotificationService;
    }

    public List<Booking> getAllBookings() {
        bookingNotificationService.dispatchDueReturnReminders();
        return bookingRepository.findAll();
    }

    public @NonNull Booking getBookingById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        bookingNotificationService.dispatchDueReturnReminders();
        return bookingRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + id));
    }

    public synchronized @NonNull Booking createBooking(@NonNull CreateBookingRequest request) {
        CreateBookingRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        LocalDateTime requestedStart = normalizeDateTime(safeRequest.getStartDateTime(), "startDateTime");
        LocalDateTime requestedEnd = normalizeDateTime(safeRequest.getEndDateTime(), "endDateTime");

        if (!requestedStart.isBefore(requestedEnd)) {
            throw new BadRequestException("startDateTime must be before endDateTime");
        }

        offerPageService.validateOfferAttribution(safeRequest.getOfferId(), safeRequest.getResourceId());

        TimeSlot selectedSlot = null;
        if (safeRequest.getTimeSlotId() != null) {
            selectedSlot = timeSlotRepository.findByIdForUpdate(safeRequest.getTimeSlotId())
                    .orElseThrow(() -> new NotFoundException("Time slot not found with id: " + safeRequest.getTimeSlotId()));

            if (!selectedSlot.getResourceId().equals(safeRequest.getResourceId())) {
                throw new BadRequestException("Time slot does not belong to resource id: " + safeRequest.getResourceId());
            }

            if (!selectedSlot.isAvailable()) {
                throw new ConflictException("Time slot is not available");
            }

            boolean alreadyBooked = bookingRepository.findByTimeSlotId(safeRequest.getTimeSlotId()).stream()
                    .anyMatch(this::isBlockingBooking);
            if (alreadyBooked) {
                throw new ConflictException("Time slot already booked");
            }
        }

        boolean carAlreadyBookedInPeriod = bookingRepository.findByResourceId(safeRequest.getResourceId())
                .stream()
                .filter(this::isBlockingBooking)
                .map(this::resolveBookingRange)
                .filter(Objects::nonNull)
                .anyMatch(range -> rangesOverlap(requestedStart, requestedEnd, range.start(), range.end()));

        if (carAlreadyBookedInPeriod) {
            throw new ConflictException("Car is already booked for the selected period");
        }

        Booking booking = new Booking();
        booking.setUserId(safeRequest.getUserId());
        booking.setResourceId(safeRequest.getResourceId());
        booking.setOfferId(safeRequest.getOfferId());
        booking.setTimeSlotId(safeRequest.getTimeSlotId());
        booking.setStartDateTime(requestedStart);
        booking.setEndDateTime(requestedEnd);
        booking.setFirstName(normalizeRequiredText(safeRequest.getFirstName(), "firstName"));
        booking.setLastName(normalizeRequiredText(safeRequest.getLastName(), "lastName"));
        booking.setAddress(normalizeRequiredText(safeRequest.getAddress(), "address"));
        booking.setBirthDate(normalizeBirthDate(safeRequest.getBirthDate()));
        booking.setPaymentMethod(PaymentMethodCatalog.normalizeRequired(safeRequest.getPaymentMethod(), "paymentMethod"));
        booking.setPaymentStatus(PaymentStatus.SUCCEEDED);
        booking.setPaymentProvider("manual");
        booking.setCustomerName(buildCustomerName(booking.getFirstName(), booking.getLastName()));
        booking.setServiceName(normalizeRequiredText(safeRequest.getServiceName(), "serviceName"));
        booking.setStatus(BookingStatus.PENDING);
        booking.setBookingTime(LocalDateTime.now());
        Booking savedBooking = bookingRepository.save(booking);
        syncTimeSlotAvailability(savedBooking.getTimeSlotId(), selectedSlot);
        return bookingNotificationService.sendBookingConfirmation(savedBooking);
    }

    public List<TimeSlot> getTimeSlotsByResource(@NonNull Long resourceId, Boolean available) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        if (available == null) {
            return timeSlotRepository.findByResourceId(resourceId);
        }
        return timeSlotRepository.findByResourceIdAndAvailable(resourceId, available);
    }

    public synchronized @NonNull Booking cancelBooking(@NonNull Long bookingId) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        return updateBookingStatus(bookingId, BookingStatus.CANCELLED);
    }

    public synchronized @NonNull Booking updateBooking(@NonNull Long bookingId, @NonNull UpdateBookingRequest request) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        UpdateBookingRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingStatus previousStatus = booking.getStatus() == null ? null : booking.getStatus().canonical();

        boolean changed = false;

        if (safeRequest.getEndDateTime() != null && !safeRequest.getEndDateTime().trim().isBlank()) {
            LocalDateTime updatedEnd = normalizeDateTime(safeRequest.getEndDateTime(), "endDateTime");
            if (!booking.getStartDateTime().isBefore(updatedEnd)) {
                throw new BadRequestException("endDateTime must be after startDateTime");
            }
            validateNoOverlap(booking, booking.getStartDateTime(), updatedEnd);
            booking.setEndDateTime(updatedEnd);
            changed = true;
        }

        if (safeRequest.getStatus() != null && !safeRequest.getStatus().trim().isBlank()) {
            BookingStatus targetStatus = normalizeStatus(safeRequest.getStatus());
            applyStatusTransition(booking, targetStatus);
            changed = true;
        }

        if (!changed) {
            throw new BadRequestException("At least one booking field must be provided for update");
        }

        Booking savedBooking = bookingRepository.save(booking);
        paymentService.handleStatusTransition(savedBooking, previousStatus);
        savedBooking = bookingRepository.save(savedBooking);
        syncTimeSlotAvailability(savedBooking.getTimeSlotId(), null);
        return savedBooking;
    }

    public synchronized @NonNull Booking updateBookingStatus(@NonNull Long bookingId, @NonNull BookingStatus targetStatus) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");
        Objects.requireNonNull(targetStatus, "targetStatus must not be null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        BookingStatus previousStatus = booking.getStatus() == null ? null : booking.getStatus().canonical();
        applyStatusTransition(booking, targetStatus);
        Booking savedBooking;
        if (targetStatus.canonical() == BookingStatus.CANCELLED) {
            savedBooking = paymentService.processBookingCancellation(booking);
        } else {
            paymentService.handleStatusTransition(booking, previousStatus);
            savedBooking = bookingRepository.save(booking);
        }
        syncTimeSlotAvailability(savedBooking.getTimeSlotId(), null);
        return savedBooking;
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

    private BookingStatus normalizeStatus(String value) {
        String normalized = normalizeRequiredText(value, "status").replace('-', '_').toUpperCase();
        try {
            return BookingStatus.valueOf(normalized).canonical();
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported booking status: " + value);
        }
    }

    private void applyStatusTransition(Booking booking, BookingStatus targetStatus) {
        BookingStatus currentStatus = booking.getStatus() == null ? BookingStatus.PENDING : booking.getStatus().canonical();
        BookingStatus normalizedTarget = targetStatus.canonical();

        if (!currentStatus.canTransitionTo(normalizedTarget)) {
            throw new BadRequestException("Invalid booking status transition from "
                    + currentStatus.name() + " to " + normalizedTarget.name());
        }

        booking.setStatus(normalizedTarget);
    }

    private void validateNoOverlap(Booking booking, LocalDateTime requestedStart, LocalDateTime requestedEnd) {
        boolean overlaps = bookingRepository.findByResourceId(booking.getResourceId()).stream()
                .filter(existing -> !Objects.equals(existing.getId(), booking.getId()))
                .filter(this::isBlockingBooking)
                .map(this::resolveBookingRange)
                .filter(Objects::nonNull)
                .anyMatch(range -> rangesOverlap(requestedStart, requestedEnd, range.start(), range.end()));

        if (overlaps) {
            throw new ConflictException("Car is already booked for the selected period");
        }
    }

    private boolean isBlockingBooking(Booking booking) {
        return booking.getStatus() != null && booking.getStatus().blocksAvailability();
    }

    private void syncTimeSlotAvailability(Long timeSlotId, TimeSlot lockedSlot) {
        if (timeSlotId == null) {
            return;
        }

        TimeSlot slot = lockedSlot;
        if (slot == null) {
            slot = timeSlotRepository.findByIdForUpdate(timeSlotId).orElse(null);
        }
        if (slot == null) {
            return;
        }

        boolean blocked = bookingRepository.findByTimeSlotId(timeSlotId).stream().anyMatch(this::isBlockingBooking);
        slot.setAvailable(!blocked);
        timeSlotRepository.save(slot);
    }

    private BookingRange resolveBookingRange(Booking booking) {
        if (booking.getStartDateTime() != null && booking.getEndDateTime() != null) {
            if (booking.getStartDateTime().isBefore(booking.getEndDateTime())) {
                return new BookingRange(booking.getStartDateTime(), booking.getEndDateTime());
            }
            return null;
        }

        Long slotId = booking.getTimeSlotId();
        if (slotId == null) {
            return null;
        }

        return timeSlotRepository.findById(slotId)
                .filter(slot -> slot.getStartTime() != null && slot.getEndTime() != null)
                .filter(slot -> slot.getStartTime().isBefore(slot.getEndTime()))
                .map(slot -> new BookingRange(slot.getStartTime(), slot.getEndTime()))
                .orElse(null);
    }

    private boolean rangesOverlap(
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            LocalDateTime existingStart,
            LocalDateTime existingEnd
    ) {
        return requestedStart.isBefore(existingEnd) && requestedEnd.isAfter(existingStart);
    }

    private record BookingRange(LocalDateTime start, LocalDateTime end) {
    }
}
