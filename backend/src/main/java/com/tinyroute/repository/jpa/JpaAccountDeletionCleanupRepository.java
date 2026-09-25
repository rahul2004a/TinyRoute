package com.tinyroute.repository.jpa;

import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JpaAccountDeletionCleanupRepository
        extends JpaRepository<AccountDeletionCleanup, UUID>, AccountDeletionCleanupRepository {
}
