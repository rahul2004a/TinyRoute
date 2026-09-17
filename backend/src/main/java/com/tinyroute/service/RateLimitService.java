package com.tinyroute.service;

import com.tinyroute.cache.RateLimitStore;
import com.tinyroute.model.RateLimitAction;
import com.tinyroute.model.RateLimitCounter;
import com.tinyroute.model.RateLimitDecision;
import com.tinyroute.security.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class RateLimitService {

    private final RateLimitStore rateLimitStore;
    private final ClientAddressResolver clientAddressResolver;

    public RateLimitService(RateLimitStore rateLimitStore, ClientAddressResolver clientAddressResolver) {
        this.rateLimitStore = Objects.requireNonNull(rateLimitStore);
        this.clientAddressResolver = Objects.requireNonNull(clientAddressResolver);
    }

    public RateLimitDecision allowClient(RateLimitAction action, HttpServletRequest request) {
        String key = "rl:auth:" + action.keySegment() + ":" + clientAddressResolver.clientHash(request);
        RateLimitCounter counter = rateLimitStore.increment(key, action.window());
        return new RateLimitDecision(counter.count() <= action.maximumAttempts(), counter.retryAfter());
    }
}
