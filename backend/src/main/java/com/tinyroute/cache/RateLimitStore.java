package com.tinyroute.cache;

import com.tinyroute.model.RateLimitCounter;

import java.time.Duration;

public interface RateLimitStore {

    RateLimitCounter increment(String key, Duration window);
}
