package com.tinyroute.model;

import java.time.Instant;
import java.util.UUID;

public record CreatedLink(
        UUID id,
        ShortCode code,
        DestinationUrl destinationUrl,
        Instant createdAt,
        Instant expiresAt) {}
