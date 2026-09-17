package com.tinyroute.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "auth_identities")
public class AuthIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Column(nullable = false, length = 255)
    private String subject;

    @Column(name = "secret_hash", length = 255)
    private String secretHash;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected AuthIdentity() {
    }

    private AuthIdentity(User user, AuthProvider provider, String subject, String secretHash) {
        this.user = Objects.requireNonNull(user);
        this.provider = Objects.requireNonNull(provider);
        this.subject = Objects.requireNonNull(subject);
        this.secretHash = secretHash;
    }

    public static AuthIdentity password(User user, String subject, String secretHash) {
        return new AuthIdentity(user, AuthProvider.PASSWORD, subject, Objects.requireNonNull(secretHash));
    }

    public static AuthIdentity google(User user, String subject) {
        return new AuthIdentity(user, AuthProvider.GOOGLE, subject, null);
    }

    public UUID id() {
        return id;
    }

    public User user() {
        return user;
    }

    public AuthProvider provider() {
        return provider;
    }

    public String subject() {
        return subject;
    }

    public String secretHash() {
        return secretHash;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @PrePersist
    void setCreatedAt() {
        createdAt = Instant.now();
    }
}
