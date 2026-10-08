package com.tinyroute.repository;

import com.tinyroute.model.RedirectLinkState;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LinkRepository {
    Optional<Integer> lockActiveOwnerTokenVersion(UUID ownerId);

    int insertIfCodeAvailable(
            UUID id,
            String code,
            UUID ownerId,
            String destinationUrl,
            Instant createdAt,
            Instant expiresAt,
            Long generationValue);

    long findMaxGenerationValue();

    Optional<RedirectLinkState> findRedirectStateByCode(String code);

    int tombstoneAllByOwnerId(UUID ownerId, Instant now);

    List<String> findDeletionCleanupCodes(UUID ownerId, String afterCode);
}
