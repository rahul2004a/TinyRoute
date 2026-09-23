package com.tinyroute.model;

import java.util.Objects;

public record PasswordResetRequested(String email, String token) {

    public PasswordResetRequested {
        Objects.requireNonNull(email);
        Objects.requireNonNull(token);
    }
}
