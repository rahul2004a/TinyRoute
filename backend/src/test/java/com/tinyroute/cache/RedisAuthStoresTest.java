package com.tinyroute.cache;

import com.tinyroute.model.RefreshSessionRotation;
import com.tinyroute.config.TestJwtTokenConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class RedisAuthStoresTest {

    @Autowired
    private RefreshSessionStore refreshSessionStore;

    @Autowired
    private JwtRevocationStore jwtRevocationStore;

    @Autowired
    private RateLimitStore rateLimitStore;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final UUID userId = UUID.randomUUID();
    private final UUID tokenId = UUID.randomUUID();
    private final String firstTokenHash = "refresh-hash-" + UUID.randomUUID();
    private final String secondTokenHash = "refresh-hash-" + UUID.randomUUID();
    private final String thirdTokenHash = "refresh-hash-" + UUID.randomUUID();
    private final String rateLimitKey = "rl:test:" + UUID.randomUUID();

    @AfterEach
    void cleanUpRedisRecords() {
        refreshSessionStore.deleteAllByUserId(userId);
        redisTemplate.delete(Stream.of(
                        "refresh-used:" + firstTokenHash,
                        "refresh-used:" + secondTokenHash,
                        "refresh-used:" + thirdTokenHash,
                        "revoked-access:" + tokenId,
                        rateLimitKey
                )
                .toList());
    }

    @Test
    void atomicallyRotatesARefreshSessionAndInvalidatesItsFamilyOnReuse() {
        refreshSessionStore.create(firstTokenHash, userId);

        RefreshSessionRotation rotation = refreshSessionStore.rotate(firstTokenHash, secondTokenHash);

        assertThat(rotation.status()).isEqualTo(RefreshSessionRotation.Status.ROTATED);
        assertThat(rotation.userId()).contains(userId);
        assertThat(refreshSessionStore.rotate(firstTokenHash, thirdTokenHash).status())
                .isEqualTo(RefreshSessionRotation.Status.REUSED);
        assertThat(refreshSessionStore.rotate(secondTokenHash, thirdTokenHash).status())
                .isEqualTo(RefreshSessionRotation.Status.MISSING);
    }

    @Test
    void keepsAnAccessTokenRevocationOnlyUntilTheAccessTokenExpires() {
        Instant expiresAt = Instant.now().plusSeconds(60);

        jwtRevocationStore.revoke(tokenId, expiresAt);

        assertThat(jwtRevocationStore.isRevoked(tokenId)).isTrue();
        assertThat(redisTemplate.getExpire("revoked-access:" + tokenId)).isBetween(1L, 60L);
    }

    @Test
    void incrementsEachRateLimitKeyAtomicallyAndStartsItsExpiryWindowOnce() throws Exception {
        try (ExecutorService executor = Executors.newFixedThreadPool(5)) {
            var counts = executor.invokeAll(Stream.generate(this::incrementRateLimit).limit(5).toList())
                    .stream()
                    .map(this::getCount)
                    .sorted()
                    .toList();

            assertThat(counts).containsExactly(1L, 2L, 3L, 4L, 5L);
        }

        assertThat(redisTemplate.getExpire(rateLimitKey)).isBetween(1L, 60L);
    }

    private Callable<Long> incrementRateLimit() {
        return () -> rateLimitStore.increment(rateLimitKey, Duration.ofMinutes(1)).count();
    }

    private long getCount(Future<Long> future) {
        try {
            return future.get();
        } catch (Exception exception) {
            throw new AssertionError(exception);
        }
    }
}
