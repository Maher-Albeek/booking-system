package com.maher.booking_system.service;

import com.maher.booking_system.dto.AdminBookingFilterRequest;
import com.maher.booking_system.dto.BookingCheckInRequest;
import com.maher.booking_system.dto.BookingCheckOutRequest;
import com.maher.booking_system.dto.BookingOperationsResponse;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.DamageReportItem;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Service
public class AdminOperationsService {
    private final BookingRepository bookingRepository;
    private final ResourcesRepository resourcesRepository;
    private final UsersRepository usersRepository;
    private final PaymentService paymentService;

    public AdminOperationsService(
            BookingRepository bookingRepository,
            ResourcesRepository resourcesRepository,
            UsersRepository usersRepository,
            PaymentService paymentService
    ) {
        this.bookingRepository = bookingRepository;
        this.resourcesRepository = resourcesRepository;
        this.usersRepository = usersRepository;
        this.paymentService = paymentService;
    }

    public List<BookingOperationsResponse> findBookings(AdminBookingFilterRequest filters) {
        return bookingRepository.findAll().stream()
                .filter(booking -> matchesFilters(booking, filters))
                .sorted(Comparator.comparing(Booking::getBookingTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    public BookingOperationsResponse getBooking(Long bookingId) {
        return toResponse(getRequiredBooking(bookingId));
    }

    public String exportBookingsCsv(AdminBookingFilterRequest filters) {
        List<BookingOperationsResponse> rows = findBookings(filters);
        StringBuilder csv = new StringBuilder();
        csv.append("Booking ID,User,Email,Car,Start,End,Total,Fees,Status,Payment Status,Invoice\n");
        for (BookingOperationsResponse row : rows) {
            csv.append(csvValue(row.id()))
                    .append(',').append(csvValue(row.userName()))
                    .append(',').append(csvValue(row.userEmail()))
                    .append(',').append(csvValue(row.resourceName()))
                    .append(',').append(csvValue(row.startDateTime()))
                    .append(',').append(csvValue(row.endDateTime()))
                    .append(',').append(csvValue(formatMoney(row.finalTotalAmountCents() != null ? row.finalTotalAmountCents() : row.payableAmountCents(), row.payableCurrency())))
                    .append(',').append(csvValue(formatFees(row)))
                    .append(',').append(csvValue(row.status()))
                    .append(',').append(csvValue(row.paymentStatus()))
                    .append(',').append(csvValue(row.finalInvoiceNumber()))
                    .append('\n');
        }
        return csv.toString();
    }

    public BookingOperationsResponse checkIn(Long bookingId, BookingCheckInRequest request) {
        Booking booking = getRequiredBooking(bookingId);
        if (booking.getStatus() == null || booking.getStatus().canonical() != BookingStatus.PENDING) {
            throw new BadRequestException("Only pending bookings can be checked in");
        }
        if (request == null || request.pickupOdometerKm() == null || request.pickupOdometerKm() < 0) {
            throw new BadRequestException("pickupOdometerKm must be a non-negative number");
        }

        BookingStatus previousStatus = booking.getStatus().canonical();
        booking.setPickupOdometerKm(request.pickupOdometerKm());
        booking.setPickupFuelLevel(request.pickupFuelLevel());
        booking.setPickupNotes(request.pickupNotes());
        booking.setPickupPhotoUrls(request.pickupPhotoUrls());
        booking.setCheckedInAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.ACTIVE);
        paymentService.handleStatusTransition(booking, previousStatus);
        return toResponse(bookingRepository.save(booking));
    }

    public BookingOperationsResponse checkOut(Long bookingId, BookingCheckOutRequest request) {
        Booking booking = getRequiredBooking(bookingId);
        if (booking.getStatus() == null || booking.getStatus().canonical() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Only active bookings can be checked out");
        }
        if (booking.getPickupOdometerKm() == null) {
            throw new BadRequestException("Booking must be checked in before check-out");
        }
        if (request == null || request.returnOdometerKm() == null || request.returnOdometerKm() < booking.getPickupOdometerKm()) {
            throw new BadRequestException("returnOdometerKm must be greater than or equal to the pickup odometer");
        }

        Resources resource = resourcesRepository.findById(booking.getResourceId())
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + booking.getResourceId()));

        LocalDateTime actualReturn = parseOptionalDateTime(request.actualReturnDateTime()).orElse(LocalDateTime.now());
        List<DamageReportItem> damageReports = normalizeDamageReports(request.damageReports());

        long extraKmFeeCents = calculateExtraKmFeeCents(booking, resource, request.returnOdometerKm());
        long lateFeeCents = calculateLateFeeCents(booking, resource, actualReturn);
        long damageFeeCents = damageReports.stream()
                .map(DamageReportItem::getFeeCents)
                .filter(Objects::nonNull)
                .mapToLong(Long::longValue)
                .sum();
        long finalTotal = (booking.getPayableAmountCents() == null ? 0L : booking.getPayableAmountCents())
                + extraKmFeeCents
                + lateFeeCents
                + damageFeeCents;

        BookingStatus previousStatus = booking.getStatus().canonical();
        booking.setReturnOdometerKm(request.returnOdometerKm());
        booking.setActualReturnDateTime(actualReturn);
        booking.setReturnNotes(request.returnNotes());
        booking.setReturnPhotoUrls(request.returnPhotoUrls());
        booking.setDamageReports(damageReports);
        booking.setExtraKmFeeCents(extraKmFeeCents);
        booking.setLateFeeCents(lateFeeCents);
        booking.setDamageFeeCents(damageFeeCents);
        booking.setFinalTotalAmountCents(finalTotal);
        booking.setFinalInvoiceNumber("INV-" + booking.getId() + "-" + LocalDate.now());
        booking.setFinalInvoiceIssuedAt(LocalDateTime.now());
        booking.setStatus(BookingStatus.COMPLETED);
        paymentService.handleStatusTransition(booking, previousStatus);
        return toResponse(bookingRepository.save(booking));
    }

    private Booking getRequiredBooking(Long bookingId) {
        if (bookingId == null) {
            throw new BadRequestException("bookingId is required");
        }
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found with id: " + bookingId));
    }

    private boolean matchesFilters(Booking booking, AdminBookingFilterRequest filters) {
        if (filters == null) {
            return true;
        }

        LocalDateTime start = parseOptionalDateTime(filters.startDateTime()).orElse(null);
        LocalDateTime end = parseOptionalDateTime(filters.endDateTime()).orElse(null);
        if (start != null && (booking.getStartDateTime() == null || booking.getStartDateTime().isBefore(start))) {
            return false;
        }
        if (end != null && (booking.getEndDateTime() == null || booking.getEndDateTime().isAfter(end))) {
            return false;
        }
        if (filters.carId() != null && !Objects.equals(filters.carId(), booking.getResourceId())) {
            return false;
        }
        if (filters.userId() != null && !Objects.equals(filters.userId(), booking.getUserId())) {
            return false;
        }
        if (filters.status() != null && !filters.status().isBlank()) {
            String status = booking.getStatus() == null ? "" : booking.getStatus().canonical().name();
            if (!status.equalsIgnoreCase(filters.status().trim())) {
                return false;
            }
        }
        if (filters.paymentStatus() != null && !filters.paymentStatus().isBlank()) {
            String paymentStatus = booking.getPaymentStatus() == null ? "" : booking.getPaymentStatus().name();
            if (!paymentStatus.equalsIgnoreCase(filters.paymentStatus().trim())) {
                return false;
            }
        }
        return true;
    }

    private BookingOperationsResponse toResponse(Booking booking) {
        Resources resource = booking.getResourceId() == null ? null : resourcesRepository.findById(booking.getResourceId()).orElse(null);
        Users user = booking.getUserId() == null ? null : usersRepository.findById(booking.getUserId()).orElse(null);

        List<BookingOperationsResponse.DamageReportResponse> damageReports = booking.getDamageReports().stream()
                .map(item -> new BookingOperationsResponse.DamageReportResponse(item.getType(), item.getNotes(), item.getFeeCents()))
                .toList();

        return new BookingOperationsResponse(
                booking.getId(),
                booking.getUserId(),
                user == null ? booking.getCustomerName() : user.getName(),
                user == null ? null : user.getEmail(),
                booking.getResourceId(),
                resource == null ? "Unknown car" : resource.getName(),
                booking.getStartDateTime(),
                booking.getEndDateTime(),
                booking.getStatus() == null ? null : booking.getStatus().canonical().name(),
                booking.getBookingTime(),
                booking.getCustomerName(),
                booking.getServiceName(),
                booking.getPaymentMethod(),
                booking.getPaymentStatus() == null ? null : booking.getPaymentStatus().name(),
                booking.getPayableAmountCents(),
                booking.getPayableCurrency(),
                booking.getPickupOdometerKm(),
                booking.getPickupFuelLevel(),
                booking.getPickupNotes(),
                List.copyOf(booking.getPickupPhotoUrls()),
                booking.getCheckedInAt(),
                booking.getReturnOdometerKm(),
                booking.getActualReturnDateTime(),
                booking.getReturnNotes(),
                List.copyOf(booking.getReturnPhotoUrls()),
                damageReports,
                booking.getExtraKmFeeCents(),
                booking.getLateFeeCents(),
                booking.getDamageFeeCents(),
                booking.getFinalTotalAmountCents(),
                booking.getFinalInvoiceNumber(),
                booking.getFinalInvoiceIssuedAt()
        );
    }

    private Optional<LocalDateTime> parseOptionalDateTime(String value) {
        if (value == null || value.trim().isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(LocalDateTime.parse(value.trim()));
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Date-time values must use yyyy-MM-ddTHH:mm format");
        }
    }

    private List<DamageReportItem> normalizeDamageReports(List<BookingCheckOutRequest.DamageReportRequest> requests) {
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .filter(Objects::nonNull)
                .map(request -> {
                    DamageReportItem item = new DamageReportItem();
                    item.setType(request.type());
                    item.setNotes(request.notes());
                    item.setFeeCents(request.feeCents());
                    return item;
                })
                .filter(item -> item.getType() != null || item.getFeeCents() > 0)
                .toList();
    }

    private long calculateExtraKmFeeCents(Booking booking, Resources resource, Integer returnOdometerKm) {
        if (resource.getKmPerDayLimit() == null || resource.getExtraKmFeePerKm() == null) {
            return 0L;
        }
        long bookedHours = booking.getStartDateTime() == null || booking.getEndDateTime() == null
                ? 24L
                : Math.max(1L, ChronoUnit.HOURS.between(booking.getStartDateTime(), booking.getEndDateTime()));
        long bookedDays = Math.max(1L, (long) Math.ceil(bookedHours / 24.0d));
        long includedKm = bookedDays * resource.getKmPerDayLimit();
        long actualKm = returnOdometerKm - booking.getPickupOdometerKm();
        long extraKm = Math.max(0L, actualKm - includedKm);
        return Math.round(extraKm * resource.getExtraKmFeePerKm() * 100.0d);
    }

    private long calculateLateFeeCents(Booking booking, Resources resource, LocalDateTime actualReturn) {
        if (booking.getEndDateTime() == null || resource.getLateFeePerHour() == null || !actualReturn.isAfter(booking.getEndDateTime())) {
            return 0L;
        }
        long lateMinutes = ChronoUnit.MINUTES.between(booking.getEndDateTime(), actualReturn);
        long lateHours = Math.max(1L, (long) Math.ceil(lateMinutes / 60.0d));
        return Math.round(lateHours * resource.getLateFeePerHour() * 100.0d);
    }

    private String formatMoney(Long amountCents, String currency) {
        if (amountCents == null) {
            return "";
        }
        String safeCurrency = currency == null || currency.isBlank() ? "EUR" : currency.toUpperCase(Locale.ROOT);
        return String.format(Locale.ROOT, "%.2f %s", amountCents / 100.0d, safeCurrency);
    }

    private String formatFees(BookingOperationsResponse row) {
        long extra = row.extraKmFeeCents() == null ? 0L : row.extraKmFeeCents();
        long late = row.lateFeeCents() == null ? 0L : row.lateFeeCents();
        long damage = row.damageFeeCents() == null ? 0L : row.damageFeeCents();
        return formatMoney(extra + late + damage, row.payableCurrency());
    }

    private String csvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
