package com.tinyroute.model;

import java.util.Objects;
import java.util.UUID;

public record RefreshSession(UUID userId, int tokenVersion, UUID accessTokenId, String familyId) {

    public RefreshSession {
        Objects.requireNonNull(userId);
        if (tokenVersion < 0) {
            throw new IllegalArgumentException("Token version must not be negative");
        }
        Objects.requireNonNull(accessTokenId);
        if (familyId == null || familyId.isBlank()) {
            throw new IllegalArgumentException("Refresh session family is required");
        }
    }
}
