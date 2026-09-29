package com.tinyroute.model;

import java.net.URI;
import java.util.Objects;

public record OAuthAuthorization(String state, URI authorizationUri) {

    public OAuthAuthorization {
        if (Objects.requireNonNull(state, "state must not be null").isBlank()) {
            throw new IllegalArgumentException("state must not be blank");
        }
        Objects.requireNonNull(authorizationUri, "authorization URI must not be null");
    }
}
