package com.tinyroute.service;

import com.tinyroute.model.RateLimitDecision;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.function.LongSupplier;
import org.springframework.scheduling.annotation.Scheduled;

/** Single-process, bounded degraded redirect budgets (FR-ABS-03; NFR-PRV-02). */
public final class InMemoryRedirectRateLimiter {
    private final int maximumAttempts;
    private final long windowNanos;
    private final int maximumKeys;
    private final LongSupplier ticker;
    private final Map<String, Window> windows = new HashMap<>();

    public InMemoryRedirectRateLimiter(
            int maximumAttempts, Duration window, int maximumKeys, LongSupplier ticker) {
        if (maximumAttempts < 1 || maximumKeys < 1 || window.isNegative() || window.isZero())
            throw new IllegalArgumentException("Positive limiter bounds required");
        this.maximumAttempts = maximumAttempts;
        this.windowNanos = window.toNanos();
        this.maximumKeys = maximumKeys;
        this.ticker = ticker;
    }

    public synchronized RateLimitDecision allow(String clientHash) {
        long now = ticker.getAsLong();
        Window current = windows.get(clientHash);
        if (current != null && now - current.deadline >= 0) {
            windows.remove(clientHash);
            current = null;
        }
        if (current == null) {
            if (windows.size() >= maximumKeys) purgeExpired();
            if (windows.size() >= maximumKeys) {
                long wait =
                        windows.values().stream()
                                .mapToLong(w -> w.deadline - now)
                                .min()
                                .orElse(windowNanos);
                return new RateLimitDecision(false, Duration.ofNanos(Math.max(1, wait)));
            }
            current = new Window(now + windowNanos, 0);
            windows.put(clientHash, current);
        }
        current.count = Math.min(current.count + 1, (long) maximumAttempts + 1);
        return new RateLimitDecision(
                current.count <= maximumAttempts,
                Duration.ofNanos(Math.max(1, current.deadline - now)));
    }

    @Scheduled(fixedDelay = 60000)
    public synchronized void purgeExpired() {
        long now = ticker.getAsLong();
        windows.values().removeIf(window -> now - window.deadline >= 0);
    }

    private static final class Window {
        final long deadline;
        long count;

        Window(long deadline, long count) {
            this.deadline = deadline;
            this.count = count;
        }
    }
}
