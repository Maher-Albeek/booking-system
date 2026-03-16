package com.maher.booking_system.service;

import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Booking;
import com.maher.booking_system.model.Resources;
import com.maher.booking_system.repository.ResourcesRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingDocumentService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ResourcesRepository resourcesRepository;

    public BookingDocumentService(ResourcesRepository resourcesRepository) {
        this.resourcesRepository = resourcesRepository;
    }

    public DocumentPayload buildContractPdf(Booking booking) {
        Resources resource = loadResource(booking.getResourceId());
        List<String> lines = new ArrayList<>();
        lines.add("Rental Contract");
        lines.add("Booking ID: " + booking.getId());
        lines.add("Customer: " + safe(booking.getCustomerName()));
        lines.add("Vehicle: " + safe(resource.getName()));
        lines.add("Pickup: " + formatDateTime(booking.getStartDateTime()));
        lines.add("Return: " + formatDateTime(booking.getEndDateTime()));
        lines.add("Trip label: " + safe(booking.getServiceName()));
        lines.add("Address: " + safe(booking.getAddress()));
        lines.add("Payment method: " + safe(booking.getPaymentMethod()));
        lines.add("Status: " + (booking.getStatus() == null ? "UNKNOWN" : booking.getStatus().canonical().name()));
        lines.add("Late fee / hour: " + formatAmount(resource.getLateFeePerHour(), resource.getPriceUnit()));
        lines.add("Extra km fee: " + formatAmount(resource.getExtraKmFeePerKm(), resource.getPriceUnit()));
        return new DocumentPayload(
                contractFilename(booking.getId()),
                renderPdf("Rental Contract #" + booking.getId(), lines)
        );
    }

    public DocumentPayload buildReceiptPdf(Booking booking) {
        Resources resource = loadResource(booking.getResourceId());
        List<String> lines = new ArrayList<>();
        lines.add("Booking Receipt");
        lines.add("Booking ID: " + booking.getId());
        lines.add("Customer: " + safe(booking.getCustomerName()));
        lines.add("Vehicle: " + safe(resource.getName()));
        lines.add("Booked at: " + formatDateTime(booking.getBookingTime()));
        lines.add("Rental period: " + formatDateTime(booking.getStartDateTime()) + " -> " + formatDateTime(booking.getEndDateTime()));
        lines.add("Amount paid: " + formatMoneyFromCents(booking.getPayableAmountCents(), booking.getPayableCurrency(), resource.getPriceUnit()));
        lines.add("Payment provider: " + safe(booking.getPaymentProvider()));
        lines.add("Payment status: " + (booking.getPaymentStatus() == null ? "UNKNOWN" : booking.getPaymentStatus().name()));
        lines.add("Invoice number: " + safe(booking.getFinalInvoiceNumber()));
        return new DocumentPayload(
                receiptFilename(booking.getId()),
                renderPdf("Booking Receipt #" + booking.getId(), lines)
        );
    }

    public String contractFilename(Long bookingId) {
        return "booking-" + bookingId + "-contract.pdf";
    }

    public String receiptFilename(Long bookingId) {
        return "booking-" + bookingId + "-receipt.pdf";
    }

    private Resources loadResource(Long resourceId) {
        return resourcesRepository.findById(resourceId)
                .orElseThrow(() -> new NotFoundException("Resource not found with id: " + resourceId));
    }

    private byte[] renderPdf(String title, List<String> lines) {
        List<String> sanitizedLines = lines.stream()
                .map(this::escapePdfText)
                .toList();

        StringBuilder stream = new StringBuilder();
        stream.append("BT\n/F1 18 Tf\n50 780 Td\n(").append(escapePdfText(title)).append(") Tj\n");
        stream.append("0 -28 Td\n/F1 11 Tf\n");
        for (String line : sanitizedLines) {
            stream.append("(").append(line).append(") Tj\n0 -16 Td\n");
        }
        stream.append("ET");

        String streamContent = stream.toString();
        List<String> objects = List.of(
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n",
                "2 0 obj << /Type /Pages /Count 1 /Kids [3 0 R] >> endobj\n",
                "3 0 obj << /Type /Page /Parent 2 0 R /MediaBox [0 0 612 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >> endobj\n",
                "4 0 obj << /Length " + streamContent.getBytes(StandardCharsets.US_ASCII).length + " >> stream\n"
                        + streamContent + "\nendstream\nendobj\n",
                "5 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n"
        );

        StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (String object : objects) {
            offsets.add(pdf.length());
            pdf.append(object);
        }

        int xrefStart = pdf.length();
        pdf.append("xref\n0 ").append(objects.size() + 1).append('\n');
        pdf.append("0000000000 65535 f \n");
        for (Integer offset : offsets) {
            pdf.append(String.format(java.util.Locale.ROOT, "%010d 00000 n \n", offset));
        }
        pdf.append("trailer << /Root 1 0 R /Size ").append(objects.size() + 1).append(" >>\n");
        pdf.append("startxref\n").append(xrefStart).append("\n%%EOF");
        return pdf.toString().getBytes(StandardCharsets.US_ASCII);
    }

    private String escapePdfText(String value) {
        return safe(value)
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "n/a" : DATE_TIME_FORMAT.format(value);
    }

    private String formatAmount(Double value, String priceUnit) {
        if (value == null) {
            return "n/a";
        }
        String unit = priceUnit == null || priceUnit.isBlank() ? "EUR" : priceUnit.trim();
        return String.format(java.util.Locale.ROOT, "%.2f %s", value, unit);
    }

    private String formatMoneyFromCents(Long value, String currency, String fallbackUnit) {
        if (value == null) {
            return "n/a";
        }
        String unit = currency;
        if (unit == null || unit.isBlank()) {
            unit = fallbackUnit == null || fallbackUnit.isBlank() ? "EUR" : fallbackUnit.trim();
        }
        return String.format(java.util.Locale.ROOT, "%.2f %s", value / 100.0d, unit.trim().toUpperCase(java.util.Locale.ROOT));
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "n/a" : value.trim();
    }

    public record DocumentPayload(String filename, byte[] bytes) {
    }
}
