package com.tinyroute.cache.redis;

import com.tinyroute.cache.RedirectCache;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class RedisRedirectCache implements RedirectCache {
    private final StringRedisTemplate redisTemplate;

    public RedisRedirectCache(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
    }

    @Override
    public void evict(String code) {
        redisTemplate.delete("redirect:" + Objects.requireNonNull(code));
    }
}
