package com.tinyroute.cache;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.*;
import com.tinyroute.exception.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
class RedisShortCodeCounterIT {
    @Autowired ShortCodeCounter counter;
    @Autowired StringRedisTemplate redis;

    @BeforeEach
    @AfterEach
    void clear() {
        redis.delete("code:global");
    }

    @Test
    void missingCounterRequiresAuthoritativeInitialization() {
        assertThat(counter.nextValueIfInitialized()).isEmpty();
        assertThat(counter.advanceAndIncrement(62)).isEqualTo(63);
        assertThat(counter.nextValueIfInitialized()).hasValue(64);
        assertThat(redis.getExpire("code:global")).isEqualTo(-1);
        assertThat(counter.advanceAndIncrement(1)).isEqualTo(65);
        assertThat(counter.advanceAndIncrement(1000)).isEqualTo(1001);
    }

    @Test
    void concurrentInitializationAndAllocationNeverLowerOrDuplicateTheCounter() throws Exception {
        var start = new CountDownLatch(1);
        try (var pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<Long>> results = new ArrayList<>();
            for (int i = 0; i < 100; i++)
                results.add(
                        pool.submit(
                                () -> {
                                    assertThat(start.await(5, TimeUnit.SECONDS)).isTrue();
                                    return counter.advanceAndIncrement(1000);
                                }));
            start.countDown();
            Set<Long> allocated = new HashSet<>();
            for (var result : results)
                assertThat(allocated.add(result.get(10, TimeUnit.SECONDS))).isTrue();
            assertThat(allocated).hasSize(100).contains(1001L, 1100L);
            List<Future<Long>> ordinary = new ArrayList<>();
            for (int i = 0; i < 100; i++)
                ordinary.add(pool.submit(() -> counter.nextValueIfInitialized().orElseThrow()));
            for (var result : ordinary)
                assertThat(allocated.add(result.get(10, TimeUnit.SECONDS))).isTrue();
            assertThat(allocated).hasSize(200).contains(1101L, 1200L);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "bad",
                "-1",
                "01",
                "1.2",
                "1e2",
                "218340105584896",
                "9999999999999999999999999"
            })
    void malformedCounterCannotBeResetOrPublished(String value) {
        redis.opsForValue().set("code:global", value);
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> counter.advanceAndIncrement(1000))
                .isInstanceOf(ServiceUnavailableException.class);
        assertThat(redis.opsForValue().get("code:global")).isEqualTo(value);
    }

    @Test
    void expiringOrWrongTypeCounterCannotBeSilentlyReinitialized() {
        redis.opsForValue().set("code:global", "1", Duration.ofMinutes(1));
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> counter.advanceAndIncrement(1000))
                .isInstanceOf(ServiceUnavailableException.class);
        redis.delete("code:global");
        redis.opsForList().leftPush("code:global", "1");
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void capacityNeverWrapsOrIncrements() {
        redis.opsForValue().set("code:global", "218340105584895");
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ShortCodeCounterExhaustedException.class);
        assertThat(redis.opsForValue().get("code:global")).isEqualTo("218340105584895");
    }
}
