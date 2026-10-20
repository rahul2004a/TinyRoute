package com.tinyroute.exception;

public class LinkNotFoundException extends RuntimeException {
    public LinkNotFoundException() {
        super("Link not found.");
    }
}
