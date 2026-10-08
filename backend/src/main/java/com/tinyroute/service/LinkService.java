package com.tinyroute.service;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.exception.*;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.model.CreateLinkCommand;
import com.tinyroute.model.CreatedLink;
import com.tinyroute.model.ShortCode;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
            for (int candidate = 0; candidate < (input.alias() == null ? 10 : 1); candidate++) {
                String value =
                        input.alias() == null ? generator.nextCandidate() : input.alias().value();
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
                                input.expiresAt())
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
            }
            throw new CodeAllocationFailedException();
        } catch (DataAccessException exception) {
            throw new ServiceUnavailableException(exception);
        }
    }

    @Transactional
    public void tombstoneOwnedLinks(UUID userId) {
        Instant now = clock.instant();
        int tombstonedLinks = linkRepository.tombstoneAllByOwnerId(userId, now);
        if (tombstonedLinks > 0) {
            cleanupRepository.save(AccountDeletionCleanup.redirectBatch(userId, now));
        }
    }
}
