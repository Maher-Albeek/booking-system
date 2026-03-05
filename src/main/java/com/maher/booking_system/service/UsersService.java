package com.maher.booking_system.service;

import com.maher.booking_system.dto.UpdateUserRequest;
import com.maher.booking_system.dto.UserResponse;
import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
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

    public @NonNull UserResponse getUserResponseById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return toUserResponse(getRequiredUser(id));
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

    public @NonNull UserResponse updateUserProfile(@NonNull Long id, @NonNull UpdateUserRequest request) {
        Objects.requireNonNull(id, "id must not be null");
        UpdateUserRequest safeRequest = Objects.requireNonNull(request, "request must not be null");

        Users existingUser = getRequiredUser(id);
        String addressStreet = normalizeAddressPart(safeRequest.addressStreet());
        String addressHouseNumber = normalizeAddressPart(safeRequest.addressHouseNumber());
        String addressPostalCode = normalizeAddressPart(safeRequest.addressPostalCode());
        String addressCity = normalizeAddressPart(safeRequest.addressCity());
        String addressCountry = normalizeAddressPart(safeRequest.addressCountry());
        String legacyAddress = normalizeOptional(safeRequest.address());

        if (legacyAddress != null && addressStreet == null) {
            addressStreet = legacyAddress;
        }

        existingUser.setFirstName(normalizeOptional(safeRequest.firstName()));
        existingUser.setLastName(normalizeOptional(safeRequest.lastName()));
        existingUser.setAddressStreet(addressStreet);
        existingUser.setAddressHouseNumber(addressHouseNumber);
        existingUser.setAddressPostalCode(addressPostalCode);
        existingUser.setAddressCity(addressCity);
        existingUser.setAddressCountry(addressCountry);
        existingUser.setAddress(buildLegacyAddress(addressStreet, addressHouseNumber, addressPostalCode, addressCity, addressCountry));
        existingUser.setBirthDate(normalizeBirthDate(safeRequest.birthDate()));
        existingUser.setAvatarUrl(normalizeAvatarUrl(safeRequest.avatarUrl()));
        existingUser.setPaymentMethods(PaymentMethodCatalog.normalizeList(safeRequest.paymentMethods()));

        String displayName = buildDisplayName(existingUser.getFirstName(), existingUser.getLastName());
        if (displayName != null) {
            existingUser.setName(displayName);
        }

        return toUserResponse(usersRepository.save(existingUser));
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
        safeUser.setFirstName(normalizeOptional(safeUser.getFirstName()));
        safeUser.setLastName(normalizeOptional(safeUser.getLastName()));
        String addressStreet = normalizeAddressPart(safeUser.getAddressStreet());
        String addressHouseNumber = normalizeAddressPart(safeUser.getAddressHouseNumber());
        String addressPostalCode = normalizeAddressPart(safeUser.getAddressPostalCode());
        String addressCity = normalizeAddressPart(safeUser.getAddressCity());
        String addressCountry = normalizeAddressPart(safeUser.getAddressCountry());
        String legacyAddress = normalizeOptional(safeUser.getAddress());

        if (legacyAddress != null && addressStreet == null) {
            addressStreet = legacyAddress;
        }

        safeUser.setAddressStreet(addressStreet);
        safeUser.setAddressHouseNumber(addressHouseNumber);
        safeUser.setAddressPostalCode(addressPostalCode);
        safeUser.setAddressCity(addressCity);
        safeUser.setAddressCountry(addressCountry);
        safeUser.setAddress(buildLegacyAddress(addressStreet, addressHouseNumber, addressPostalCode, addressCity, addressCountry));
        safeUser.setBirthDate(normalizeBirthDate(safeUser.getBirthDate()));
        safeUser.setAvatarUrl(normalizeAvatarUrl(safeUser.getAvatarUrl()));
        safeUser.setPaymentMethods(PaymentMethodCatalog.normalizeList(safeUser.getPaymentMethods()));

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

    private Users getRequiredUser(Long id) {
        return usersRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }

    private String normalizeOptional(String value) {
        if (value == null) {
            return null;
        }

        String normalized = value.trim();
        return normalized.isBlank() ? null : normalized;
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        return normalizeOptional(avatarUrl);
    }

    private String normalizeAddressPart(String value) {
        return normalizeOptional(value);
    }

    private String buildLegacyAddress(
            String street,
            String houseNumber,
            String postalCode,
            String city,
            String country
    ) {
        String firstLine = Arrays.stream(new String[]{street, houseNumber})
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
        String secondLine = Arrays.stream(new String[]{postalCode, city})
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);

        return Arrays.stream(new String[]{firstLine, secondLine, country})
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + ", " + right)
                .orElse(null);
    }

    private String normalizeBirthDate(String birthDate) {
        String normalized = normalizeOptional(birthDate);
        if (normalized == null) {
            return null;
        }

        try {
            return LocalDate.parse(normalized).toString();
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("birthDate must use the YYYY-MM-DD format");
        }
    }

    private String buildDisplayName(String firstName, String lastName) {
        return Arrays.stream(new String[]{firstName, lastName})
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " " + right)
                .orElse(null);
    }

    private UserResponse toUserResponse(Users user) {
        String legacyAddress = normalizeOptional(user.getAddress());
        String addressStreet = normalizeAddressPart(user.getAddressStreet());
        String addressHouseNumber = normalizeAddressPart(user.getAddressHouseNumber());
        String addressPostalCode = normalizeAddressPart(user.getAddressPostalCode());
        String addressCity = normalizeAddressPart(user.getAddressCity());
        String addressCountry = normalizeAddressPart(user.getAddressCountry());

        if (legacyAddress != null && addressStreet == null) {
            addressStreet = legacyAddress;
        }

        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                normalizeRole(user.getRole()),
                user.getFirstName(),
                user.getLastName(),
                legacyAddress,
                addressStreet,
                addressHouseNumber,
                addressPostalCode,
                addressCity,
                addressCountry,
                user.getBirthDate(),
                user.getAvatarUrl(),
                List.copyOf(user.getPaymentMethods())
        );
    }
}
