package com.tinyroute.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.tinyroute.cache.redis.RedisRedirectCache;
import com.tinyroute.config.LinkProperties;
import java.time.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.*;
import tools.jackson.databind.json.JsonMapper;

class RedisRedirectCacheTest {
    @Test
    void parallelCallersSkipRedisDuringTheKnownOutageCooldown() throws Exception {
        var redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("redirect:Abc"))
                .thenThrow(new DataAccessResourceFailureException("unavailable"));
        var properties = new LinkProperties();
        properties.setShortBaseUrl("https://localhost:8443");
        var cache = new RedisRedirectCache(redis, new JsonMapper(), Clock.systemUTC(), properties);
        assertThat(cache.get("Abc")).isEmpty();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 50; i++)
                futures.add(executor.submit(() -> assertThat(cache.get("Abc")).isEmpty()));
            for (var future : futures) future.get();
        }
        verify(values, times(1)).get("redirect:Abc");
    }
}
