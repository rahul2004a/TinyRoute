package com.tinyroute.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "account_deletion_cleanup")
public class AccountDeletionCleanup {
    public enum Kind { SESSION, REDIRECT_CACHE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Kind kind;

    @Column(name = "redirect_code", length = 64)
    private String redirectCursor;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AccountDeletionCleanup() {
    }

    private AccountDeletionCleanup(UUID userId, Kind kind, String redirectCursor, Instant now) {
        this.userId = Objects.requireNonNull(userId);
        this.kind = Objects.requireNonNull(kind);
        this.redirectCursor = redirectCursor;
        this.nextAttemptAt = Objects.requireNonNull(now);
        this.createdAt = now;
    }

    public static AccountDeletionCleanup session(UUID userId, Instant now) {
        return new AccountDeletionCleanup(userId, Kind.SESSION, null, now);
    }

    public static AccountDeletionCleanup redirectBatch(UUID userId, Instant now) {
        return new AccountDeletionCleanup(userId, Kind.REDIRECT_CACHE, "", now);
    }

    public UUID userId() { return userId; }
    public Kind kind() { return kind; }
    public String redirectCursor() { return redirectCursor; }
    public int attempts() { return attempts; }

    public void advanceRedirectCursor(String cursor) {
        if (kind != Kind.REDIRECT_CACHE) {
            throw new IllegalStateException("Only redirect cleanup jobs have a cursor");
        }
        redirectCursor = Objects.requireNonNull(cursor);
    }

    public void retryAfterFailure(Instant now) {
        attempts++;
        nextAttemptAt = now.plus(Math.min(300, 1L << Math.min(attempts, 8)), ChronoUnit.SECONDS);
    }
}
