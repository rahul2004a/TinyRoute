package com.tinyroute.repository.jpa;

import com.tinyroute.model.PasswordResetToken;
import com.tinyroute.repository.PasswordResetTokenRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface JpaPasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID>, PasswordResetTokenRepository {

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.user.id = :userId")
    Optional<PasswordResetToken> findByUserIdForUpdate(@Param("userId") UUID userId);

    @Override
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from PasswordResetToken token where token.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);
}
