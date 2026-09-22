package com.tinyroute.model;

import java.util.Optional;

public record RefreshSessionRotation(Status status, Optional<RefreshSession> session) {

    public enum Status {
        ROTATED,
        MISSING,
        REUSED
    }

    public static RefreshSessionRotation rotated(RefreshSession session) {
        return new RefreshSessionRotation(Status.ROTATED, Optional.of(session));
    }

    public static RefreshSessionRotation missing() {
        return new RefreshSessionRotation(Status.MISSING, Optional.empty());
    }

    public static RefreshSessionRotation reused() {
        return new RefreshSessionRotation(Status.REUSED, Optional.empty());
    }
}
