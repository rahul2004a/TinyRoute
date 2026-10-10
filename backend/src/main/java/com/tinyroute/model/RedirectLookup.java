package com.tinyroute.model;

import java.time.Instant;

/** Versioned snapshot, valid only within its original read/expiry deadline. */
public record RedirectLookup(
        int schemaVersion,
        String code,
        LinkStatus status,
        String destinationUrl,
        Instant expiresAt,
        Instant loadedAt,
        Instant validUntil) {
    public boolean isUsableFor(String requestedCode, Instant now, String shortHost) {
        if (schemaVersion != 1
                || code == null
                || !code.equals(requestedCode)
                || status == null
                || loadedAt == null
                || validUntil == null
                || loadedAt.isAfter(now)
                || !validUntil.isAfter(loadedAt)
                || !validUntil.isAfter(now)
                || validUntil.isAfter(loadedAt.plusSeconds(5))
                || (expiresAt != null
                        && (!expiresAt.isAfter(now) || validUntil.isAfter(expiresAt))))
            return false;
        try {
            new ShortCode(code);
            if (ShortCode.isReserved(code)) return false;
            if (status == LinkStatus.ACTIVE) DestinationUrl.parse(destinationUrl, shortHost);
            else if (destinationUrl != null) return false;
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
