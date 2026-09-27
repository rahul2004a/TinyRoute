package com.tinyroute.service;

import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class LinkService {
    private final LinkRepository linkRepository;
    private final AccountDeletionCleanupRepository cleanupRepository;
    private final Clock clock;

    @Autowired
    public LinkService(LinkRepository linkRepository, AccountDeletionCleanupRepository cleanupRepository) {
        this(linkRepository, cleanupRepository, Clock.systemUTC());
    }

    LinkService(LinkRepository linkRepository, AccountDeletionCleanupRepository cleanupRepository, Clock clock) {
        this.linkRepository = linkRepository;
        this.cleanupRepository = cleanupRepository;
        this.clock = clock;
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
