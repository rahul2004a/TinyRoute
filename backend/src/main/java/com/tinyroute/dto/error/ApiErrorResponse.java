package com.tinyroute.dto.error;

import java.util.Objects;

public record ApiErrorResponse(Error error) {

    public ApiErrorResponse {
        Objects.requireNonNull(error, "error must not be null");
    }

    public static ApiErrorResponse authenticationFailed(String requestId) {
        return new ApiErrorResponse(new Error(
                "AUTHENTICATION_FAILED",
                "Authentication is invalid or expired.",
                requestId
        ));
    }

    public record Error(String code, String message, String requestId) {

        public Error {
            Objects.requireNonNull(code, "code must not be null");
            Objects.requireNonNull(message, "message must not be null");
            Objects.requireNonNull(requestId, "requestId must not be null");
        }
    }
}
