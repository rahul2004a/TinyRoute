package com.tinyroute.cache;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;

class StoreFailureBackoffTest {
    @Test
    void skipsFailedStoreAndAdmitsOnlyOneRecoveryProbe() throws Exception {
        var ticks = new AtomicLong();
        var backoff = new StoreFailureBackoff(Duration.ofSeconds(1), ticks::get);
        assertThat(backoff.tryAcquire()).isTrue();
        backoff.recordFailure();
        assertThat(backoff.tryAcquire()).isFalse();
        ticks.set(Duration.ofSeconds(1).toNanos());
        var admitted = new AtomicInteger();
        try (var pool = Executors.newFixedThreadPool(8)) {
            var futures = new java.util.ArrayList<Future<?>>();
            for (int i = 0; i < 20; i++)
                futures.add(
                        pool.submit(
                                () -> {
                                    if (backoff.tryAcquire()) admitted.incrementAndGet();
                                }));
            for (var future : futures) future.get(5, TimeUnit.SECONDS);
        }
        assertThat(admitted.get()).isEqualTo(1);
        backoff.recordSuccess();
        assertThat(backoff.tryAcquire()).isTrue();
        assertThat(backoff.tryAcquire()).isTrue();
    }
}
