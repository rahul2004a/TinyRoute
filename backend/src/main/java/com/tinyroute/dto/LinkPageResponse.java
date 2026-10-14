package com.tinyroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record LinkPageResponse(List<Item> items, String nextCursor) {
    public LinkPageResponse {
        items = List.copyOf(items);
    }

    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Item(
            UUID id,
            String code,
            String shortUrl,
            String destinationUrl,
            Instant createdAt,
            Instant expiresAt,
            String status,
            long clickCount) {}
}
