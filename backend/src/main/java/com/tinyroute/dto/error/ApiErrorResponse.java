package com.tinyroute.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Objects;
import java.util.Map;

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

    public static ApiErrorResponse csrfInvalid(String requestId) {
        return new ApiErrorResponse(new Error(
                "CSRF_INVALID",
                "CSRF validation failed.",
                requestId
        ));
    }

    public static ApiErrorResponse sessionUnavailable(String requestId) {
        return new ApiErrorResponse(new Error(
                "SESSION_UNAVAILABLE",
                "Session validation is temporarily unavailable.",
                requestId
        ));
    }

    public static ApiErrorResponse rateLimited(String requestId, long retryAfterSeconds) {
        return new ApiErrorResponse(new Error(
                "RATE_LIMITED",
                "Too many attempts. Please try again later.",
                null,
                retryAfterSeconds,
                requestId
        ));
    }

    public static ApiErrorResponse otpInvalid(String requestId) {
        return new ApiErrorResponse(new Error("OTP_INVALID", "The verification code is invalid.", requestId));
    }

    public static ApiErrorResponse otpExpired(String requestId) {
        return new ApiErrorResponse(new Error("OTP_EXPIRED", "The verification code has expired.", requestId));
    }

    public static ApiErrorResponse validationError(String requestId, Map<String, String> fieldErrors) {
        return new ApiErrorResponse(new Error(
                "VALIDATION_ERROR",
                "The request is invalid.",
                fieldErrors,
                null,
                requestId
        ));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Error(
            String code,
            String message,
            Map<String, String> fieldErrors,
            Long retryAfterSeconds,
            String requestId
    ) {

        public Error(String code, String message, String requestId) {
            this(code, message, null, null, requestId);
        }

        public Error {
            Objects.requireNonNull(code, "code must not be null");
            Objects.requireNonNull(message, "message must not be null");
            Objects.requireNonNull(requestId, "requestId must not be null");
        }
    }
}
