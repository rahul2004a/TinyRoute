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
        return allow(action, clientAddressResolver.clientHash(request));
    }

    public RateLimitDecision allow(RateLimitAction action, String subjectHash) {
        String key = "rl:auth:" + action.keySegment() + ":" + Objects.requireNonNull(subjectHash);
        RateLimitCounter counter = rateLimitStore.increment(key, action.window());
        return new RateLimitDecision(counter.count() <= action.maximumAttempts(), counter.retryAfter());
    }
}
