package com.tinyroute.repository.jpa;

import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {
}
