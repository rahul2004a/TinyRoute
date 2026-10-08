package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InMemoryRedirectRateLimiterTest {
    @Test
    void keepsIndependentFirstUseWindowsAndResetsAtTheirBoundary() {
        var ticks = new AtomicLong();
        var limiter = new InMemoryRedirectRateLimiter(2, Duration.ofMinutes(1), 10_000, ticks::get);
        assertThat(limiter.allow("a").allowed()).isTrue();
        ticks.set(Duration.ofSeconds(30).toNanos());
        assertThat(limiter.allow("a").allowed()).isTrue();
        assertThat(limiter.allow("a").allowed()).isFalse();
        assertThat(limiter.allow("a").retryAfter()).isEqualTo(Duration.ofSeconds(30));
        assertThat(limiter.allow("b").allowed()).isTrue();
        ticks.set(Duration.ofMinutes(1).toNanos());
        assertThat(limiter.allow("a").allowed()).isTrue();
        assertThat(limiter.allow("b").retryAfter()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void refusesNewKeysAtCapacityWithoutEvictingActiveBudgets() {
        var ticks = new AtomicLong();
        var limiter = new InMemoryRedirectRateLimiter(1, Duration.ofMinutes(1), 10_000, ticks::get);
        for (int i = 0; i < 10_000; i++)
            assertThat(limiter.allow("client-" + i).allowed()).isTrue();
        assertThat(limiter.allow("new").allowed()).isFalse();
        assertThat(limiter.allow("client-0").allowed()).isFalse();
        ticks.set(Duration.ofMinutes(1).toNanos());
        limiter.purgeExpired();
        assertThat(limiter.allow("new").allowed()).isTrue();
    }
}
