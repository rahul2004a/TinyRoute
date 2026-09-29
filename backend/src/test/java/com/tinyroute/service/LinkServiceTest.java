package com.tinyroute.service;

import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T00:00:00Z");

    @Test
    void tombstonesAnyNumberOfOwnedLinksWithOneBulkUpdateAndOneCleanupJob() {
        UUID userId = UUID.randomUUID();
        LinkRepository links = mock(LinkRepository.class);
        AccountDeletionCleanupRepository cleanups = mock(AccountDeletionCleanupRepository.class);
        when(links.tombstoneAllByOwnerId(userId, NOW)).thenReturn(100_000);

        new LinkService(links, cleanups, Clock.fixed(NOW, ZoneOffset.UTC))
                .tombstoneOwnedLinks(userId);

        ArgumentCaptor<AccountDeletionCleanup> cleanupCaptor =
                ArgumentCaptor.forClass(AccountDeletionCleanup.class);
        verify(links).tombstoneAllByOwnerId(userId, NOW);
        verify(cleanups).save(cleanupCaptor.capture());
        assertThat(cleanupCaptor.getValue().kind())
                .isEqualTo(AccountDeletionCleanup.Kind.REDIRECT_CACHE);
        assertThat(cleanupCaptor.getValue().userId()).isEqualTo(userId);
        assertThat(cleanupCaptor.getValue().redirectCursor()).isEmpty();
    }

    @Test
    void skipsCleanupJobWhenTheOwnerHasNoLinks() {
        UUID userId = UUID.randomUUID();
        LinkRepository links = mock(LinkRepository.class);
        AccountDeletionCleanupRepository cleanups = mock(AccountDeletionCleanupRepository.class);
        when(links.tombstoneAllByOwnerId(userId, NOW)).thenReturn(0);

        new LinkService(links, cleanups, Clock.fixed(NOW, ZoneOffset.UTC))
                .tombstoneOwnedLinks(userId);

        verify(cleanups, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
