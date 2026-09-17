package com.tinyroute.repository;

import com.tinyroute.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {

    Optional<User> findById(UUID userId);

    Optional<User> findByEmailNormalized(String emailNormalized);

    User save(User user);
}