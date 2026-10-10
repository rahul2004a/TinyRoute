package com.tinyroute.model;

import java.time.Instant;

public record RedirectLinkState(
        String code,
        LinkStatus status,
        String destinationUrl,
        Instant deletedAt,
        Instant expiresAt) {}
