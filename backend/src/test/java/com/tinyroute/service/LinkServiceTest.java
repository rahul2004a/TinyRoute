package com.tinyroute.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.config.LinkProperties;
import com.tinyroute.exception.*;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.model.CreateLinkCommand;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LinkServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-25T00:00:00Z");

    @Test
    void tombstonesAnyNumberOfOwnedLinksWithOneBulkUpdateAndOneCleanupJob() {
        UUID userId = UUID.randomUUID();
        LinkRepository links = mock(LinkRepository.class);
        AccountDeletionCleanupRepository cleanups = mock(AccountDeletionCleanupRepository.class);
        when(links.tombstoneAllByOwnerId(userId, NOW)).thenReturn(100_000);

        service(links, cleanups).tombstoneOwnedLinks(userId);

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

        service(links, cleanups).tombstoneOwnedLinks(userId);

        verify(cleanups, never()).save(org.mockito.ArgumentMatchers.any());
    }

    private LinkService service(LinkRepository links, AccountDeletionCleanupRepository cleanups) {
        var p = new LinkProperties();
        p.setShortBaseUrl("https://go.test");
        return new LinkService(
                links,
                cleanups,
                mock(RedirectCache.class),
                new LinkCreationValidator(p),
                new ShortCodeGenerator(new java.security.SecureRandom()),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AccessToken principal() {
        return new AccessToken(UUID.randomUUID(), UUID.randomUUID(), NOW, NOW.plusSeconds(900), 0);
    }

    @Test
    void returnsInsertedValuesAndDoesNotOverwriteACustomAlias() {
        var links = mock(LinkRepository.class);
        var owner = principal();
        when(links.lockActiveOwnerTokenVersion(owner.userId())).thenReturn(Optional.of(0));
        when(links.insertIfCodeAvailable(
                        any(),
                        eq("Abc"),
                        eq(owner.userId()),
                        eq("https://example.com/a#b"),
                        eq(NOW),
                        isNull()))
                .thenReturn(1, 0);
        var service = service(links, mock(AccountDeletionCleanupRepository.class));
        var created =
                service.create(
                        owner, new CreateLinkCommand("https://example.com/a#b", "Abc", null));
        assertThat(created.code().value()).isEqualTo("Abc");
        assertThat(created.destinationUrl().value()).isEqualTo("https://example.com/a#b");
        assertThat(created.createdAt()).isEqualTo(NOW);
        assertThatThrownBy(
                        () ->
                                service.create(
                                        owner,
                                        new CreateLinkCommand(
                                                "https://example.com/a#b", "Abc", null)))
                .isInstanceOf(AliasUnavailableException.class);
    }

    @Test
    void boundsGeneratedAllocationAndRetriesOnlyConfirmedCodeConflicts() {
        var links = mock(LinkRepository.class);
        var owner = principal();
        when(links.lockActiveOwnerTokenVersion(owner.userId())).thenReturn(Optional.of(0));
        when(links.insertIfCodeAvailable(any(), anyString(), any(), anyString(), any(), isNull()))
                .thenReturn(0);
        assertThatThrownBy(
                        () ->
                                service(links, mock(AccountDeletionCleanupRepository.class))
                                        .create(
                                                owner,
                                                new CreateLinkCommand(
                                                        "https://example.com", null, null)))
                .isInstanceOf(CodeAllocationFailedException.class);
        verify(links, org.mockito.Mockito.times(10))
                .insertIfCodeAvailable(any(), anyString(), any(), anyString(), any(), isNull());
    }

    @Test
    void rejectsAnOwnerWhoseVersionChangedBeforeInsert() {
        var links = mock(LinkRepository.class);
        var owner = principal();
        when(links.lockActiveOwnerTokenVersion(owner.userId())).thenReturn(Optional.of(1));
        assertThatThrownBy(
                        () ->
                                service(links, mock(AccountDeletionCleanupRepository.class))
                                        .create(
                                                owner,
                                                new CreateLinkCommand(
                                                        "https://example.com", "Abc", null)))
                .isInstanceOf(AuthenticationFailedException.class);
        verify(links, never())
                .insertIfCodeAvailable(any(), anyString(), any(), anyString(), any(), any());
    }

    @Test
    void doesNotRetryAnUnrelatedDatastoreFailure() {
        var links = mock(LinkRepository.class);
        var owner = principal();
        when(links.lockActiveOwnerTokenVersion(owner.userId())).thenReturn(Optional.of(0));
        when(links.insertIfCodeAvailable(any(), anyString(), any(), anyString(), any(), isNull()))
                .thenThrow(
                        new org.springframework.dao.DataAccessResourceFailureException(
                                "private SQL"));
        assertThatThrownBy(
                        () ->
                                service(links, mock(AccountDeletionCleanupRepository.class))
                                        .create(
                                                owner,
                                                new CreateLinkCommand(
                                                        "https://example.com", null, null)))
                .isInstanceOf(ServiceUnavailableException.class);
        verify(links)
                .insertIfCodeAvailable(any(), anyString(), any(), anyString(), any(), isNull());
    }
}
