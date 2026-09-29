package com.tinyroute.exception;

import com.tinyroute.dto.error.ApiErrorResponse;
import com.tinyroute.security.RequestBodyTooLargeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.authenticationFailed(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(RefreshConcurrentException.class)
    public ResponseEntity<ApiErrorResponse> handleRefreshConcurrent() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiErrorResponse.refreshConcurrent(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimitExceeded(RateLimitExceededException exception) {
        long retryAfterSeconds = Math.max(1, exception.retryAfter().toSeconds());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, Long.toString(retryAfterSeconds))
                .body(ApiErrorResponse.rateLimited(UUID.randomUUID().toString(), retryAfterSeconds));
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceUnavailable() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiErrorResponse.serviceUnavailable(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(OtpInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpInvalid() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.otpInvalid(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleOtpExpired() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.otpExpired(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(ResetTokenInvalidException.class)
    public ResponseEntity<ApiErrorResponse> handleResetTokenInvalid() {
        return ResponseEntity.badRequest().body(ApiErrorResponse.resetTokenInvalid(UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            fieldErrors.putIfAbsent(fieldError.getField(), "Invalid value.");
        }
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.validationError(UUID.randomUUID().toString(), fieldErrors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedJson(HttpMessageNotReadableException exception) {
        if (wasCausedByRequestBodyLimit(exception)) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(ApiErrorResponse.requestBodyTooLarge(UUID.randomUUID().toString()));
        }
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.validationError(UUID.randomUUID().toString(), Map.of()));
    }

    private boolean wasCausedByRequestBodyLimit(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof RequestBodyTooLargeException) {
                return true;
            }
        }
        return false;
    }
}
