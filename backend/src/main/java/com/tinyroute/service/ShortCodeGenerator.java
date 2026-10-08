package com.tinyroute.service;

import java.security.SecureRandom;

public final class ShortCodeGenerator {
    private static final String ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private final SecureRandom random;

    public ShortCodeGenerator(SecureRandom random) {
        this.random = random;
    }

    public String nextCandidate() {
        StringBuilder code = new StringBuilder(8);
        for (int i = 0; i < 8; i++) code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        return code.toString();
    }
}
