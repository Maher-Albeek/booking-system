package com.maher.booking_system.service;

import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
public class UsersService {
    private final UsersRepository usersRepository;

    public UsersService(UsersRepository usersRepository){
        this.usersRepository = usersRepository;
    }

    public @NonNull UserResponse createUsers(@NonNull Users users){
        Users safeUser = Objects.requireNonNull(users, "users must not be null");

        if(safeUser.getName() == null || safeUser.getName().isBlank() || safeUser.getEmail() == null || safeUser.getEmail().isBlank()) {
            throw new IllegalArgumentException("Name and email cannot be null");
        }
        if(safeUser.getPassword() == null || safeUser.getPassword().trim().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        safeUser.setName(safeUser.getName().trim());
        safeUser.setEmail(safeUser.getEmail().trim());
        safeUser.setPassword(safeUser.getPassword().trim());
        safeUser.setRole(normalizeRole(safeUser.getRole()));

        return toUserResponse(usersRepository.save(safeUser));
    }

    public Users getUsersById(long id){
        ensureDefaultAccounts();
        return usersRepository.findById(id).orElse(null);
    }

    public List<Users> getAllUsers(){
        ensureDefaultAccounts();
        return usersRepository.findAll();
    }

    public List<UserResponse> getAllUserResponses() {
        return getAllUsers().stream()
                .sorted(Comparator.comparing(user -> user.getName() == null ? "" : user.getName(), String.CASE_INSENSITIVE_ORDER))
                .map(this::toUserResponse)
                .toList();
    }

    public @NonNull UserResponse authenticate(String identifier, String password) {
        String safeIdentifier = identifier == null ? "" : identifier.trim();
        String safePassword = password == null ? "" : password.trim();

        if (safeIdentifier.isBlank() || safePassword.isBlank()) {
            throw new IllegalArgumentException("Email or username and password are required");
        }

        return getAllUsers().stream()
                .filter(user -> matchesIdentifier(user, safeIdentifier))
                .filter(user -> safePassword.equals(user.getPassword()))
                .findFirst()
                .map(this::toUserResponse)
                .orElseThrow(() -> new IllegalArgumentException("Invalid email, username, or password"));
    }

    public void deleteUser(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        usersRepository.deleteById(id);
    }

    private void ensureDefaultAccounts() {
        List<Users> existingUsers = new ArrayList<>(usersRepository.findAll());
        boolean hasLoginReadyAccounts = existingUsers.stream().anyMatch(this::isLoginReady);

        if (hasLoginReadyAccounts) {
            return;
        }

        if (existingUsers.isEmpty()) {
            usersRepository.save(buildDefaultUser("Demo User", "user@booking.local", "user123", "USER"));
            usersRepository.save(buildDefaultUser("Demo Admin", "admin@booking.local", "admin123", "ADMIN"));
            return;
        }

        boolean hasAdminAccount = existingUsers.stream()
                .anyMatch(user -> "ADMIN".equals(normalizeRole(user.getRole())) && hasPassword(user));

        for (Users user : existingUsers) {
            if (user.getName() == null || user.getName().isBlank()) {
                user.setName("User " + user.getId());
            }

            if (user.getEmail() == null || user.getEmail().isBlank()) {
                user.setEmail(("user" + user.getId() + "@booking.local").toLowerCase(Locale.ROOT));
            }

            if (!hasPassword(user)) {
                user.setPassword("user123");
            }

            user.setRole(hasAdminAccount ? normalizeRole(user.getRole()) : "ADMIN");
            hasAdminAccount = true;
            usersRepository.save(user);
        }
    }

    private boolean matchesIdentifier(Users user, String identifier) {
        return identifier.equalsIgnoreCase(user.getEmail()) || identifier.equalsIgnoreCase(user.getName());
    }

    private boolean isLoginReady(Users user) {
        return hasPassword(user)
                && user.getName() != null
                && !user.getName().isBlank()
                && user.getEmail() != null
                && !user.getEmail().isBlank();
    }

    private boolean hasPassword(Users user) {
        return user.getPassword() != null && !user.getPassword().trim().isBlank();
    }

    private Users buildDefaultUser(String name, String email, String password, String role) {
        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole(role);
        return user;
    }

    private String normalizeRole(String role) {
        return "ADMIN".equalsIgnoreCase(role) ? "ADMIN" : "USER";
    }

    private UserResponse toUserResponse(Users user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                normalizeRole(user.getRole())
        );
    }
}
