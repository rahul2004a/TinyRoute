package com.tinyroute.exception;

import com.tinyroute.dto.error.ApiErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.UUID;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidAccessTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidAccessToken() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiErrorResponse.authenticationFailed(UUID.randomUUID().toString()));
    }
}
