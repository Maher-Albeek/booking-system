package com.maher.booking_system.controller;

import com.maher.booking_system.dto.AuthLoginRequest;
import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.service.UsersService;
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

    public AuthController(UsersService usersService) {
        this.usersService = usersService;
    }

    @PostMapping("/login")
    public @NonNull UserResponse login(@RequestBody @NonNull AuthLoginRequest request) {
        AuthLoginRequest safeRequest = Objects.requireNonNull(request, "request must not be null");
        return usersService.authenticate(safeRequest.identifier(), safeRequest.password());
    }
}
