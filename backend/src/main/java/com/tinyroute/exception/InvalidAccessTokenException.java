package com.tinyroute.exception;

public final class InvalidAccessTokenException extends RuntimeException {

    public InvalidAccessTokenException() {
        super("Invalid access token");
    }
}
