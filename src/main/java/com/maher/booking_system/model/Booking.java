package com.maher.booking_system.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer user_id;
    private Integer resource_id;
    private Integer time_slot_id;
    private String status;
    private LocalDateTime booking_time;
    private String customer_name;
    private String service_name;

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUser_id() { return user_id; }
    public void setUser_id(Integer user_id) { this.user_id = user_id; }

    public Integer getResource_id() { return resource_id; }
    public void setResource_id(Integer resource_id) { this.resource_id = resource_id; }

    public Integer getTime_slot_id() { return time_slot_id; }
    public void setTime_slot_id(Integer time_slot_id) { this.time_slot_id = time_slot_id; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getBooking_time() { return booking_time; }
    public void setBooking_time(LocalDateTime booking_time) { this.booking_time = booking_time; }

    public String getCustomer_name() { return customer_name; }
    public void setCustomer_name(String customer_name) { this.customer_name = customer_name; }

    public String getService_name() { return service_name; }
    public void setService_name(String service_name) { this.service_name = service_name; }
}