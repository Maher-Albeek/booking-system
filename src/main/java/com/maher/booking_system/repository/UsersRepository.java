package com.maher.booking_system.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.maher.booking_system.model.Users;
import com.maher.booking_system.repository.support.JsonRepositorySupport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class UsersRepository extends JsonRepositorySupport<Users> {

    public UsersRepository(
            ObjectMapper objectMapper,
            @Value("${app.storage.directory:data}") String storageDirectory
    ) {
        super(objectMapper, java.nio.file.Path.of(storageDirectory), "users.json", Users.class, Users::getId, Users::setId);
    }
}
