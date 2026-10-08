package com.tinyroute.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.ALWAYS)
public record CreateLinkResponse(
        UUID id,
        String code,
        String shortUrl,
        String destinationUrl,
        Instant createdAt,
        Instant expiresAt) {}
