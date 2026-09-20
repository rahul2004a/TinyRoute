package com.tinyroute.security;

import com.tinyroute.dto.error.ApiErrorResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import tools.jackson.databind.ObjectMapper;

@Component
public class SecurityErrorResponseWriter implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public SecurityErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authenticationException
    ) throws IOException {
        write(response, HttpStatus.UNAUTHORIZED, ApiErrorResponse.authenticationFailed(requestId()));
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException, ServletException {
        write(response, HttpStatus.FORBIDDEN, ApiErrorResponse.csrfInvalid(requestId()));
    }

    public void sessionUnavailable(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.SERVICE_UNAVAILABLE, ApiErrorResponse.sessionUnavailable(requestId()));
    }

    public void requestBodyTooLarge(HttpServletResponse response) throws IOException {
        write(response, HttpStatus.PAYLOAD_TOO_LARGE, ApiErrorResponse.requestBodyTooLarge(requestId()));
    }

    private void write(HttpServletResponse response, HttpStatus status, ApiErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private String requestId() {
        return UUID.randomUUID().toString();
    }
}
