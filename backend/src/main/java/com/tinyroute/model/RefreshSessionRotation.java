package com.tinyroute.model;

import java.util.Optional;
import java.util.UUID;

public record RefreshSessionRotation(Status status, Optional<UUID> userId) {

    public enum Status {
        ROTATED,
        MISSING,
        REUSED
    }

    public static RefreshSessionRotation rotated(UUID userId) {
        return new RefreshSessionRotation(Status.ROTATED, Optional.of(userId));
    }

    public static RefreshSessionRotation missing() {
        return new RefreshSessionRotation(Status.MISSING, Optional.empty());
    }

    public static RefreshSessionRotation reused() {
        return new RefreshSessionRotation(Status.REUSED, Optional.empty());
    }
}
