package com.maher.booking_system;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.directory=target/test-resource-catalog-storage")
class ResourceCatalogIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpStorage() throws IOException {
        copy("users.json");
        copy("resources.json");
        copy("bookings.json");
        copy("time-slots.json");
    }

    @Test
    void catalogFiltersOutBookedCarsForDateRange() throws Exception {
        mockMvc.perform(get("/api/resources/catalog")
                        .param("pickupDateTime", "2026-03-03T09:30")
                        .param("returnDateTime", "2026-03-03T10:30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == 3)]").isEmpty());
    }

    @Test
    void catalogSupportsUserIdAndFavoritesEndpointReturnsOk() throws Exception {
        mockMvc.perform(get("/api/resources/catalog")
                        .param("pickupDateTime", "2026-03-17T10:00")
                        .param("returnDateTime", "2026-03-17T12:00")
                        .param("userId", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/resources/favorites")
                        .param("userId", "1"))
                .andExpect(status().isOk());
    }

    @Test
    void favoritesCanBeAddedAndRemoved() throws Exception {
        mockMvc.perform(post("/api/resources/14/favorites")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "userId": 2
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteUserIds[0]").value(2));

        mockMvc.perform(delete("/api/resources/14/favorites/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.favoriteUserIds").isEmpty());
    }

    private void copy(String fileName) throws IOException {
        Path source = Path.of("data", fileName);
        Path targetDir = Path.of("target", "test-resource-catalog-storage");
        Path targetFile = targetDir.resolve(fileName);

        Files.createDirectories(targetDir);
        Files.copy(source, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }
}
