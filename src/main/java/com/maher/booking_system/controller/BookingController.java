package com.maher.booking_system.controller;

import com.maher.booking_system.mapper.BookingMapper;
import com.maher.booking_system.dto.BookingNotificationResponse;
import com.maher.booking_system.service.BookingDocumentService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.maher.booking_system.dto.BookingResponse;
import com.maher.booking_system.dto.CreateBookingRequest;
import com.maher.booking_system.dto.UpdateBookingRequest;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.service.BookingNotificationService;
import com.maher.booking_system.service.BookingService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;
    private final BookingNotificationService bookingNotificationService;
    private final BookingDocumentService bookingDocumentService;

    public BookingController(
            BookingService bookingService,
            BookingNotificationService bookingNotificationService,
            BookingDocumentService bookingDocumentService
    ) {
        this.bookingService = bookingService;
        this.bookingNotificationService = bookingNotificationService;
        this.bookingDocumentService = bookingDocumentService;
    }

    @GetMapping
    public List<BookingResponse> getAllBookings() {
        return bookingService.getAllBookings()
                .stream()
                .map(BookingMapper::toResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public BookingResponse getBookingById(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        Booking booking = bookingService.getBookingById(id);
        return BookingMapper.toResponse(booking);
    }

    @GetMapping("/{id}/notifications")
    public List<BookingNotificationResponse> getNotifications(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        Booking booking = bookingService.getBookingById(id);
        return bookingNotificationService.listNotifications(booking);
    }

    @GetMapping("/{id}/documents/contract")
    public ResponseEntity<byte[]> downloadContract(@PathVariable @NonNull Long id) {
        return documentResponse(bookingDocumentService.buildContractPdf(bookingService.getBookingById(id)));
    }

    @GetMapping("/{id}/documents/receipt")
    public ResponseEntity<byte[]> downloadReceipt(@PathVariable @NonNull Long id) {
        return documentResponse(bookingDocumentService.buildReceiptPdf(bookingService.getBookingById(id)));
    }

    @PostMapping
    public BookingResponse create(@Valid @NonNull @RequestBody CreateBookingRequest request) {
        Booking booking = bookingService.createBooking(request);
        return BookingMapper.toResponse(booking);
    }

    @PatchMapping("/{id}/cancel")
    public BookingResponse cancel(@PathVariable @NonNull Long id) {
        return BookingMapper.toResponse(bookingService.cancelBooking(id));
    }

    @PatchMapping("/{id}")
    public BookingResponse update(
            @PathVariable @NonNull Long id,
            @Valid @RequestBody UpdateBookingRequest request
    ) {
        Booking booking = bookingService.updateBooking(id, request);
        return BookingMapper.toResponse(booking);
    }

    private ResponseEntity<byte[]> documentResponse(BookingDocumentService.DocumentPayload document) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(document.filename()).build().toString())
                .body(document.bytes());
    }
}
