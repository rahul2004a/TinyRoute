package com.tinyroute.model;

import java.util.Objects;
import java.util.UUID;

public record AuthenticatedSession(UUID userId, String email, String accessToken, String refreshToken) {

    public AuthenticatedSession {
        Objects.requireNonNull(userId);
        Objects.requireNonNull(email);
        Objects.requireNonNull(accessToken);
        Objects.requireNonNull(refreshToken);
    }
}
