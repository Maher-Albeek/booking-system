package com.maher.booking_system.service;

import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

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
        return createUser(users, false);
    }

    public @NonNull UserResponse registerUser(String name, String email, String password) {
        Users user = new Users();
        user.setName(name);
        user.setEmail(email);
        user.setPassword(password);
        user.setRole("USER");
        return createUser(user, true);
    }

    public Users getUsersById(long id){
        return usersRepository.findById(id).orElse(null);
    }

    public List<Users> getAllUsers(){
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

        return usersRepository.findAll().stream()
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

    private @NonNull UserResponse createUser(@NonNull Users users, boolean forceUserRole) {
        Users safeUser = Objects.requireNonNull(users, "users must not be null");

        String normalizedName = safeUser.getName() == null ? "" : safeUser.getName().trim();
        String normalizedEmail = safeUser.getEmail() == null ? "" : safeUser.getEmail().trim().toLowerCase(Locale.ROOT);
        String normalizedPassword = safeUser.getPassword() == null ? "" : safeUser.getPassword().trim();

        if(normalizedName.isBlank() || normalizedEmail.isBlank()) {
            throw new IllegalArgumentException("Name and email cannot be null");
        }
        if(normalizedPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }

        if (emailExists(normalizedEmail)) {
            throw new IllegalArgumentException("An account with this email already exists");
        }

        safeUser.setName(normalizedName);
        safeUser.setEmail(normalizedEmail);
        safeUser.setPassword(normalizedPassword);
        safeUser.setRole(forceUserRole ? "USER" : normalizeRole(safeUser.getRole()));

        return toUserResponse(usersRepository.save(safeUser));
    }

    private boolean matchesIdentifier(Users user, String identifier) {
        return identifier.equalsIgnoreCase(user.getEmail()) || identifier.equalsIgnoreCase(user.getName());
    }

    private boolean emailExists(String email) {
        return usersRepository.findAll().stream()
                .map(Users::getEmail)
                .filter(Objects::nonNull)
                .map(existingEmail -> existingEmail.trim().toLowerCase(Locale.ROOT))
                .anyMatch(email::equals);
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
