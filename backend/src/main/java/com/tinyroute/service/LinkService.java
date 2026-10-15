package com.tinyroute.service;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.exception.*;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.model.CreateLinkCommand;
import com.tinyroute.model.CreatedLink;
import com.tinyroute.model.GeneratedShortCode;
import com.tinyroute.model.LinkPage;
import com.tinyroute.model.LinkStatus;
import com.tinyroute.model.ShortCode;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class LinkService {
    private final LinkRepository linkRepository;
    private final AccountDeletionCleanupRepository cleanupRepository;
    private final Clock clock;
    private final RedirectCache redirectCache;
    private final LinkCreationValidator validator;
    private final ShortCodeGenerator generator;

    @Autowired
    public LinkService(
            LinkRepository linkRepository,
            AccountDeletionCleanupRepository cleanupRepository,
            RedirectCache redirectCache,
            LinkCreationValidator validator,
            ShortCodeGenerator generator,
            Clock clock) {
        this.linkRepository = linkRepository;
        this.cleanupRepository = cleanupRepository;
        this.clock = clock;
        this.redirectCache = redirectCache;
        this.validator = validator;
        this.generator = generator;
    }

    /** Owner lock precedes code insertion and is held through commit (FR-CRE-04; FR-ACC-05). */
    @Transactional
    public CreatedLink create(AccessToken principal, CreateLinkCommand command) {
        if (principal == null) throw new AuthenticationFailedException();
        try {
            int version =
                    linkRepository
                            .lockActiveOwnerTokenVersion(principal.userId())
                            .orElseThrow(AuthenticationFailedException::new);
            if (version != principal.tokenVersion()) throw new AuthenticationFailedException();
            var input = validator.validate(command, clock.instant());
            UUID id = UUID.randomUUID();
            boolean recoverNext = false;
            for (int candidate = 0; candidate < (input.alias() == null ? 10 : 1); candidate++) {
                GeneratedShortCode allocation =
                        input.alias() != null
                                ? null
                                : (recoverNext
                                        ? generator.nextCandidateAfterConflict()
                                        : generator.nextCandidate());
                recoverNext = false;
                String value =
                        input.alias() != null ? input.alias().value() : allocation.code().value();
                if (ShortCode.isReserved(value)) continue;
                ShortCode code = new ShortCode(value);
                Instant createdAt = clock.instant().truncatedTo(ChronoUnit.MILLIS);
                validator.requireFutureExpiry(input.expiresAt(), clock.instant());
                if (linkRepository.insertIfCodeAvailable(
                                id,
                                value,
                                principal.userId(),
                                input.destinationUrl().value(),
                                createdAt,
                                input.expiresAt(),
                                allocation == null ? null : allocation.generationValue())
                        == 1) {
                    if (TransactionSynchronizationManager.isSynchronizationActive()) {
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        try {
                                            redirectCache.evict(value);
                                        } catch (RuntimeException ignored) {
                                            /* committed creation remains successful */
                                        }
                                    }
                                });
                    }
                    return new CreatedLink(
                            id, code, input.destinationUrl(), createdAt, input.expiresAt());
                }
                if (input.alias() != null) throw new AliasUnavailableException();
                recoverNext = true;
            }
            throw new CodeAllocationFailedException();
        } catch (DataAccessException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    /** Cursor positions never replace the owner filter (FR-MGT-01/02, NFR-SEC-04). */
    @Transactional(readOnly = true)
    public LinkPage list(AccessToken principal, int size, String encodedCursor) {
        if (principal == null) throw new AuthenticationFailedException();
        if (size < 1 || size > 50) {
            throw new LinkValidationException("size", "Choose a page size between 1 and 50.");
        }
        Cursor cursor = decodeCursor(encodedCursor);
        var rows =
                linkRepository.findPageByOwnerId(
                        principal.userId(),
                        cursor == null ? null : cursor.createdAt(),
                        cursor == null ? null : cursor.id(),
                        size + 1);
        Instant now = clock.instant();
        var items =
                rows.stream()
                        .limit(size)
                        .map(
                                link ->
                                        new LinkPage.Item(
                                                link.id(),
                                                new ShortCode(link.code()),
                                                link.destinationUrl(),
                                                link.createdAt(),
                                                link.expiresAt(),
                                                link.status() == LinkStatus.ACTIVE
                                                                && link.expiresAt() != null
                                                                && !now.isBefore(link.expiresAt())
                                                        ? "EXPIRED"
                                                        : link.status().name(),
                                                link.clickCount()))
                        .toList();
        String nextCursor = null;
        if (rows.size() > size) {
            var last = items.getLast();
            nextCursor =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    (last.createdAt() + "|" + last.id())
                                            .getBytes(StandardCharsets.UTF_8));
        }
        return new LinkPage(items, nextCursor);
    }

    private Cursor decodeCursor(String encoded) {
        if (encoded == null) return null;
        try {
            if (encoded.isEmpty() || encoded.length() > 256) throw new IllegalArgumentException();
            String[] parts =
                    new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8)
                            .split("\\|", -1);
            if (parts.length != 2) throw new IllegalArgumentException();
            Instant createdAt = Instant.parse(parts[0]);
            int year = createdAt.atOffset(ZoneOffset.UTC).getYear();
            if (year < 1 || year > 9999) throw new IllegalArgumentException();
            return new Cursor(createdAt, UUID.fromString(parts[1]));
        } catch (IllegalArgumentException | DateTimeException invalid) {
            throw new LinkValidationException(
                    "cursor", "This page cursor is invalid. Start from the first page.");
        }
    }

    private record Cursor(Instant createdAt, UUID id) {}

    @Transactional
    public void tombstoneOwnedLinks(UUID userId) {
        Instant now = clock.instant();
        int tombstonedLinks = linkRepository.tombstoneAllByOwnerId(userId, now);
        if (tombstonedLinks > 0) {
            cleanupRepository.save(AccountDeletionCleanup.redirectBatch(userId, now));
        }
    }
}
