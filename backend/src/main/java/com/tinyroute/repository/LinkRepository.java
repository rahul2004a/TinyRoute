package com.tinyroute.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface LinkRepository {
    int tombstoneAllByOwnerId(UUID ownerId, Instant now);
    List<String> findDeletionCleanupCodes(UUID ownerId, String afterCode);
}
