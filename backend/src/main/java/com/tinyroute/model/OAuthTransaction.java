package com.tinyroute.model;

import java.util.Objects;

public record OAuthTransaction(String nonce, String pkceVerifier) {

    public OAuthTransaction {
        requireValue(nonce, "nonce");
        requireValue(pkceVerifier, "PKCE verifier");
    }

    private static void requireValue(String value, String name) {
        if (Objects.requireNonNull(value, name + " must not be null").isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
