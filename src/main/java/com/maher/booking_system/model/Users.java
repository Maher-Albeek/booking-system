package com.maher.booking_system.model;

import java.util.ArrayList;
import java.util.List;

public class Users {
    private Long id;
    private String name;
    private String email;
    private String password;
    private String role;
    private String firstName;
    private String lastName;
    private String address;
    private String birthDate;
    private String avatarUrl;
    private List<String> paymentMethods = new ArrayList<>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getBirthDate() { return birthDate; }
    public void setBirthDate(String birthDate) { this.birthDate = birthDate; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public List<String> getPaymentMethods() {
        if (paymentMethods == null) {
            paymentMethods = new ArrayList<>();
        }
        return paymentMethods;
    }

    public void setPaymentMethods(List<String> paymentMethods) {
        this.paymentMethods = paymentMethods == null ? new ArrayList<>() : new ArrayList<>(paymentMethods);
    }
}
