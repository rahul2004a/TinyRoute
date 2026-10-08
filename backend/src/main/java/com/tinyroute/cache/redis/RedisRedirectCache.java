package com.tinyroute.cache.redis;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.cache.StoreFailureBackoff;
import com.tinyroute.config.LinkProperties;
import com.tinyroute.model.*;
import java.time.*;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.*;

@Component
public class RedisRedirectCache implements RedirectCache {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper json;
    private final Clock clock;
    private final LinkProperties properties;
    private final StoreFailureBackoff backoff =
            new StoreFailureBackoff(Duration.ofSeconds(1), System::nanoTime);
    private static final Set<String> FIELDS =
            Set.of(
                    "schemaVersion",
                    "code",
                    "status",
                    "destinationUrl",
                    "expiresAt",
                    "loadedAt",
                    "validUntil");

    public RedisRedirectCache(
            StringRedisTemplate redisTemplate,
            ObjectMapper json,
            Clock clock,
            LinkProperties properties) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
        this.json = json;
        this.clock = clock;
        this.properties = properties;
    }

    @Override
    public Optional<RedirectLookup> get(String code) {
        if (!backoff.tryAcquire()) return Optional.empty();
        final String value;
        try {
            value = redisTemplate.opsForValue().get("redirect:" + code);
            backoff.recordSuccess();
        } catch (RuntimeException e) {
            backoff.recordFailure();
            return Optional.empty();
        }
        if (value == null || value.length() > 16_384) return Optional.empty();
        try {
            var node = json.readTree(value);
            if (!node.isObject()
                    || node.propertyNames().size() != FIELDS.size()
                    || !node.propertyNames().containsAll(FIELDS)
                    || !node.get("schemaVersion").isIntegralNumber()
                    || node.get("schemaVersion").asLong() != 1
                    || !node.get("code").isString()
                    || !node.get("status").isString()
                    || !(node.get("destinationUrl").isString()
                            || node.get("destinationUrl").isNull())
                    || !(node.get("expiresAt").isString() || node.get("expiresAt").isNull())
                    || !node.get("loadedAt").isString()
                    || !node.get("validUntil").isString()) return Optional.empty();
            var lookup =
                    new RedirectLookup(
                            1,
                            node.get("code").asString(),
                            LinkStatus.valueOf(node.get("status").asString()),
                            node.get("destinationUrl").isNull()
                                    ? null
                                    : node.get("destinationUrl").asString(),
                            node.get("expiresAt").isNull()
                                    ? null
                                    : Instant.parse(node.get("expiresAt").asString()),
                            Instant.parse(node.get("loadedAt").asString()),
                            Instant.parse(node.get("validUntil").asString()));
            return lookup.isUsableFor(code, clock.instant(), properties.shortHost())
                    ? Optional.of(lookup)
                    : Optional.empty();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(RedirectLookup lookup) {
        var safe =
                new RedirectLookup(
                        lookup.schemaVersion(),
                        lookup.code(),
                        lookup.status(),
                        lookup.status() == LinkStatus.ACTIVE ? lookup.destinationUrl() : null,
                        lookup.expiresAt(),
                        lookup.loadedAt(),
                        lookup.validUntil());
        if (!safe.isUsableFor(safe.code(), clock.instant(), properties.shortHost())) return;
        String value = json.writeValueAsString(safe);
        if (!backoff.tryAcquire()) return;
        try {
            long ttl = Duration.between(clock.instant(), safe.validUntil()).toMillis();
            if (ttl > 0)
                redisTemplate
                        .opsForValue()
                        .set("redirect:" + safe.code(), value, Duration.ofMillis(ttl));
            backoff.recordSuccess();
        } catch (RuntimeException e) {
            backoff.recordFailure();
        }
    }

    @Override
    public void evict(String code) {
        redisTemplate.delete("redirect:" + Objects.requireNonNull(code));
    }
}
