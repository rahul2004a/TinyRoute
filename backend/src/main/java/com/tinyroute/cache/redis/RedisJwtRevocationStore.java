package com.tinyroute.cache.redis;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.config.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Component
public class RedisJwtRevocationStore implements JwtRevocationStore {

    private final StringRedisTemplate redisTemplate;
    private final Duration clockSkew;

    public RedisJwtRevocationStore(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.clockSkew = Objects.requireNonNull(jwtProperties).getClockSkew();
    }

    @Override
    public void revoke(UUID tokenId, Instant expiresAt) {
        Duration remainingLifetime = Duration.between(Instant.now(), expiresAt.plus(clockSkew));
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
