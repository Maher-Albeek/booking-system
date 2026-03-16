package com.maher.booking_system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.directory=target/test-booking-notification-storage")
class BookingNotificationsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUpStorage() throws IOException {
        copy("users.json");
        copy("resources.json");
        copy("bookings.json");
        copy("time-slots.json");
    }

    @Test
    void confirmedBookingProvidesEmailNotificationsAndPdfDownloads() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = start.plusHours(5);

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "resourceId": 14,
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "serviceName": "EP07 flow",
                                  "firstName": "Maher",
                                  "lastName": "Test",
                                  "address": "Main 1, 12345 Berlin, DE",
                                  "birthDate": "1990-01-04",
                                  "paymentMethod": "PayPal"
                                }
                                """.formatted(start, end)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.confirmationEmailRecipient").value("malbeek92@gmail.com"))
                .andExpect(jsonPath("$.confirmationEmailSentAt").isNotEmpty())
                .andReturn();

        JsonNode createdBooking = objectMapper.readTree(result.getResponse().getContentAsString());
        long bookingId = createdBooking.path("id").asLong();

        mockMvc.perform(get("/api/bookings/{id}/notifications", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("BOOKING_CONFIRMATION"))
                .andExpect(jsonPath("$[0].attachments.length()").value(2))
                .andExpect(jsonPath("$[0].attachments[0].downloadUrl").value("/api/bookings/" + bookingId + "/documents/contract"))
                .andExpect(jsonPath("$[0].attachments[1].downloadUrl").value("/api/bookings/" + bookingId + "/documents/receipt"));

        mockMvc.perform(get("/api/bookings/{id}/documents/contract", bookingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("booking-" + bookingId + "-contract.pdf")));

        mockMvc.perform(get("/api/bookings/{id}/documents/receipt", bookingId))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Content-Disposition", containsString("booking-" + bookingId + "-receipt.pdf")));
    }

    @Test
    void dueReturnReminderAppearsWhenBookingIsWithinSixHoursOfReturn() throws Exception {
        LocalDateTime start = LocalDateTime.now().minusHours(20).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime end = LocalDateTime.now().plusHours(5).truncatedTo(ChronoUnit.MINUTES);

        MvcResult result = mockMvc.perform(post("/api/bookings")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 1,
                                  "resourceId": 11,
                                  "startDateTime": "%s",
                                  "endDateTime": "%s",
                                  "serviceName": "Reminder flow",
                                  "firstName": "Maher",
                                  "lastName": "Reminder",
                                  "address": "Main 1, 12345 Berlin, DE",
                                  "birthDate": "1990-01-04",
                                  "paymentMethod": "Visa"
                                }
                                """.formatted(start, end)))
                .andExpect(status().isOk())
                .andReturn();

        long bookingId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();

        mockMvc.perform(get("/api/bookings/{id}/notifications", bookingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.type == 'RETURN_REMINDER')]").isNotEmpty());
    }

    private void copy(String fileName) throws IOException {
        Path source = Path.of("data", fileName);
        Path targetDir = Path.of("target", "test-booking-notification-storage");
        Path targetFile = targetDir.resolve(fileName);

        Files.createDirectories(targetDir);
        Files.copy(source, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
