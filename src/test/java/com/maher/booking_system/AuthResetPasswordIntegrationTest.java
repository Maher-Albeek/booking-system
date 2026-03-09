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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.storage.directory=target/test-reset-storage")
class AuthResetPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUpStorage() throws IOException {
        Path source = Path.of("data", "users.json");
        Path targetDir = Path.of("target", "test-reset-storage");
        Path targetFile = targetDir.resolve("users.json");

        Files.createDirectories(targetDir);
        Files.copy(source, targetFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    @Test
    void resetPasswordReturnsOkForExistingUser() throws Exception {
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "identifier": "maher",
                                  "newPassword": "abcdef"
                                }
                                """))
                .andExpect(status().isOk());
    }
}
