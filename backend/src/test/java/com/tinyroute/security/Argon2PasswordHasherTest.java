package com.tinyroute.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class Argon2PasswordHasherTest {

    private final PasswordHasher passwordHasher = new Argon2PasswordHasher();

    @Test
    void hashesPasswordsWithArgon2idAndASalt() {
        String firstHash = passwordHasher.hash("correct horse battery staple");
        String secondHash = passwordHasher.hash("correct horse battery staple");

        assertThat(firstHash).startsWith("$argon2id$");
        assertThat(secondHash).startsWith("$argon2id$");
        assertThat(firstHash).isNotEqualTo(secondHash);
    }

    @Test
    void matchesOnlyTheOriginalPassword() {
        String passwordHash = passwordHasher.hash("correct horse battery staple");

        assertThat(passwordHasher.matches("correct horse battery staple", passwordHash)).isTrue();
        assertThat(passwordHasher.matches("incorrect password", passwordHash)).isFalse();
    }

    @Test
    void rejectsAMalformedPasswordHashWithoutAuthenticating() {
        assertThat(passwordHasher.matches("correct horse battery staple", "$argon2id$malformed")).isFalse();
    }
}
