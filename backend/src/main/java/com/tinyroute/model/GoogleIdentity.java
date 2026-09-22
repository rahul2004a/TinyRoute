package com.tinyroute.model;

import java.util.Objects;

public record GoogleIdentity(String subject, String email) {

    public GoogleIdentity {
        if (Objects.requireNonNull(subject, "subject must not be null").isBlank()) {
            throw new IllegalArgumentException("subject must not be blank");
        }
        if (Objects.requireNonNull(email, "email must not be null").isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
    }
}
