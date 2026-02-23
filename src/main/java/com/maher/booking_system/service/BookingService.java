package com.maher.booking_system.service;

import com.maher.booking_system.model.Booking;
import com.maher.booking_system.repository.BookingRepository;
import com.maher.booking_system.repository.Time_slotsRepository;
import com.maher.booking_system.repository.UsersRepository;
import com.maher.booking_system.repository.ResourcesRepository;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class BookingService {

    private final Time_slotsService time_slotsService;

    private final ResourcesService resourcesService;

    private final UsersService usersService;

    private final BookingRepository bookingRepository;
    private final UsersRepository usersRepository;
    private final Time_slotsRepository timeSlotsRepository;
    private final ResourcesRepository resourcesRepository;

    public BookingService(BookingRepository bookingRepository, UsersRepository usersRepository, Time_slotsRepository timeSlotsRepository, 
        ResourcesRepository resourcesRepository, UsersService usersService, ResourcesService resourcesService, Time_slotsService time_slotsService)
        {
            this.bookingRepository = bookingRepository;
            this.usersRepository = usersRepository;
            this.timeSlotsRepository = timeSlotsRepository;
            this.resourcesRepository = resourcesRepository;
            this.usersService = usersService;
            this.resourcesService = resourcesService;
            this.time_slotsService = time_slotsService;
    }

    public Booking createBooking(Booking booking) {

        if(usersService.getUsersByid(booking.getUser_id()) == null) {
            throw new IllegalArgumentException("User does not exist");
        }

        if(resourcesService.getResourceById(booking.getResource_id()) == null) {
            throw new IllegalArgumentException("Resource does not exist");
        }

        if(time_slotsService.getTimeSlotById(booking.getTime_slot_id()) == null) {
            throw new IllegalArgumentException("Time slot does not exist");
        }

        boolean alreadyBooked = bookingRepository.existsByResourceIdAndTimeSlotId(
                booking.getResource_id(),
                booking.getTime_slot_id()
        );

        if(alreadyBooked) {
            throw new IllegalArgumentException("This time slot is already booked for this resource");
        }

        return bookingRepository.save(booking);
    }

    public List<Booking> getAllBookings() {
        return bookingRepository.findAll();
    }

    public Booking getBookingById(Long id) {
        return bookingRepository.findById(id).orElse(null);
    }
    public void deleteBooking(Long id) {
        bookingRepository.deleteById(id);
    }
}