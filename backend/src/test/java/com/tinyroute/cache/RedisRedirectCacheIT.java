package com.tinyroute.cache;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.*;
import com.tinyroute.model.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
class RedisRedirectCacheIT {
    @Autowired RedirectCache cache;
    @Autowired StringRedisTemplate redis;
    @Autowired ObjectMapper json;

    @Test
    void storesAnExactSnapshotWithABoundedOriginalDeadline() {
        String code = "cache-" + UUID.randomUUID();
        Instant now = Instant.now();
        Instant deadline = now.plusSeconds(2);
        var value =
                new RedirectLookup(
                        1,
                        code,
                        LinkStatus.ACTIVE,
                        "https://example.com/a?q=java#setup",
                        null,
                        now,
                        deadline);
        cache.put(value);
        assertThat(cache.get(code)).contains(value);
        assertThat(redis.getExpire("redirect:" + code, TimeUnit.MILLISECONDS)).isBetween(1L, 2000L);
        cache.evict(code);
        assertThat(cache.get(code)).isEmpty();
    }

    @Test
    void sanitizesInactiveDestinationsAndDoesNotWriteElapsedDeadlines() {
        String code = "cache-" + UUID.randomUUID();
        Instant now = Instant.now();
        cache.put(
                new RedirectLookup(
                        1,
                        code,
                        LinkStatus.DISABLED,
                        "https://private.example",
                        null,
                        now,
                        now.plusSeconds(5)));
        assertThat(redis.opsForValue().get("redirect:" + code)).doesNotContain("private.example");
        cache.evict(code);
        cache.put(
                new RedirectLookup(
                        1,
                        code,
                        LinkStatus.ACTIVE,
                        "https://example.com",
                        null,
                        now.minusSeconds(6),
                        now.minusSeconds(1)));
        assertThat(redis.hasKey("redirect:" + code)).isFalse();
    }

    @Test
    void missingNullExpiryAndWrongTypesAreCacheMisses() {
        String code = "cache-" + UUID.randomUUID();
        Instant now = Instant.now();
        var value =
                new RedirectLookup(
                        1,
                        code,
                        LinkStatus.ACTIVE,
                        "https://example.com",
                        null,
                        now,
                        now.plusSeconds(5));
        var node = json.valueToTree(value).deepCopy();
        ((tools.jackson.databind.node.ObjectNode) node).remove("expiresAt");
        redis.opsForValue().set("redirect:" + code, json.writeValueAsString(node));
        assertThat(cache.get(code)).isEmpty();
        redis.opsForValue().set("redirect:" + code, "{\"schemaVersion\":\"1\"}");
        assertThat(cache.get(code)).isEmpty();
        redis.opsForValue().set("redirect:" + code, "not json");
        assertThat(cache.get(code)).isEmpty();
        cache.evict(code);
    }
}
