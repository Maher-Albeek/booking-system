package com.maher.booking_system.controller;

import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.service.UsersService;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UsersService usersService;

    public UserController(UsersService usersService) {
        this.usersService = usersService;
    }

    @GetMapping
    public List<UserResponse> getAllUsers() {
        return usersService.getAllUserResponses();
    }

    @PostMapping
    public @NonNull UserResponse createUser(@RequestBody @NonNull Users user) {
        Users safeUser = Objects.requireNonNull(user, "user must not be null");
        return usersService.createUsers(safeUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        usersService.deleteUser(id);
    }
}
