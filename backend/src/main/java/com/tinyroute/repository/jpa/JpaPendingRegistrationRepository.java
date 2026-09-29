package com.tinyroute.repository.jpa;

import com.tinyroute.model.PendingRegistration;
import com.tinyroute.repository.PendingRegistrationRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaPendingRegistrationRepository extends JpaRepository<PendingRegistration, UUID>, PendingRegistrationRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select pendingRegistration from PendingRegistration pendingRegistration where pendingRegistration.tokenHash = :tokenHash")
    Optional<PendingRegistration> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
