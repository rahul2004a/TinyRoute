package com.tinyroute.service;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import org.junit.jupiter.api.Test;

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
        AccountDeletionCleanup redirect = AccountDeletionCleanup.redirect(userId, "owned", now);
        AccountDeletionCleanup session = AccountDeletionCleanup.session(userId, now);
        AccountDeletionCleanupRepository repository = mock(AccountDeletionCleanupRepository.class);
        RedirectCache cache = mock(RedirectCache.class);
        RefreshSessionStore sessions = mock(RefreshSessionStore.class);
        when(repository.findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(now))
                .thenReturn(List.of(redirect, session));
        doThrow(new IllegalStateException("Redis unavailable")).when(cache).evict("owned");

        new AccountDeletionRetryJob(repository, sessions, cache, Clock.fixed(now, ZoneOffset.UTC))
                .processPending();

        assertThat(redirect.attempts()).isEqualTo(1);
        verify(repository).delete(session);
        verify(sessions).deleteAllByUserId(userId);
        verify(repository, never()).delete(redirect);
    }
}
