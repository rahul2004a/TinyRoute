package com.tinyroute.service;

import com.tinyroute.cache.RedirectCache;
import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import com.tinyroute.repository.LinkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class AccountDeletionRetryJob {
    private static final int REDIRECT_BATCH_SIZE = 100;
    private static final Logger log = LoggerFactory.getLogger(AccountDeletionRetryJob.class);

    private final AccountDeletionCleanupRepository cleanupRepository;
    private final LinkRepository linkRepository;
    private final RefreshSessionStore refreshSessionStore;
    private final RedirectCache redirectCache;
    private final Clock clock;

    @Autowired
    public AccountDeletionRetryJob(AccountDeletionCleanupRepository cleanupRepository,
                                   LinkRepository linkRepository, RefreshSessionStore refreshSessionStore,
                                   RedirectCache redirectCache) {
        this(cleanupRepository, linkRepository, refreshSessionStore, redirectCache, Clock.systemUTC());
    }

    AccountDeletionRetryJob(AccountDeletionCleanupRepository cleanupRepository,
                            LinkRepository linkRepository, RefreshSessionStore refreshSessionStore,
                            RedirectCache redirectCache, Clock clock) {
        this.cleanupRepository = cleanupRepository;
        this.linkRepository = linkRepository;
        this.refreshSessionStore = refreshSessionStore;
        this.redirectCache = redirectCache;
        this.clock = clock;
    }

    @Scheduled(fixedDelay = 5000)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processPending() {
        for (AccountDeletionCleanup cleanup : cleanupRepository
                .findTop100ByNextAttemptAtLessThanEqualOrderByCreatedAtAsc(clock.instant())) {
            try {
                boolean completed;
                if (cleanup.kind() == AccountDeletionCleanup.Kind.SESSION) {
                    refreshSessionStore.deleteAllByUserId(cleanup.userId());
                    completed = true;
                } else {
                    completed = processRedirectBatch(cleanup);
                }
                if (completed) {
                    cleanupRepository.delete(cleanup);
                }
            } catch (RuntimeException exception) {
                cleanup.retryAfterFailure(clock.instant());
                if (cleanup.attempts() % 10 == 0) {
                    log.error("Account deletion cleanup needs intervention: kind={}, attempts={}",
                            cleanup.kind(), cleanup.attempts(), exception);
                } else {
                    log.warn("Account deletion cleanup will retry: kind={}, attempts={}",
                            cleanup.kind(), cleanup.attempts(), exception);
                }
            }
        }
    }

    private boolean processRedirectBatch(AccountDeletionCleanup cleanup) {
        var codes = linkRepository.findDeletionCleanupCodes(cleanup.userId(), cleanup.redirectCursor());
        for (String code : codes) {
            redirectCache.evict(code);
        }
        if (codes.size() < REDIRECT_BATCH_SIZE) {
            return true;
        }
        cleanup.advanceRedirectCursor(codes.getLast());
        return false;
    }
}
