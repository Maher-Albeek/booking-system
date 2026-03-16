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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.directory=target/test-auth-epic-storage")
class AuthEpicIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpStorage() throws IOException {
        Path source = Path.of("data", "users.json");
        Path targetDir = Path.of("target", "test-auth-epic-storage");
        Path targetFile = targetDir.resolve("users.json");

        Files.createDirectories(targetDir);
        Files.copy(source, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void registerRejectsWeakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Weak Password User",
                                  "email": "weak@example.com",
                                  "password": "abcdefg1"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfileRejectsDuplicateEmail() throws Exception {
        mockMvc.perform(put("/api/users/2")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "ad",
                                  "email": "malbeek92@gmail.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void updateProfileAcceptsUniqueEmail() throws Exception {
        mockMvc.perform(put("/api/users/2")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated User",
                                  "email": "updated@example.com",
                                  "firstName": "Updated",
                                  "lastName": "User"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated User"))
                .andExpect(jsonPath("$.email").value("updated@example.com"));
    }
}
