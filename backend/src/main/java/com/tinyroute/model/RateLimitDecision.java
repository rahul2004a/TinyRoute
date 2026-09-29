package com.tinyroute.model;

import java.time.Duration;

public record RateLimitDecision(boolean allowed, Duration retryAfter) {
}
