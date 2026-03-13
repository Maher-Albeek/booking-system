package com.maher.booking_system.service;

import com.maher.booking_system.exception.BadRequestException;
import com.maher.booking_system.exception.NotFoundException;
import com.maher.booking_system.model.Branch;
import com.maher.booking_system.repository.BranchRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class BranchService {
    private final BranchRepository branchRepository;

    public BranchService(BranchRepository branchRepository) {
        this.branchRepository = branchRepository;
    }

    public List<Branch> getAll() {
        return branchRepository.findAll();
    }

    public Branch create(Branch branch) {
        return branchRepository.save(normalize(branch));
    }

    public Branch update(Long id, Branch branch) {
        branchRepository.findById(id).orElseThrow(() -> new NotFoundException("Branch not found"));
        Branch normalized = normalize(branch);
        normalized.setId(id);
        return branchRepository.save(normalized);
    }

    public void delete(Long id) {
        branchRepository.deleteById(id);
    }

    public void validateBookingWindow(Long branchId, LocalDateTime pickupDateTime, LocalDateTime returnDateTime) {
        if (branchId == null) {
            return;
        }
        Branch branch = branchRepository.findById(branchId).orElseThrow(() -> new NotFoundException("Branch not found"));
        ensureInsideWorkingHours(branch, pickupDateTime, "pickup");
        ensureInsideWorkingHours(branch, returnDateTime, "return");
    }

    private Branch normalize(Branch input) {
        Branch safe = Objects.requireNonNull(input, "branch must not be null");
        if (safe.getName() == null || safe.getName().trim().isBlank()) {
            throw new BadRequestException("name is required");
        }
        Branch branch = new Branch();
        branch.setName(safe.getName().trim());
        branch.setCity(safe.getCity() == null ? null : safe.getCity().trim());
        branch.setAddress(safe.getAddress() == null ? null : safe.getAddress().trim());
        branch.setAirportPickupSupported(safe.isAirportPickupSupported());
        branch.setAirportPickupFeeCents(safe.getAirportPickupFeeCents() == null ? 0L : Math.max(0, safe.getAirportPickupFeeCents()));
        branch.setWorkingHours(safe.getWorkingHours());
        branch.setClosedDates(safe.getClosedDates());
        return branch;
    }

    private void ensureInsideWorkingHours(Branch branch, LocalDateTime dateTime, String label) {
        LocalDate date = dateTime.toLocalDate();
        if (branch.getClosedDates().contains(date.toString())) {
            throw new BadRequestException("Branch is closed on " + date + " for " + label);
        }

        DayOfWeek dayOfWeek = dateTime.getDayOfWeek();
        String dayKey = dayOfWeek.name().toLowerCase(Locale.ROOT);
        String window = branch.getWorkingHours().get(dayKey);
        if (window == null || window.isBlank()) {
            return;
        }
        String[] parts = window.split("-", 2);
        if (parts.length != 2) {
            throw new BadRequestException("Invalid working hour format for " + dayKey);
        }
        try {
            LocalTime start = LocalTime.parse(parts[0].trim());
            LocalTime end = LocalTime.parse(parts[1].trim());
            LocalTime actual = dateTime.toLocalTime();
            if (actual.isBefore(start) || actual.isAfter(end)) {
                throw new BadRequestException("Booking " + label + " is outside branch working hours");
            }
        } catch (DateTimeParseException ex) {
            throw new BadRequestException("Invalid working hour format for " + dayKey);
        }
    }
}
