package com.tinyroute.controller;

import com.tinyroute.dto.PendingRegistrationResponse;
import com.tinyroute.dto.OtpVerificationRequest;
import com.tinyroute.dto.RegistrationRequest;
import com.tinyroute.dto.SessionResponse;
import com.tinyroute.model.AuthenticatedSession;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.service.AuthService;
import com.tinyroute.model.RateLimitAction;
import com.tinyroute.service.RateLimitService;
import com.tinyroute.exception.RateLimitExceededException;
import com.tinyroute.security.TokenHashing;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.web.csrf.CsrfTokenRepository;

import java.util.Objects;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final AuthCookieService authCookieService;
    private final CsrfTokenRepository csrfTokenRepository;

    public AuthController(
            AuthService authService,
            RateLimitService rateLimitService,
            AuthCookieService authCookieService,
            CsrfTokenRepository csrfTokenRepository) {
        this.authService = Objects.requireNonNull(authService);
        this.rateLimitService = Objects.requireNonNull(rateLimitService);
        this.authCookieService = Objects.requireNonNull(authCookieService);
        this.csrfTokenRepository = Objects.requireNonNull(csrfTokenRepository);
    }

    @PostMapping("/register")
    public ResponseEntity<PendingRegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request,
            HttpServletRequest servletRequest) {
        var decision = rateLimitService.allowClient(RateLimitAction.REGISTER, servletRequest);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfter());
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        authService.startRegistration(request.email(), request.password())
                .ifPresent(token -> response.header("Set-Cookie",
                        authCookieService.pendingRegistrationCookie(token).toString()));
        return response.body(PendingRegistrationResponse.pendingVerification());
    }

    @PostMapping("/register/verify")
    public ResponseEntity<SessionResponse> verifyRegistration(
            @CookieValue(AuthCookieService.PENDING_REGISTRATION_COOKIE_NAME) String pendingToken,
            @Valid @RequestBody OtpVerificationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        requireAllowed(rateLimitService.allowClient(RateLimitAction.OTP_VERIFY_CLIENT, servletRequest));
        requireAllowed(rateLimitService.allow(
                RateLimitAction.OTP_VERIFY_PENDING_REGISTRATION,
                TokenHashing.sha256(pendingToken)));
        AuthenticatedSession session = authService.verifyRegistration(pendingToken, request.otp());
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.status(201)
                .header("Set-Cookie", authCookieService.accessCookie(session.accessToken()).toString())
                .header("Set-Cookie", authCookieService.refreshCookie(session.refreshToken()).toString())
                .header("Set-Cookie", authCookieService.clearPendingRegistrationCookie().toString())
                .body(SessionResponse.authenticated(session.email()));
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<PendingRegistrationResponse> resendRegistrationOtp(
            @CookieValue(AuthCookieService.PENDING_REGISTRATION_COOKIE_NAME) String pendingToken
    ) {
        requireAllowed(rateLimitService.allow(
                RateLimitAction.OTP_RESEND_PENDING_REGISTRATION,
                TokenHashing.sha256(pendingToken)
        ));
        authService.resendRegistrationOtp(pendingToken);
        return ResponseEntity.accepted().body(PendingRegistrationResponse.pendingVerification());
    }

    private void requireAllowed(com.tinyroute.model.RateLimitDecision decision) {
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfter());
        }
    }
}
