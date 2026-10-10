package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.config.LinkProperties;
import com.tinyroute.model.*;
import com.tinyroute.model.RedirectOutcome.Kind;
import com.tinyroute.repository.LinkRepository;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;

class RedirectServiceTest {
    static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    static final String DEST = "https://example.com/docs?q=java#setup";
    final LinkRepository links = mock(LinkRepository.class);
    final RedirectCache cache = mock(RedirectCache.class);
    final AtomicReference<Instant> now = new AtomicReference<>(NOW);
    final Clock clock =
            new Clock() {
                public ZoneId getZone() {
                    return ZoneOffset.UTC;
                }

                public Clock withZone(ZoneId zone) {
                    return this;
                }

                public Instant instant() {
                    return now.get();
                }
            };

    RedirectService service() {
        var p = new LinkProperties();
        p.setShortBaseUrl("https://go.test");
        return new RedirectService(links, cache, p, clock);
    }

    RedirectLookup active(Instant expiry) {
        return new RedirectLookup(
                1,
                "Abc",
                LinkStatus.ACTIVE,
                DEST,
                expiry,
                NOW,
                expiry == null ? NOW.plusSeconds(5) : expiry);
    }

    @Test
    void freshCachePreservesTheExactDestinationEvenDuringDatabaseFailure() {
        when(cache.get("Abc")).thenReturn(Optional.of(active(null)));
        when(links.findRedirectStateByCode("Abc"))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        var outcome = service().resolve(new ShortCode("Abc"));
        assertThat(outcome.kind()).isEqualTo(Kind.REDIRECT);
        assertThat(outcome.destinationUrl()).isEqualTo(DEST);
        verifyNoInteractions(links);
    }

    @Test
    void expiryAtTheExactInstantCannotUseAPopulatedCache() {
        var expiry = NOW.plusSeconds(2);
        when(cache.get("Abc")).thenReturn(Optional.of(active(expiry)));
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.ACTIVE, DEST, null, expiry)));
        var service = service();
        now.set(expiry.minusNanos(1));
        assertThat(service.resolve(new ShortCode("Abc")).kind()).isEqualTo(Kind.REDIRECT);
        now.set(expiry);
        assertThat(service.resolve(new ShortCode("Abc")).kind()).isEqualTo(Kind.NOT_FOUND);
        verify(cache, never()).put(any());
    }

    @Test
    void staleCacheCannotRescueAnUnknownDatabaseState() {
        when(cache.get("Abc")).thenReturn(Optional.of(active(null)));
        now.set(NOW.plusSeconds(5));
        when(links.findRedirectStateByCode("Abc"))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        var outcome = service().resolve(new ShortCode("Abc"));
        assertThat(outcome.kind()).isEqualTo(Kind.SERVICE_UNAVAILABLE);
        assertThat(outcome.destinationUrl()).isNull();
    }

    @Test
    void aCacheReadCrossingTheDeadlineIsNotAUsableSnapshot() {
        when(cache.get("Abc"))
                .thenAnswer(
                        inv -> {
                            now.set(NOW.plusSeconds(5));
                            return Optional.of(active(null));
                        });
        when(links.findRedirectStateByCode("Abc"))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        assertThat(service().resolve(new ShortCode("Abc")).kind())
                .isEqualTo(Kind.SERVICE_UNAVAILABLE);
    }

    @Test
    void cacheReadAndWriteFailuresStillUseAuthoritativePostgres() {
        when(cache.get("Abc")).thenThrow(new IllegalStateException("offline"));
        doThrow(new IllegalStateException("offline")).when(cache).put(any());
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState("Abc", LinkStatus.ACTIVE, DEST, null, null)));
        assertThat(service().resolve(new ShortCode("Abc")).destinationUrl()).isEqualTo(DEST);
    }

    @Test
    void expiryReachedWhileWritingTheCacheCannotReturnARedirect() {
        var expiry = NOW.plusSeconds(1);
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.ACTIVE, DEST, null, expiry)));
        doAnswer(
                        inv -> {
                            now.set(expiry);
                            return null;
                        })
                .when(cache)
                .put(any());
        assertThat(service().resolve(new ShortCode("Abc")).kind()).isEqualTo(Kind.NOT_FOUND);
    }

    @Test
    void unknownCodesAreNeverCached() {
        when(links.findRedirectStateByCode("abc")).thenReturn(Optional.empty());
        assertThat(service().resolve(new ShortCode("abc")).kind()).isEqualTo(Kind.NOT_FOUND);
        verify(cache, never()).put(any());
    }

    @Test
    void deletionMarkersAndExpiryTakePrecedenceOverDisabled() {
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.DISABLED, DEST, NOW, null)));
        assertThat(service().resolve(new ShortCode("Abc")).kind()).isEqualTo(Kind.NOT_FOUND);
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.DISABLED, DEST, null, NOW)));
        assertThat(service().resolve(new ShortCode("Abc")).kind()).isEqualTo(Kind.NOT_FOUND);
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.DISABLED, DEST, null, null)));
        var outcome = service().resolve(new ShortCode("Abc"));
        assertThat(outcome.kind()).isEqualTo(Kind.UNAVAILABLE);
        assertThat(outcome.destinationUrl()).isNull();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"", "http://example.com", "https://go.test/a", "https://example.com/a\n"})
    void incompleteOrInvalidAuthoritativeDestinationsFailClosed(String destination) {
        when(links.findRedirectStateByCode("Abc"))
                .thenReturn(
                        Optional.of(
                                new RedirectLinkState(
                                        "Abc", LinkStatus.ACTIVE, destination, null, null)));
        var outcome = service().resolve(new ShortCode("Abc"));
        assertThat(outcome.kind()).isEqualTo(Kind.SERVICE_UNAVAILABLE);
        assertThat(outcome.destinationUrl()).isNull();
    }

    @Test
    void delayedFillsRetainTheOriginalReadDeadline() {
        when(links.findRedirectStateByCode("Abc"))
                .thenAnswer(
                        inv -> {
                            now.set(NOW.plusSeconds(4));
                            return Optional.of(
                                    new RedirectLinkState(
                                            "Abc", LinkStatus.ACTIVE, DEST, null, null));
                        });
        service().resolve(new ShortCode("Abc"));
        var captured = org.mockito.ArgumentCaptor.forClass(RedirectLookup.class);
        verify(cache).put(captured.capture());
        assertThat(captured.getValue().loadedAt()).isEqualTo(NOW);
        assertThat(captured.getValue().validUntil()).isEqualTo(NOW.plusSeconds(5));
    }

    @Test
    void aReadThatOutlivesTheFreshnessBudgetFailsClosed() {
        when(links.findRedirectStateByCode("Abc"))
                .thenAnswer(
                        inv -> {
                            now.set(NOW.plusSeconds(6));
                            return Optional.of(
                                    new RedirectLinkState(
                                            "Abc", LinkStatus.ACTIVE, DEST, null, null));
                        });
        assertThat(service().resolve(new ShortCode("Abc")).kind())
                .isEqualTo(Kind.SERVICE_UNAVAILABLE);
        verify(cache, never()).put(any());
    }

    @Test
    void corruptedSnapshotsNeverAvoidTheAuthoritativeRead() {
        var bad =
                List.of(
                        new RedirectLookup(
                                2, "Abc", LinkStatus.ACTIVE, DEST, null, NOW, NOW.plusSeconds(5)),
                        new RedirectLookup(
                                1, "abc", LinkStatus.ACTIVE, DEST, null, NOW, NOW.plusSeconds(5)),
                        new RedirectLookup(
                                1,
                                "Abc",
                                LinkStatus.ACTIVE,
                                DEST,
                                null,
                                NOW.plusSeconds(1),
                                NOW.plusSeconds(2)),
                        new RedirectLookup(
                                1, "Abc", LinkStatus.ACTIVE, DEST, null, NOW, NOW.plusSeconds(6)),
                        new RedirectLookup(
                                1,
                                "Abc",
                                LinkStatus.ACTIVE,
                                DEST,
                                NOW.plusSeconds(1),
                                NOW,
                                NOW.plusSeconds(2)),
                        new RedirectLookup(
                                1, "Abc", LinkStatus.ACTIVE, null, null, NOW, NOW.plusSeconds(5)),
                        new RedirectLookup(
                                1,
                                "Abc",
                                LinkStatus.DISABLED,
                                DEST,
                                null,
                                NOW,
                                NOW.plusSeconds(5)));
        when(links.findRedirectStateByCode("Abc"))
                .thenThrow(new DataAccessResourceFailureException("offline"));
        for (var snapshot : bad) {
            when(cache.get("Abc")).thenReturn(Optional.of(snapshot));
            assertThat(service().resolve(new ShortCode("Abc")).kind())
                    .isEqualTo(Kind.SERVICE_UNAVAILABLE);
        }
    }
}
