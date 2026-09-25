package com.tinyroute.repository;

import com.tinyroute.model.PendingRegistration;

import java.util.Optional;

public interface PendingRegistrationRepository {

    Optional<PendingRegistration> findByEmailNormalized(String emailNormalized);

    Optional<PendingRegistration> findByTokenHashForUpdate(String tokenHash);

    PendingRegistration save(PendingRegistration pendingRegistration);

    void delete(PendingRegistration pendingRegistration);

    void deleteByEmailNormalized(String emailNormalized);
}
