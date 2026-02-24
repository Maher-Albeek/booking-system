package com.maher.booking_system.service;

import java.util.List;
import java.util.Objects;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.ResourcesRepository;
import com.maher.booking_system.repository.Time_slotsRepository;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

@Service
public class BookingService {

    private final Time_slotsService time_slotsService;
    private final ResourcesService resourcesService;
    private final UsersService usersService;
    private final BookingRepository bookingRepository;

    public final UsersRepository usersRepository;
    public final Time_slotsRepository timeSlotsRepository;
    public final ResourcesRepository resourcesRepository;

    public BookingService(BookingRepository bookingRepository,
                          UsersRepository usersRepository,
                          Time_slotsRepository timeSlotsRepository,
                          ResourcesRepository resourcesRepository,
                          UsersService usersService,
                          ResourcesService resourcesService,
                          Time_slotsService time_slotsService) {
        this.bookingRepository = bookingRepository;
        this.usersRepository = usersRepository;
        this.timeSlotsRepository = timeSlotsRepository;
        this.resourcesRepository = resourcesRepository;
        this.usersService = usersService;
        this.resourcesService = resourcesService;
        this.time_slotsService = time_slotsService;
    }

    public @NonNull Booking createBooking(@NonNull Booking booking) {
        Booking safeBooking = Objects.requireNonNull(booking, "booking must not be null");

        if (safeBooking.getUserId() == null || usersService.getUsersById(safeBooking.getUserId()) == null) {
            throw new IllegalArgumentException("User does not exist");
        }

        if (safeBooking.getResourceId() == null || resourcesService.getResourceById(safeBooking.getResourceId()) == null) {
            throw new IllegalArgumentException("Resource does not exist");
        }

        Long timeSlotId = safeBooking.getTimeSlotId();
        if (timeSlotId == null || time_slotsService.getTimeSlotById(timeSlotId) == null) {
            throw new IllegalArgumentException("Time slot does not exist");
        }

        boolean alreadyBooked = bookingRepository.existsByResourceIdAndTimeSlotId(
            safeBooking.getResourceId(),
            safeBooking.getTimeSlotId()
        );

        if (alreadyBooked) {
            throw new IllegalArgumentException("This time slot is already booked for this resource");
        }

        return bookingRepository.save(safeBooking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        return bookingRepository.findById(id).orElse(null);
    }

    public void deleteBooking(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        bookingRepository.deleteById(id);
    }
}
