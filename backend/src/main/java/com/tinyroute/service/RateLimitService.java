package com.tinyroute.service;

import com.tinyroute.cache.RateLimitStore;
import com.tinyroute.exception.ServiceUnavailableException;
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
        RateLimitAction requiredAction = Objects.requireNonNull(action);
        String requiredSubjectHash = Objects.requireNonNull(subjectHash);
        String key = "rl:auth:" + requiredAction.keySegment() + ":" + requiredSubjectHash;
        try {
            RateLimitCounter counter = rateLimitStore.increment(key, requiredAction.window());
            return new RateLimitDecision(counter.count() <= requiredAction.maximumAttempts(), counter.retryAfter());
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }
}
