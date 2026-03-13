package com.maher.booking_system.controller;

import com.maher.booking_system.dto.AuthLoginRequest;
import com.maher.booking_system.dto.AuthRegisterRequest;
import com.maher.booking_system.dto.AuthResetPasswordRequest;
import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.security.JwtService;
import com.maher.booking_system.service.UsersService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsersService usersService;
    private final JwtService jwtService;

    public AuthController(UsersService usersService, JwtService jwtService) {
        this.usersService = usersService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public @NonNull ResponseEntity<UserResponse> login(@RequestBody @NonNull AuthLoginRequest request) {
        AuthLoginRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        UserResponse user = usersService.authenticate(safeRequest.identifier(), safeRequest.password());
        return responseWithJwt(user);
    }

    @PostMapping("/register")
    public @NonNull ResponseEntity<UserResponse> register(@Valid @RequestBody @NonNull AuthRegisterRequest request) {
        AuthRegisterRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        UserResponse user = usersService.registerUser(safeRequest.name(), safeRequest.email(), safeRequest.password());
        return responseWithJwt(user);
    }

    @PostMapping("/google")
    public @NonNull ResponseEntity<UserResponse> googleLogin(@RequestBody GoogleLoginRequest request) {
        if (request == null || request.email() == null || request.email().isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        String name = request.name() == null || request.name().isBlank() ? request.email() : request.name();
        UserResponse existing = usersService.getAllUserResponses().stream()
                .filter(user -> user.email() != null && user.email().equalsIgnoreCase(request.email().trim()))
                .findFirst()
                .orElse(null);
        UserResponse user = existing != null
                ? existing
                : usersService.registerUser(name.trim(), request.email().trim(), "google-oauth-placeholder");
        return responseWithJwt(user);
    }

    @PostMapping("/reset-password")
    public void resetPassword(@Valid @RequestBody @NonNull AuthResetPasswordRequest request) {
        AuthResetPasswordRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        usersService.resetPassword(safeRequest.identifier(), safeRequest.newPassword());
    }

    private ResponseEntity<UserResponse> responseWithJwt(UserResponse user) {
        String token = jwtService.createToken(user.id(), user.email(), user.role());
        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .header("X-Auth-Token", token)
                .body(user);
    }

    public record GoogleLoginRequest(String email, String name) {
    }
}
