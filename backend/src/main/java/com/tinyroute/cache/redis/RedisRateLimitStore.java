package com.tinyroute.cache.redis;

import com.tinyroute.cache.RateLimitStore;
import com.tinyroute.model.RateLimitCounter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class RedisRateLimitStore implements RateLimitStore {

    private static final DefaultRedisScript<List> INCREMENT_WITH_WINDOW = new DefaultRedisScript<>(
            "local count = redis.call('INCR', KEYS[1]); "
                    + "if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); end; "
                    + "return {count, redis.call('PTTL', KEYS[1])};",
            List.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRateLimitStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
    }

    @Override
    public RateLimitCounter increment(String key, Duration window) {
        if (key == null || key.isBlank() || window == null || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Rate limit key or window is invalid");
        }
        List<?> response = redisTemplate.execute(INCREMENT_WITH_WINDOW, List.of(key), Long.toString(window.toMillis()));
        if (response == null || response.size() != 2) {
            throw new IllegalStateException("Redis rate limit response is invalid");
        }
        return new RateLimitCounter(numberAt(response, 0), Duration.ofMillis(numberAt(response, 1)));
    }

    private long numberAt(List<?> response, int index) {
        Object value = response.get(index);
        if (!(value instanceof Number number) || number.longValue() < 0) {
            throw new IllegalStateException("Redis rate limit response is invalid");
        }
        return number.longValue();
    }
}
