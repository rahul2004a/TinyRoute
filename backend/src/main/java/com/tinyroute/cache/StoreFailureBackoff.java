package com.tinyroute.cache;

import java.time.Duration;
import java.util.function.LongSupplier;

/** Bounded recovery probe state only; contains no link or client data. */
public final class StoreFailureBackoff {
    private final long cooldownNanos;
    private final LongSupplier ticker;
    private boolean healthy = true;
    private boolean probing;
    private long retryAt;

    public StoreFailureBackoff(Duration cooldown, LongSupplier ticker) {
        if (cooldown.isNegative() || cooldown.isZero())
            throw new IllegalArgumentException("Positive cooldown required");
        this.cooldownNanos = cooldown.toNanos();
        this.ticker = ticker;
    }

    public synchronized boolean tryAcquire() {
        if (healthy) return true;
        if (probing || ticker.getAsLong() - retryAt < 0) return false;
        probing = true;
        return true;
    }

    public synchronized void recordSuccess() {
        healthy = true;
        probing = false;
    }

    public synchronized void recordFailure() {
        healthy = false;
        probing = false;
        retryAt = ticker.getAsLong() + cooldownNanos;
    }
}
