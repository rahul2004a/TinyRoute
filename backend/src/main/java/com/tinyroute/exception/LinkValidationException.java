package com.tinyroute.exception;

import java.util.Map;

/** Safe, allowlisted creation field errors (FR-CRE-03/06/07/08). */
public class LinkValidationException extends IllegalArgumentException {
    private final Map<String, String> fieldErrors;

    public LinkValidationException(String field, String message) {
        super("Invalid link input");
        fieldErrors = Map.of(field, message);
    }

    public Map<String, String> fieldErrors() {
        return fieldErrors;
    }
}
