package com.tinyroute.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Owner-filtered, immutable listing results (FR-MGT-01/02). */
public record LinkPage(List<Item> items, String nextCursor) {
    public LinkPage {
        items = List.copyOf(items);
    }

    public record Item(
            UUID id,
            ShortCode code,
            String destinationUrl,
            Instant createdAt,
            Instant expiresAt,
            String status,
            long clickCount) {}
}
