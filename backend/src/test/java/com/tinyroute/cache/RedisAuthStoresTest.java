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
import java.util.concurrent.CountDownLatch;
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
    private final UUID replacementTokenId = UUID.randomUUID();
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
                        "refresh-access:" + tokenId,
                        "refresh-access:" + replacementTokenId,
                        "revoked-access:" + tokenId,
                        rateLimitKey
                )
                .toList());
    }

    @Test
    void atomicallyRotatesARefreshSessionAndInvalidatesItsFamilyOnReuse() {
        refreshSessionStore.create(firstTokenHash, userId, 0, tokenId);

        RefreshSessionRotation rotation = refreshSessionStore.rotate(firstTokenHash, secondTokenHash, replacementTokenId);

        assertThat(rotation.status()).isEqualTo(RefreshSessionRotation.Status.ROTATED);
        assertThat(rotation.session()).hasValueSatisfying(session -> assertThat(session.userId()).isEqualTo(userId));
        assertThat(refreshSessionStore.rotate(firstTokenHash, thirdTokenHash, UUID.randomUUID()).status())
                .isEqualTo(RefreshSessionRotation.Status.CONCURRENT);
        assertThat(refreshSessionStore.find(secondTokenHash)).isPresent();
        redisTemplate.opsForHash().put("refresh-used:" + firstTokenHash, "rotatedAt",
                Long.toString(Instant.now().minusSeconds(6).toEpochMilli()));
        assertThat(refreshSessionStore.rotate(firstTokenHash, thirdTokenHash, UUID.randomUUID()).status())
                .isEqualTo(RefreshSessionRotation.Status.REUSED);
        assertThat(refreshSessionStore.rotate(secondTokenHash, thirdTokenHash, UUID.randomUUID()).status())
                .isEqualTo(RefreshSessionRotation.Status.MISSING);
    }

    @Test
    void simultaneousRefreshAttemptsLeaveOneUsableSessionWithoutIssuingTwoTokens() throws Exception {
        refreshSessionStore.create(firstTokenHash, userId, 0, tokenId);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Callable<RefreshSessionRotation.Status> first = () -> rotateAfterBarrier(
                    ready, start, secondTokenHash, replacementTokenId);
            Callable<RefreshSessionRotation.Status> second = () -> rotateAfterBarrier(
                    ready, start, thirdTokenHash, UUID.randomUUID());
            Future<RefreshSessionRotation.Status> firstResult = executor.submit(first);
            Future<RefreshSessionRotation.Status> secondResult = executor.submit(second);
            ready.await();
            start.countDown();
            assertThat(java.util.Set.of(firstResult.get(), secondResult.get()))
                    .containsExactlyInAnyOrder(RefreshSessionRotation.Status.ROTATED,
                            RefreshSessionRotation.Status.CONCURRENT);
        }
        assertThat(Stream.of(secondTokenHash, thirdTokenHash)
                .filter(hash -> refreshSessionStore.find(hash).isPresent()).count()).isEqualTo(1);
    }

    @Test
    void logoutFollowsRotationButRejectsAnAccessTokenFromAnotherDevice() {
        refreshSessionStore.create(firstTokenHash, userId, 0, tokenId);
        UUID otherDeviceAccessId = UUID.randomUUID();
        String otherDeviceHash = "refresh-hash-" + UUID.randomUUID();
        refreshSessionStore.create(otherDeviceHash, userId, 0, otherDeviceAccessId);
        try {
            refreshSessionStore.rotate(firstTokenHash, secondTokenHash, replacementTokenId);
            assertThat(refreshSessionStore.deleteCurrent(secondTokenHash, userId, otherDeviceAccessId)).isFalse();
            assertThat(refreshSessionStore.find(secondTokenHash)).isPresent();
            assertThat(refreshSessionStore.deleteCurrent(firstTokenHash, userId, tokenId)).isTrue();
            assertThat(refreshSessionStore.find(secondTokenHash)).isEmpty();
            assertThat(refreshSessionStore.find(otherDeviceHash)).isPresent();
        } finally {
            refreshSessionStore.deleteAllByUserId(userId);
            redisTemplate.delete("refresh-access:" + otherDeviceAccessId);
        }
    }

    private RefreshSessionRotation.Status rotateAfterBarrier(CountDownLatch ready, CountDownLatch start,
                                                              String replacementHash, UUID accessId) throws Exception {
        ready.countDown();
        start.await();
        return refreshSessionStore.rotate(firstTokenHash, replacementHash, accessId).status();
    }

    @Test
    void keepsAnAccessTokenRevocationOnlyUntilTheAccessTokenExpires() {
        Instant expiresAt = Instant.now().plusSeconds(60);

        jwtRevocationStore.revoke(tokenId, expiresAt);

        assertThat(jwtRevocationStore.isRevoked(tokenId)).isTrue();
        assertThat(redisTemplate.getExpire("revoked-access:" + tokenId)).isBetween(60L, 120L);
    }

    @Test
    void keepsRevocationThroughTheAcceptedJwtClockSkew() {
        Instant nominalExpiry = Instant.now().minusSeconds(30);

        jwtRevocationStore.revoke(tokenId, nominalExpiry);

        assertThat(jwtRevocationStore.isRevoked(tokenId)).isTrue();
        assertThat(redisTemplate.getExpire("revoked-access:" + tokenId)).isBetween(1L, 30L);
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
