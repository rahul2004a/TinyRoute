package com.tinyroute.repository;

import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.AuthProvider;

import java.util.Optional;
import java.util.UUID;

public interface AuthIdentityRepository {

    Optional<AuthIdentity> findByProviderAndSubject(AuthProvider provider, String subject);

    Optional<AuthIdentity> findByUserIdAndProvider(UUID userId, AuthProvider provider);

    AuthIdentity save(AuthIdentity authIdentity);
}
