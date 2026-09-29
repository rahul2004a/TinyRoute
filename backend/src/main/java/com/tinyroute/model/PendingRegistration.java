package com.tinyroute.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "pending_registrations")
public class PendingRegistration {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "email_normalized", nullable = false, unique = true, length = 254)
    private String emailNormalized;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    @Column(name = "otp_expires_at", nullable = false)
    private Instant otpExpiresAt;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(name = "resend_window_started_at", nullable = false)
    private Instant resendWindowStartedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PendingRegistration() {
    }

    private PendingRegistration(
            String tokenHash,
            String emailNormalized,
            String passwordHash,
            String otpHash,
            Instant otpExpiresAt,
            Instant now
    ) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.emailNormalized = Objects.requireNonNull(emailNormalized);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.otpHash = Objects.requireNonNull(otpHash);
        this.otpExpiresAt = Objects.requireNonNull(otpExpiresAt);
        this.resendWindowStartedAt = Objects.requireNonNull(now);
    }

    public static PendingRegistration create(
            String tokenHash,
            String emailNormalized,
            String passwordHash,
            String otpHash,
            Instant otpExpiresAt,
            Instant now
    ) {
        return new PendingRegistration(tokenHash, emailNormalized, passwordHash, otpHash, otpExpiresAt, now);
    }

    public void replace(String tokenHash, String passwordHash, String otpHash, Instant otpExpiresAt, Instant now) {
        this.tokenHash = Objects.requireNonNull(tokenHash);
        this.passwordHash = Objects.requireNonNull(passwordHash);
        this.otpHash = Objects.requireNonNull(otpHash);
        this.otpExpiresAt = Objects.requireNonNull(otpExpiresAt);
        this.failedAttempts = 0;
        this.resendCount = 0;
        this.resendWindowStartedAt = Objects.requireNonNull(now);
    }

    public UUID id() {
        return id;
    }

    public String tokenHash() {
        return tokenHash;
    }

    public String emailNormalized() {
        return emailNormalized;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public String otpHash() {
        return otpHash;
    }

    public Instant otpExpiresAt() {
        return otpExpiresAt;
    }

    public int failedAttempts() {
        return failedAttempts;
    }

    public int resendCount() {
        return resendCount;
    }

    public Instant resendWindowStartedAt() {
        return resendWindowStartedAt;
    }

    public void recordFailedAttempt() {
        if (failedAttempts < 5) {
            failedAttempts++;
        }
    }

    public boolean hasExhaustedAttempts() {
        return failedAttempts >= 5;
    }

    public void replaceOtp(String otpHash, Instant otpExpiresAt, Instant now) {
        this.otpHash = Objects.requireNonNull(otpHash);
        this.otpExpiresAt = Objects.requireNonNull(otpExpiresAt);
        this.failedAttempts = 0;
        if (!now.isBefore(resendWindowStartedAt.plusSeconds(3600))) {
            resendWindowStartedAt = now;
            resendCount = 0;
        }
        resendCount++;
    }

    @PrePersist
    void setCreationTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void setUpdatedTimestamp() {
        updatedAt = Instant.now();
    }
}
