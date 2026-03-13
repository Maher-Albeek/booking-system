package com.maher.booking_system.service;

import com.maher.booking_system.dto.CheckInRequest;
import com.maher.booking_system.dto.CheckOutRequest;
import com.maher.booking_system.dto.ExtendBookingRequest;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.*;
import com.maher.booking_system.model.enums.BookingStatus;
import com.maher.booking_system.model.enums.PaymentStatus;
import com.maher.booking_system.repository.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
public class RentalOperationsService {
    private final BookingRepository bookingRepository;
    private final ResourcesRepository resourcesRepository;
    private final PaymentRepository paymentRepository;
    private final CancellationPolicyRepository cancellationPolicyRepository;
    private final CheckInOutRepository checkInOutRepository;
    private final DepositRepository depositRepository;
    private final FeeRecordRepository feeRecordRepository;
    private final DamageReportRepository damageReportRepository;
    private final BookingService bookingService;

    public RentalOperationsService(
            BookingRepository bookingRepository,
            ResourcesRepository resourcesRepository,
            PaymentRepository paymentRepository,
            CancellationPolicyRepository cancellationPolicyRepository,
            CheckInOutRepository checkInOutRepository,
            DepositRepository depositRepository,
            FeeRecordRepository feeRecordRepository,
            DamageReportRepository damageReportRepository,
            BookingService bookingService
    ) {
        this.bookingRepository = bookingRepository;
        this.resourcesRepository = resourcesRepository;
        this.paymentRepository = paymentRepository;
        this.cancellationPolicyRepository = cancellationPolicyRepository;
        this.checkInOutRepository = checkInOutRepository;
        this.depositRepository = depositRepository;
        this.feeRecordRepository = feeRecordRepository;
        this.damageReportRepository = damageReportRepository;
        this.bookingService = bookingService;
    }

    public Booking extendBooking(Long bookingId, ExtendBookingRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!request.isPaymentConfirmed()) {
            throw new BadRequestException("Extension requires successful payment confirmation");
        }
        LocalDateTime newEnd = parseDateTime(request.getNewEndDateTime(), "newEndDateTime");
        if (booking.getEndDateTime() == null || !newEnd.isAfter(booking.getEndDateTime())) {
            throw new BadRequestException("newEndDateTime must be after current endDateTime");
        }
        boolean available = bookingService.isCarAvailableForRange(booking.getResourceId(), booking.getEndDateTime(), newEnd);
        if (!available) {
            throw new BadRequestException("Car is not available for requested extension");
        }
        booking.setEndDateTime(newEnd);
        long currentAmount = booking.getPayableAmountCents() == null ? 0L : booking.getPayableAmountCents();
        booking.setPayableAmountCents(currentAmount + calculateExtraAmount(booking));
        return bookingRepository.save(booking);
    }

    public RefundResponse cancelWithRefund(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStartDateTime() == null || booking.getPayableAmountCents() == null) {
            throw new BadRequestException("Booking cannot be refunded");
        }
        long hoursBeforePickup = Duration.between(LocalDateTime.now(), booking.getStartDateTime()).toHours();
        int refundPercent = resolveRefundPercent(hoursBeforePickup);
        long refundAmount = Math.round(booking.getPayableAmountCents() * (refundPercent / 100.0d));

        booking.setStatus(BookingStatus.CANCELLED);
        booking.setPaymentStatus(refundAmount > 0 ? PaymentStatus.REFUNDED : booking.getPaymentStatus());
        bookingRepository.save(booking);

        paymentRepository.findByBookingId(bookingId).ifPresent(payment -> {
            payment.setStatus(refundAmount > 0 ? PaymentStatus.REFUNDED : payment.getStatus());
            payment.setRefundedAmountCents(refundAmount);
            payment.setUpdatedAt(LocalDateTime.now());
            paymentRepository.save(payment);
        });
        return new RefundResponse(bookingId, refundPercent, refundAmount);
    }

    public List<CancellationPolicyRule> getCancellationPolicy() {
        return cancellationPolicyRepository.findAll();
    }

    public List<CancellationPolicyRule> saveCancellationPolicy(List<CancellationPolicyRule> rules) {
        List<CancellationPolicyRule> normalized = (rules == null ? List.<CancellationPolicyRule>of() : rules).stream()
                .peek(rule -> {
                    if (rule.getRefundPercent() == null || rule.getRefundPercent() < 0 || rule.getRefundPercent() > 100) {
                        throw new BadRequestException("refundPercent must be 0..100");
                    }
                })
                .toList();
        cancellationPolicyRepository.findAll().forEach(existing -> cancellationPolicyRepository.deleteById(existing.getId()));
        normalized.forEach(cancellationPolicyRepository::save);
        return cancellationPolicyRepository.findAll();
    }

    public CheckInOutRecord checkIn(Long bookingId, CheckInRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BadRequestException("Booking must be PENDING for check-in");
        }
        CheckInOutRecord record = checkInOutRepository.findByBookingId(bookingId).orElseGet(CheckInOutRecord::new);
        record.setBookingId(bookingId);
        record.setEmployeeUserId(request.getEmployeeUserId());
        record.setOdometerStart(request.getOdometerStart());
        record.setFuelLevelStart(request.getFuelLevel());
        record.setInternalNotes(request.getInternalNotes());
        record.setCheckInPhotoUrls(request.getPhotoUrls());
        record.setCheckInTime(LocalDateTime.now());
        checkInOutRepository.save(record);

        booking.setStatus(BookingStatus.ACTIVE);
        bookingRepository.save(booking);

        Resources resource = resourcesRepository.findById(booking.getResourceId()).orElse(null);
        if (resource != null && resource.getDepositAmountCents() != null && resource.getDepositAmountCents() > 0) {
            DepositRecord deposit = depositRepository.findByBookingId(bookingId).orElseGet(DepositRecord::new);
            deposit.setBookingId(bookingId);
            deposit.setAmountCents(resource.getDepositAmountCents());
            deposit.setStatus("HELD");
            deposit.setHeldAt(LocalDateTime.now());
            depositRepository.save(deposit);
        }
        return record;
    }

    public CheckOutSummary checkOut(Long bookingId, CheckOutRequest request) {
        Booking booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getStatus() != BookingStatus.ACTIVE) {
            throw new BadRequestException("Booking must be ACTIVE for check-out");
        }
        CheckInOutRecord record = checkInOutRepository.findByBookingId(bookingId)
                .orElseThrow(() -> new BadRequestException("No check-in record found"));
        record.setOdometerEnd(request.getOdometerEnd());
        record.setFuelLevelEnd(request.getFuelLevel());
        record.setInternalNotes(request.getInternalNotes());
        record.setCheckOutPhotoUrls(request.getPhotoUrls());
        record.setCheckOutTime(LocalDateTime.now());
        checkInOutRepository.save(record);

        Resources resource = resourcesRepository.findById(booking.getResourceId()).orElse(null);
        long extraKmFee = calculateExtraKmFee(record, booking, resource);
        long lateFee = calculateLateFee(booking, resource);
        long damageFees = createDamageFees(bookingId, request.getDamageFees());
        if (extraKmFee > 0) {
            createFee(bookingId, "EXTRA_KM", extraKmFee, "Exceeded km/day limit");
        }
        if (lateFee > 0) {
            createFee(bookingId, "LATE_RETURN", lateFee, "Late return fee");
        }

        booking.setStatus(BookingStatus.COMPLETED);
        bookingRepository.save(booking);

        depositRepository.findByBookingId(bookingId).ifPresent(deposit -> {
            deposit.setStatus("RELEASED");
            deposit.setReleasedAt(LocalDateTime.now());
            depositRepository.save(deposit);
        });

        long totalFees = extraKmFee + lateFee + damageFees;
        return new CheckOutSummary(bookingId, extraKmFee, lateFee, damageFees, totalFees);
    }

    private long calculateExtraAmount(Booking booking) {
        Resources resource = resourcesRepository.findById(booking.getResourceId()).orElse(null);
        if (resource == null || resource.getDailyPrice() == null) {
            return 0L;
        }
        return Math.round(resource.getDailyPrice() * 100.0d);
    }

    private int resolveRefundPercent(long hoursBeforePickup) {
        return cancellationPolicyRepository.findAll().stream()
                .filter(rule -> rule.getMinHoursBeforePickup() == null || hoursBeforePickup >= rule.getMinHoursBeforePickup())
                .filter(rule -> rule.getMaxHoursBeforePickup() == null || hoursBeforePickup < rule.getMaxHoursBeforePickup())
                .findFirst()
                .map(CancellationPolicyRule::getRefundPercent)
                .orElse(0);
    }

    private long calculateExtraKmFee(CheckInOutRecord record, Booking booking, Resources resource) {
        if (resource == null || resource.getKmPerDayLimit() == null || resource.getExtraKmFeePerKmCents() == null) {
            return 0L;
        }
        if (record.getOdometerStart() == null || record.getOdometerEnd() == null || booking.getStartDateTime() == null || booking.getEndDateTime() == null) {
            return 0L;
        }
        long usedKm = Math.max(0, record.getOdometerEnd() - record.getOdometerStart());
        long days = Math.max(1L, Duration.between(booking.getStartDateTime(), booking.getEndDateTime()).toHours() / 24L);
        long allowedKm = resource.getKmPerDayLimit() * days;
        long exceeded = Math.max(0, usedKm - allowedKm);
        return exceeded * resource.getExtraKmFeePerKmCents();
    }

    private long calculateLateFee(Booking booking, Resources resource) {
        if (resource == null || resource.getLateFeePerHourCents() == null || booking.getEndDateTime() == null) {
            return 0L;
        }
        long lateHours = Math.max(0L, Duration.between(booking.getEndDateTime(), LocalDateTime.now()).toHours());
        return lateHours * resource.getLateFeePerHourCents();
    }

    private long createDamageFees(Long bookingId, List<CheckOutRequest.DamageFeeRequest> damageFees) {
        long total = 0L;
        for (CheckOutRequest.DamageFeeRequest request : damageFees) {
            long amount = request.getAmountCents() == null ? 0L : Math.max(0L, request.getAmountCents());
            if (amount == 0) {
                continue;
            }
            DamageReport report = new DamageReport();
            report.setBookingId(bookingId);
            report.setFeeType(request.getFeeType());
            report.setNotes(request.getNotes());
            report.setAmountCents(amount);
            report.setCreatedAt(LocalDateTime.now());
            damageReportRepository.save(report);
            createFee(bookingId, "DAMAGE", amount, request.getNotes());
            total += amount;
        }
        return total;
    }

    private void createFee(Long bookingId, String feeType, long amountCents, String reason) {
        FeeRecord feeRecord = new FeeRecord();
        feeRecord.setBookingId(bookingId);
        feeRecord.setFeeType(feeType);
        feeRecord.setAmountCents(amountCents);
        feeRecord.setReason(reason);
        feeRecord.setCreatedAt(LocalDateTime.now());
        feeRecordRepository.save(feeRecord);
    }

    private LocalDateTime parseDateTime(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(fieldName + " is required");
        }
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BadRequestException(fieldName + " must use yyyy-MM-ddTHH:mm format");
        }
    }

    public record RefundResponse(Long bookingId, int refundPercent, long refundAmountCents) {
    }

    public record CheckOutSummary(
            Long bookingId,
            long extraKmFeeCents,
            long lateFeeCents,
            long damageFeeCents,
            long totalFeeCents
    ) {
    }
}
