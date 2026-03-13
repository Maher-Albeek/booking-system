package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Repository
public class BookingRepository extends JsonRepositorySupport<Booking> {
    private static final Set<BookingStatus> BLOCKING_STATUSES = EnumSet.of(
            BookingStatus.PENDING,
            BookingStatus.ACTIVE,
            BookingStatus.COMPLETED
    );

    public BookingRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, Path.of(storageDirectory), "bookings.json", Booking.class, Booking::getId, Booking::setId);
    }

    public boolean existsByTimeSlotIdAndStatus(Long timeSlotId, BookingStatus status) {
        return findAll().stream()
                .anyMatch(booking -> timeSlotId.equals(booking.getTimeSlotId()) && status == booking.getStatus());
    }

    public List<Booking> findByResourceIdAndStatus(Long resourceId, BookingStatus status) {
        return findAll().stream()
                .filter(booking -> resourceId.equals(booking.getResourceId()) && status == booking.getStatus())
                .toList();
    }

    public boolean existsOverlapping(
            Long resourceId,
            LocalDateTime requestedStart,
            LocalDateTime requestedEnd,
            Set<BookingStatus> statuses
    ) {
        Set<BookingStatus> safeStatuses = statuses == null || statuses.isEmpty()
                ? BLOCKING_STATUSES
                : statuses;

        return findAll().stream()
                .filter(booking -> Objects.equals(resourceId, booking.getResourceId()))
                .filter(booking -> safeStatuses.contains(booking.getStatus()))
                .filter(booking -> booking.getStartDateTime() != null && booking.getEndDateTime() != null)
                .anyMatch(booking ->
                        requestedStart.isBefore(booking.getEndDateTime())
                                && requestedEnd.isAfter(booking.getStartDateTime()));
    }

    public Booking saveIfNoOverlap(Booking booking) {
        return withMonitor(() -> {
            List<Booking> entities = readAllUnsafe();
            boolean hasOverlap = entities.stream()
                    .filter(existing -> Objects.equals(existing.getResourceId(), booking.getResourceId()))
                    .filter(existing -> BLOCKING_STATUSES.contains(existing.getStatus()))
                    .filter(existing -> existing.getStartDateTime() != null && existing.getEndDateTime() != null)
                    .anyMatch(existing ->
                            booking.getStartDateTime().isBefore(existing.getEndDateTime())
                                    && booking.getEndDateTime().isAfter(existing.getStartDateTime()));
            if (hasOverlap) {
                return null;
            }

            Long nextId = entities.stream()
                    .map(Booking::getId)
                    .filter(Objects::nonNull)
                    .max(Long::compareTo)
                    .orElse(0L) + 1;
            booking.setId(nextId);
            if (booking.getPaymentStatus() == null) {
                booking.setPaymentStatus(PaymentStatus.SUCCEEDED);
            }
            entities.add(booking);
            writeAllUnsafe(entities);
            return booking;
        });
    }

    public List<Booking> findByFilters(
            LocalDateTime from,
            LocalDateTime to,
            BookingStatus status,
            Long resourceId,
            Long userId,
            PaymentStatus paymentStatus
    ) {
        return findAll().stream()
                .filter(booking -> status == null || booking.getStatus() == status)
                .filter(booking -> resourceId == null || resourceId.equals(booking.getResourceId()))
                .filter(booking -> userId == null || userId.equals(booking.getUserId()))
                .filter(booking -> paymentStatus == null || paymentStatus.equals(booking.getPaymentStatus()))
                .filter(booking -> from == null || booking.getStartDateTime() == null || !booking.getEndDateTime().isBefore(from))
                .filter(booking -> to == null || booking.getEndDateTime() == null || !booking.getStartDateTime().isAfter(to))
                .toList();
    }
}
