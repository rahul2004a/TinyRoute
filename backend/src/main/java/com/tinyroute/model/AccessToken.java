package com.tinyroute.model;

import java.time.Instant;
import java.util.UUID;

public record AccessToken(
        UUID userId,
        UUID tokenId,
        Instant issuedAt,
        Instant expiresAt,
        int tokenVersion
) {
}
