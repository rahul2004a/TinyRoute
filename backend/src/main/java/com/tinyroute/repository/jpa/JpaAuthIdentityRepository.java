package com.tinyroute.repository.jpa;

import com.tinyroute.model.AuthIdentity;
import com.tinyroute.repository.AuthIdentityRepository;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAuthIdentityRepository extends JpaRepository<AuthIdentity, UUID>, AuthIdentityRepository {
}
