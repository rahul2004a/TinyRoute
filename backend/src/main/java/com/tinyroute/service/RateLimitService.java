package com.tinyroute.service;

import com.tinyroute.cache.RateLimitStore;
import com.tinyroute.cache.StoreFailureBackoff;
import com.tinyroute.config.RateLimitProperties;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.model.RateLimitAction;
import com.tinyroute.model.RateLimitCounter;
import com.tinyroute.model.RateLimitDecision;
import com.tinyroute.security.ClientAddressResolver;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private final RateLimitStore rateLimitStore;
    private final ClientAddressResolver clientAddressResolver;
    private final RateLimitProperties properties;
    private final InMemoryRedirectRateLimiter fallback;
    private final StoreFailureBackoff redirectBackoff =
            new StoreFailureBackoff(Duration.ofSeconds(1), System::nanoTime);

    public RateLimitService(
            RateLimitStore rateLimitStore,
            ClientAddressResolver clientAddressResolver,
            RateLimitProperties properties,
            InMemoryRedirectRateLimiter fallback) {
        this.rateLimitStore = Objects.requireNonNull(rateLimitStore);
        this.clientAddressResolver = Objects.requireNonNull(clientAddressResolver);
        this.properties = properties;
        this.fallback = fallback;
    }

    public RateLimitDecision allowCreation(UUID userId) {
        try {
            var counter =
                    rateLimitStore.increment("rl:create:" + userId, properties.getCreationWindow());
            return new RateLimitDecision(
                    counter.count() <= properties.getCreationMaximumAttempts(),
                    counter.retryAfter());
        } catch (RuntimeException e) {
            throw new ServiceUnavailableException(e);
        }
    }

    public RateLimitDecision allowRedirect(HttpServletRequest request) {
        final String hash;
        try {
            hash = clientAddressResolver.clientHash(request);
        } catch (IllegalArgumentException e) {
            throw new ServiceUnavailableException(e);
        }
        if (redirectBackoff.tryAcquire()) {
            try {
                var counter =
                        rateLimitStore.increment(
                                "rl:redirect:" + hash, properties.getRedirectWindow());
                redirectBackoff.recordSuccess();
                return new RateLimitDecision(
                        counter.count() <= properties.getRedirectMaximumAttempts(),
                        counter.retryAfter());
            } catch (RuntimeException e) {
                redirectBackoff.recordFailure();
            }
        }
        return fallback.allow(hash);
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
            return new RateLimitDecision(
                    counter.count() <= requiredAction.maximumAttempts(), counter.retryAfter());
        } catch (RuntimeException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }
}
