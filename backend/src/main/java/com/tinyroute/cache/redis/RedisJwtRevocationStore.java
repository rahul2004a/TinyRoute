package com.tinyroute.cache.redis;

import com.tinyroute.cache.JwtRevocationStore;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class RedisJwtRevocationStore implements JwtRevocationStore {

    private final StringRedisTemplate redisTemplate;

    public RedisJwtRevocationStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
    }

    @Override
    public void revoke(UUID tokenId, Instant expiresAt) {
        Duration remainingLifetime = Duration.between(Instant.now(), expiresAt);
        if (!remainingLifetime.isPositive()) {
            return;
        }
        redisTemplate.opsForValue().set(key(tokenId), "1", remainingLifetime);
    }

    @Override
    public boolean isRevoked(UUID tokenId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key(tokenId)));
    }

    private String key(UUID tokenId) {
        return "revoked-access:" + tokenId;
    }
}
