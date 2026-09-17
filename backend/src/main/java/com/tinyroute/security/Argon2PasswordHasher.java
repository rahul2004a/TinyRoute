package com.tinyroute.security;

import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Objects;
import java.util.regex.Pattern;

public final class Argon2PasswordHasher implements PasswordHasher {

    private static final Pattern ARGON2ID_HASH = Pattern.compile(
            "\\$argon2id\\$v=\\d+\\$m=\\d+,t=\\d+,p=\\d+\\$[A-Za-z0-9+/]+\\$[A-Za-z0-9+/]+"
    );

    private final PasswordEncoder passwordEncoder = Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8();

    @Override
    public String hash(String password) {
        return passwordEncoder.encode(Objects.requireNonNull(password));
    }

    @Override
    public boolean matches(String password, String passwordHash) {
        if (password == null || passwordHash == null || !ARGON2ID_HASH.matcher(passwordHash).matches()) {
            return false;
        }
        return passwordEncoder.matches(password, passwordHash);
    }
}
