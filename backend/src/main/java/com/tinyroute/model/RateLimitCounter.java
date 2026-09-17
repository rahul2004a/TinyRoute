package com.tinyroute.model;

import java.time.Duration;

public record RateLimitCounter(long count, Duration retryAfter) {
}
