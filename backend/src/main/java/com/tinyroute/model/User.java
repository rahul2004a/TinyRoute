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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "email_normalized", nullable = false, unique = true, length = 254)
    private String emailNormalized;

    @Column(name = "token_version", nullable = false)
    private int tokenVersion;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected User() {
    }

    private User(String emailNormalized) {
        this.emailNormalized = Objects.requireNonNull(emailNormalized);
    }

    public static User create(String emailNormalized) {
        return new User(emailNormalized);
    }

    public UUID id() {
        return id;
    }

    public String emailNormalized() {
        return emailNormalized;
    }

    public int tokenVersion() {
        return tokenVersion;
    }

    public Instant deletedAt() {
        return deletedAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant updatedAt() {
        return updatedAt;
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
