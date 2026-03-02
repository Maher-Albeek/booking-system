package com.maher.booking_system.service;

import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class UsersService {
    private final UsersRepository usersRepository;

    public UsersService(UsersRepository usersRepository){
        this.usersRepository = usersRepository;
    }

    public @NonNull Users createUsers(@NonNull Users users){
        Users safeUser = Objects.requireNonNull(users, "users must not be null");

        if(safeUser.getName() == null || safeUser.getEmail() == null) {
            throw new IllegalArgumentException("Name and email cannot be null");
        }
        if(safeUser.getPassword() == null || safeUser.getPassword().length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long");
        }
        return usersRepository.save(safeUser);
    }

    public Users getUsersById(long id){
        return usersRepository.findById(id).orElse(null);
    }

    public List<Users> getAllUsers(){
        return usersRepository.findAll();
    }

    public void deleteUser(@NonNull Long id) {
        Objects.requireNonNull(id, "id must not be null");
        usersRepository.deleteById(id);
    }
}
