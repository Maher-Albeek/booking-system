package com.maher.booking_system.service;

import com.maher.booking_system.dto.CreateBookingRequest;
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

    private static final BookingStatus CONFIRMED_STATUS = BookingStatus.CONFIRMED;

    private final BookingRepository bookingRepository;
    private final TimeSlotRepository timeSlotRepository;

    public BookingService(BookingRepository bookingRepository, TimeSlotRepository timeSlotRepository) {
        this.bookingRepository = bookingRepository;
        this.timeSlotRepository = timeSlotRepository;
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public @NonNull Booking getBookingById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
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

            boolean alreadyBooked = bookingRepository.existsByTimeSlotIdAndStatus(safeRequest.getTimeSlotId(), CONFIRMED_STATUS);
            if (alreadyBooked) {
                throw new ConflictException("Time slot already booked");
            }
        }

        boolean carAlreadyBookedInPeriod = bookingRepository.findByResourceIdAndStatus(
                        safeRequest.getResourceId(),
                        CONFIRMED_STATUS
                )
                .stream()
                .map(this::resolveBookingRange)
                .filter(Objects::nonNull)
                .anyMatch(range -> rangesOverlap(requestedStart, requestedEnd, range.start(), range.end()));

        if (carAlreadyBookedInPeriod) {
            throw new ConflictException("Car is already booked for the selected period");
        }

        Booking booking = new Booking();
        booking.setUserId(safeRequest.getUserId());
        booking.setResourceId(safeRequest.getResourceId());
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
        booking.setStatus(CONFIRMED_STATUS);
        booking.setBookingTime(LocalDateTime.now());

        if (selectedSlot != null) {
            selectedSlot.setAvailable(false);
            timeSlotRepository.save(selectedSlot);
        }

        return bookingRepository.save(booking);
    }

    public List<TimeSlot> getTimeSlotsByResource(@NonNull Long resourceId, Boolean available) {
        Objects.requireNonNull(resourceId, "resourceId must not be null");
        if (available == null) {
            return timeSlotRepository.findByResourceId(resourceId);
        }
        return timeSlotRepository.findByResourceIdAndAvailable(resourceId, available);
    }

    public synchronized void cancelBooking(@NonNull Long bookingId) {
        Objects.requireNonNull(bookingId, "bookingId must not be null");

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Booking already cancelled");
        }

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);

        Long slotId = booking.getTimeSlotId();
        if (slotId != null) {
            timeSlotRepository.findByIdForUpdate(slotId).ifPresent(slot -> {
                slot.setAvailable(true);
                timeSlotRepository.save(slot);
            });
        }
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
