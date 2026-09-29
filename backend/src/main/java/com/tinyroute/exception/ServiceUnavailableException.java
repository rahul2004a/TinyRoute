package com.tinyroute.exception;

public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException() {
        super();
    }

    public ServiceUnavailableException(Throwable cause) {
        super(cause);
    }
}
