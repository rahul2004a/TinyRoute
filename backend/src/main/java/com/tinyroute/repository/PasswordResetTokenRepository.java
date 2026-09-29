package com.tinyroute.repository;

import com.tinyroute.model.PasswordResetToken;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository {

    Optional<PasswordResetToken> findByUserIdForUpdate(UUID userId);

    Optional<PasswordResetToken> findByTokenHashForUpdate(String tokenHash);

    PasswordResetToken save(PasswordResetToken passwordResetToken);

    void delete(PasswordResetToken passwordResetToken);

    void deleteByUser_Id(UUID userId);
}
