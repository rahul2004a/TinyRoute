package com.tinyroute.service;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tinyroute.cache.RedirectCache;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

class AccountDeletionRetryJobTest {
    @Test
    void retainsFailedCacheEvictionForRetryWithoutDiscardingSessionCleanup() {
        Instant now = Instant.parse("2026-09-25T00:00:00Z");
        UUID userId = UUID.randomUUID();
        AccountDeletionCleanup redirect = AccountDeletionCleanup.redirectBatch(userId, now);
        AccountDeletionCleanup session = AccountDeletionCleanup.session(userId, now);
        AccountDeletionCleanupRepository repository = mock(AccountDeletionCleanupRepository.class);
        LinkRepository links = mock(LinkRepository.class);
        RedirectCache cache = mock(RedirectCache.class);
        RefreshSessionStore sessions = mock(RefreshSessionStore.class);
        when(repository.findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now))
                .thenReturn(List.of(redirect, session));
        when(links.findDeletionCleanupCodes(userId, "")).thenReturn(List.of("owned"));
        doThrow(new IllegalStateException("Redis unavailable")).when(cache).evict("owned");

        new AccountDeletionRetryJob(repository, links, sessions, cache, Clock.fixed(now, ZoneOffset.UTC))
                .processPending();

        assertThat(redirect.attempts()).isEqualTo(1);
        verify(repository).delete(session);
        verify(sessions).deleteAllByUserId(userId);
        verify(repository, never()).delete(redirect);
    }

    @Test
    void evictsAtMostOneHundredRedirectsAndAdvancesTheCursor() {
        Instant now = Instant.parse("2026-09-25T00:00:00Z");
        UUID userId = UUID.randomUUID();
        AccountDeletionCleanup redirect = AccountDeletionCleanup.redirectBatch(userId, now);
        AccountDeletionCleanupRepository repository = mock(AccountDeletionCleanupRepository.class);
        LinkRepository links = mock(LinkRepository.class);
        RedirectCache cache = mock(RedirectCache.class);
        RefreshSessionStore sessions = mock(RefreshSessionStore.class);
        List<String> codes = java.util.stream.IntStream.range(0, 100)
                .mapToObj(index -> "code-%03d".formatted(index))
                .toList();
        when(repository.findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now))
                .thenReturn(List.of(redirect));
        when(links.findDeletionCleanupCodes(userId, "")).thenReturn(codes);

        new AccountDeletionRetryJob(repository, links, sessions, cache, Clock.fixed(now, ZoneOffset.UTC))
                .processPending();

        codes.forEach(code -> verify(cache).evict(code));
        assertThat(redirect.redirectCursor()).isEqualTo("code-099");
        verify(repository, never()).delete(redirect);
    }

    @Test
    void deletesRedirectCleanupAfterTheFinalPartialBatch() {
        Instant now = Instant.parse("2026-09-25T00:00:00Z");
        UUID userId = UUID.randomUUID();
        AccountDeletionCleanup redirect = AccountDeletionCleanup.redirectBatch(userId, now);
        AccountDeletionCleanupRepository repository = mock(AccountDeletionCleanupRepository.class);
        LinkRepository links = mock(LinkRepository.class);
        RedirectCache cache = mock(RedirectCache.class);
        RefreshSessionStore sessions = mock(RefreshSessionStore.class);
        when(repository.findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now))
                .thenReturn(List.of(redirect));
        when(links.findDeletionCleanupCodes(userId, "")).thenReturn(List.of("a", "b"));

        new AccountDeletionRetryJob(repository, links, sessions, cache, Clock.fixed(now, ZoneOffset.UTC))
                .processPending();

        verify(cache).evict("a");
        verify(cache).evict("b");
        verify(repository).delete(redirect);
    }

    @Test
    void retryLogDoesNotExposeTheRawUserId() {
        Instant now = Instant.parse("2026-09-25T00:00:00Z");
        UUID userId = UUID.randomUUID();
        AccountDeletionCleanup session = AccountDeletionCleanup.session(userId, now);
        AccountDeletionCleanupRepository repository = mock(AccountDeletionCleanupRepository.class);
        LinkRepository links = mock(LinkRepository.class);
        RedirectCache cache = mock(RedirectCache.class);
        RefreshSessionStore sessions = mock(RefreshSessionStore.class);
        when(repository.findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now))
                .thenReturn(List.of(session));
        doThrow(new IllegalStateException("Redis unavailable"))
                .when(sessions).deleteAllByUserId(userId);
        ch.qos.logback.classic.Logger logger =
                (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(AccountDeletionRetryJob.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            new AccountDeletionRetryJob(repository, links, sessions, cache,
                    Clock.fixed(now, ZoneOffset.UTC)).processPending();
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .noneMatch(message -> message.contains(userId.toString()));
    }
}
