package com.tinyroute.repository;

import com.tinyroute.model.AccountDeletionCleanup;

import java.time.Instant;
import java.util.List;

public interface AccountDeletionCleanupRepository {
    AccountDeletionCleanup save(AccountDeletionCleanup cleanup);
    List<AccountDeletionCleanup> findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(Instant now);
    void delete(AccountDeletionCleanup cleanup);
}
