package com.maher.booking_system.service;

import org.springframework.stereotype.Service;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.UsersRepository;
import java.util.List;
@Service

public class UsersService {
    private final UsersRepository usersRepository;

    public UsersService(UsersRepository usersRepository){
        this.usersRepository = usersRepository;
    }
    
    public Users createUsers(Users users){
        if(users.getName() == null || users.getEmail() == null) {
            throw new IllegalArgumentException("Name and email cannot be null");
            
        }
        if(users.getPassword() == null || users.getPassword().length() < 6) {
                throw new IllegalArgumentException("Password must be at least 6 characters long");
            }
        return usersRepository.save(users);
    }

    public Users getUsersByid(long id){
        return usersRepository.findById(id).orElse(null);
    }

    public List<Users> getAllUsers(){
        return usersRepository.findAll();
    }


}
