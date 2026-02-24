package com.maher.booking_system.controller;

import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UsersRepository usersRepository;

    public UserController(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    // GET all users
    @GetMapping
    public List<Users> getAllUsers() {
        return usersRepository.findAll();
    }
    

    // POST create user
    @PostMapping
    public @NonNull Users createUser(@RequestBody @NonNull Users user) {
        Users safeUser = Objects.requireNonNull(user, "user must not be null");
        return usersRepository.save(safeUser);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable @NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        usersRepository.deleteById(id);
    }
}
