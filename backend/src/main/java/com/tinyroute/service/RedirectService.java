package com.tinyroute.service;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.config.LinkProperties;
import com.tinyroute.model.*;
import com.tinyroute.model.RedirectOutcome.Kind;
import com.tinyroute.repository.LinkRepository;
import java.time.*;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.annotation.*;

@Service
public class RedirectService {
    private final LinkRepository links;
    private final RedirectCache cache;
    private final LinkProperties properties;
    private final Clock clock;

    public RedirectService(
            LinkRepository links, RedirectCache cache, LinkProperties properties, Clock clock) {
        this.links = links;
        this.cache = cache;
        this.properties = properties;
        this.clock = clock;
    }

    /** No old caller transaction/snapshot can be reused (NFR-REL-01; NFR-CON-02). */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RedirectOutcome resolve(ShortCode code) {
        Instant now;
        try {
            var snapshot = cache.get(code.value());
            now = clock.instant();
            if (snapshot.isPresent()
                    && snapshot.get().isUsableFor(code.value(), now, properties.shortHost()))
                return outcome(snapshot.get(), now);
        } catch (RuntimeException ignored) {
            /* PostgreSQL remains authoritative */
        }
        Instant readStart = clock.instant();
        final Optional<RedirectLinkState> result;
        try {
            result = links.findRedirectStateByCode(code.value());
        } catch (DataAccessException | TransactionException e) {
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        }
        if (result.isEmpty()) return new RedirectOutcome(Kind.NOT_FOUND, null);
        var state = result.get();
        now = clock.instant();
        if (!now.isBefore(readStart.plusSeconds(5)))
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        var status = state.deletedAt() != null ? LinkStatus.DELETED : state.status();
        Instant deadline = readStart.plusSeconds(5);
        if (state.expiresAt() != null && state.expiresAt().isBefore(deadline))
            deadline = state.expiresAt();
        var lookup =
                new RedirectLookup(
                        1,
                        state.code(),
                        status,
                        status == LinkStatus.ACTIVE ? state.destinationUrl() : null,
                        state.expiresAt(),
                        readStart,
                        deadline);
        if (!code.value().equals(state.code()))
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        var outcome = outcome(lookup, now);
        if (outcome.kind() != Kind.SERVICE_UNAVAILABLE
                && lookup.isUsableFor(code.value(), now, properties.shortHost())) {
            try {
                cache.put(lookup);
            } catch (RuntimeException ignored) {
                /* known database result still wins */
            }
        }
        Instant usedAt = clock.instant();
        if (!usedAt.isBefore(readStart.plusSeconds(5)))
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        return outcome(lookup, usedAt);
    }

    private RedirectOutcome outcome(RedirectLookup state, Instant now) {
        if (state.status() == LinkStatus.DELETED
                || (state.expiresAt() != null && !state.expiresAt().isAfter(now)))
            return new RedirectOutcome(Kind.NOT_FOUND, null);
        if (state.status() == LinkStatus.DISABLED)
            return new RedirectOutcome(Kind.UNAVAILABLE, null);
        if (state.status() != LinkStatus.ACTIVE)
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        try {
            return new RedirectOutcome(
                    Kind.REDIRECT,
                    DestinationUrl.parse(state.destinationUrl(), properties.shortHost()).value());
        } catch (IllegalArgumentException e) {
            return new RedirectOutcome(Kind.SERVICE_UNAVAILABLE, null);
        }
    }
}
