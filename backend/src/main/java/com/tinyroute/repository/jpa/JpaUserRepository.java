package com.tinyroute.repository.jpa;

import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface JpaUserRepository extends JpaRepository<User, UUID>, UserRepository {
    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select user from User user where user.id = :userId")
    Optional<User> findByIdForUpdate(@Param("userId") UUID userId);

    @Override
    @Query(value = "select pg_advisory_xact_lock(hashtextextended(cast(:creationKey as text), 0))", nativeQuery = true)
    void acquireAccountCreationLock(@Param("creationKey") String creationKey);
}
