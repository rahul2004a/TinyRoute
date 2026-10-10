package com.tinyroute.model;

public record RedirectOutcome(Kind kind, String destinationUrl) {
    public enum Kind {
        REDIRECT,
        NOT_FOUND,
        UNAVAILABLE,
        SERVICE_UNAVAILABLE
    }

    public RedirectOutcome {
        if (kind == null || (kind == Kind.REDIRECT) != (destinationUrl != null))
            throw new IllegalArgumentException("Invalid redirect outcome");
    }
}
