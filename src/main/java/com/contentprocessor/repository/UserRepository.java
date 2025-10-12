package com.contentprocessor.repository;

import com.contentprocessor.model.entities.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByMicrosoftId(String microsoftId);
    boolean existsByEmail(String email);
}
