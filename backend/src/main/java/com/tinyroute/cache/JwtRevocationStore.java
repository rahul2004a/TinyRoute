package com.tinyroute.cache;

import java.time.Instant;
import java.util.UUID;

public interface JwtRevocationStore {

    void revoke(UUID tokenId, Instant expiresAt);

    boolean isRevoked(UUID tokenId);
}
