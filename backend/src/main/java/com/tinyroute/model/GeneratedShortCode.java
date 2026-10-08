package com.tinyroute.model;

/** Internal allocation metadata; never an authorization credential (FR-CRE-04). */
public record GeneratedShortCode(ShortCode code, long generationValue) {
    public static final long MAX_VALUE = 218_340_105_584_895L;

    public GeneratedShortCode {
        if (code == null
                || !code.value().matches("[A-Za-z0-9]{8}")
                || generationValue < 1
                || generationValue > MAX_VALUE)
            throw new IllegalArgumentException("Generated short code is invalid");
    }
}
