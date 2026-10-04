package com.tinyroute.controller;

import com.tinyroute.config.GoogleOAuthProperties;
import com.tinyroute.dto.GenericAcceptedResponse;
import com.tinyroute.dto.LoginRequest;
import com.tinyroute.dto.OtpVerificationRequest;
import com.tinyroute.dto.PasswordResetConfirmationRequest;
import com.tinyroute.dto.PasswordResetRequest;
import com.tinyroute.dto.PendingRegistrationResponse;
import com.tinyroute.dto.RegistrationRequest;
import com.tinyroute.dto.SessionResponse;
import com.tinyroute.exception.AuthenticationFailedException;
import com.tinyroute.exception.OAuthFailedException;
import com.tinyroute.exception.OtpInvalidException;
import com.tinyroute.exception.RateLimitExceededException;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AuthenticatedSession;
import com.tinyroute.model.OAuthAuthorization;
import com.tinyroute.model.RateLimitAction;
import com.tinyroute.model.RateLimitDecision;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.security.TokenHashing;
import com.tinyroute.service.AuthService;
import com.tinyroute.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimitService rateLimitService;
    private final AuthCookieService authCookieService;
    private final CsrfTokenRepository csrfTokenRepository;
    private final GoogleOAuthProperties googleOAuthProperties;

    public AuthController(
            AuthService authService,
            RateLimitService rateLimitService,
            AuthCookieService authCookieService,
            CsrfTokenRepository csrfTokenRepository,
            GoogleOAuthProperties googleOAuthProperties) {
        this.authService = Objects.requireNonNull(authService);
        this.rateLimitService = Objects.requireNonNull(rateLimitService);
        this.authCookieService = Objects.requireNonNull(authCookieService);
        this.csrfTokenRepository = Objects.requireNonNull(csrfTokenRepository);
        this.googleOAuthProperties = Objects.requireNonNull(googleOAuthProperties);
    }

    @PostMapping("/register")
    public ResponseEntity<PendingRegistrationResponse> register(
            @Valid @RequestBody RegistrationRequest request, HttpServletRequest servletRequest) {
        var decision = rateLimitService.allowClient(RateLimitAction.REGISTER, servletRequest);
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfter());
        }

        ResponseEntity.BodyBuilder response = ResponseEntity.accepted();
        authService
                .startRegistration(request.email(), request.password())
                .ifPresent(
                        token ->
                                response.header(
                                        "Set-Cookie",
                                        authCookieService
                                                .pendingRegistrationCookie(token)
                                                .toString()));
        return response.body(PendingRegistrationResponse.pendingVerification());
    }

    @PostMapping("/register/verify")
    public ResponseEntity<SessionResponse> verifyRegistration(
            @CookieValue(
                            value = AuthCookieService.PENDING_REGISTRATION_COOKIE_NAME,
                            required = false)
                    String pendingToken,
            @Valid @RequestBody OtpVerificationRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        String verifiedPendingToken = requirePendingRegistrationToken(pendingToken);
        requireAllowed(
                rateLimitService.allowClient(RateLimitAction.OTP_VERIFY_CLIENT, servletRequest));
        requireAllowed(
                rateLimitService.allow(
                        RateLimitAction.OTP_VERIFY_PENDING_REGISTRATION,
                        TokenHashing.sha256(verifiedPendingToken)));
        AuthenticatedSession session =
                authService.verifyRegistration(verifiedPendingToken, request.otp());
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.status(201)
                .header(
                        "Set-Cookie",
                        authCookieService.accessCookie(session.accessToken()).toString())
                .header(
                        "Set-Cookie",
                        authCookieService.refreshCookie(session.refreshToken()).toString())
                .header("Set-Cookie", authCookieService.clearPendingRegistrationCookie().toString())
                .body(SessionResponse.authenticated(session.email()));
    }

    @PostMapping("/register/resend-otp")
    public ResponseEntity<PendingRegistrationResponse> resendRegistrationOtp(
            @CookieValue(
                            value = AuthCookieService.PENDING_REGISTRATION_COOKIE_NAME,
                            required = false)
                    String pendingToken,
            HttpServletRequest servletRequest) {
        String verifiedPendingToken = requirePendingRegistrationToken(pendingToken);
        requireAllowed(
                rateLimitService.allowClient(RateLimitAction.OTP_RESEND_CLIENT, servletRequest));
        requireAllowed(
                rateLimitService.allow(
                        RateLimitAction.OTP_RESEND_PENDING_REGISTRATION,
                        TokenHashing.sha256(verifiedPendingToken)));
        authService.resendRegistrationOtp(verifiedPendingToken);
        return ResponseEntity.accepted().body(PendingRegistrationResponse.pendingVerification());
    }

    @PostMapping("/login")
    public ResponseEntity<SessionResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        requireAllowed(
                rateLimitService.allowClient(RateLimitAction.PASSWORD_LOGIN, servletRequest));
        AuthenticatedSession session = authService.login(request.email(), request.password());
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.ok()
                .header(
                        "Set-Cookie",
                        authCookieService.accessCookie(session.accessToken()).toString())
                .header(
                        "Set-Cookie",
                        authCookieService.refreshCookie(session.refreshToken()).toString())
                .body(SessionResponse.authenticated(session.email()));
    }

    @GetMapping("/me")
    public SessionResponse currentSession(@AuthenticationPrincipal AccessToken accessToken) {
        return SessionResponse.authenticated(authService.currentSessionEmail(accessToken));
    }

    @GetMapping("/google/start")
    public ResponseEntity<Void> startGoogleAuthorization(HttpServletRequest servletRequest) {
        requireAllowed(rateLimitService.allowClient(RateLimitAction.GOOGLE_START, servletRequest));
        OAuthAuthorization authorization = authService.startGoogleAuthorization();
        return ResponseEntity.status(302)
                .location(authorization.authorizationUri())
                .header(
                        "Set-Cookie",
                        authCookieService.oauthStateCookie(authorization.state()).toString())
                .build();
    }

    @GetMapping("/google/callback")
    public ResponseEntity<Void> googleCallback(
            @CookieValue(value = AuthCookieService.OAUTH_STATE_COOKIE_NAME, required = false)
                    String stateCookie,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String state,
            @org.springframework.web.bind.annotation.RequestParam(required = false) String code,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        try {
            authService.validateGoogleCallback(stateCookie, state, code);
            AuthenticatedSession session = authService.finishGoogleAuthorization(state, code);
            csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
            return ResponseEntity.status(303)
                    .location(googleOAuthProperties.successEndpoint())
                    .header(
                            "Set-Cookie",
                            authCookieService.accessCookie(session.accessToken()).toString())
                    .header(
                            "Set-Cookie",
                            authCookieService.refreshCookie(session.refreshToken()).toString())
                    .header("Set-Cookie", authCookieService.clearOauthStateCookie().toString())
                    .build();
        } catch (OAuthFailedException exception) {
            return oauthFailureRedirect();
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<SessionResponse> refresh(
            @CookieValue(value = AuthCookieService.REFRESH_COOKIE_NAME, required = false)
                    String refreshToken) {
        String verifiedRefreshToken = requireRefreshToken(refreshToken);
        requireAllowed(
                rateLimitService.allow(
                        RateLimitAction.REFRESH_SESSION_FAMILY,
                        authService.refreshRateLimitSubject(verifiedRefreshToken)));
        AuthenticatedSession session = authService.refresh(verifiedRefreshToken);
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header(
                        "Set-Cookie",
                        authCookieService.accessCookie(session.accessToken()).toString())
                .header(
                        "Set-Cookie",
                        authCookieService.refreshCookie(session.refreshToken()).toString())
                .body(SessionResponse.authenticated(session.email()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal AccessToken accessToken,
            @CookieValue(value = AuthCookieService.REFRESH_COOKIE_NAME, required = false)
                    String refreshToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        authService.logout(accessToken, refreshToken);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .header("Set-Cookie", authCookieService.clearAccessCookie().toString())
                .header("Set-Cookie", authCookieService.clearRefreshCookie().toString())
                .build();
    }

    @DeleteMapping("/account")
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal AccessToken accessToken,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse) {
        authService.deleteAccount(accessToken);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .header("Set-Cookie", authCookieService.clearAccessCookie().toString())
                .header("Set-Cookie", authCookieService.clearRefreshCookie().toString())
                .build();
    }

    @PostMapping("/password-reset")
    public ResponseEntity<GenericAcceptedResponse> requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request, HttpServletRequest servletRequest) {
        requireAllowed(
                rateLimitService.allowClient(
                        RateLimitAction.PASSWORD_RESET_REQUEST, servletRequest));
        authService.requestPasswordReset(request.email());
        return ResponseEntity.accepted().body(GenericAcceptedResponse.accepted());
    }

    @PostMapping("/password-reset/confirm")
    public ResponseEntity<Void> confirmPasswordReset(
            @Valid @RequestBody PasswordResetConfirmationRequest request,
            HttpServletRequest servletRequest) {
        requireAllowed(
                rateLimitService.allowClient(RateLimitAction.RESET_CONFIRM_CLIENT, servletRequest));
        requireAllowed(
                rateLimitService.allow(
                        RateLimitAction.RESET_CONFIRM_TOKEN, TokenHashing.sha256(request.token())));
        authService.confirmPasswordReset(request.token(), request.newPassword());
        return ResponseEntity.noContent().build();
    }

    private void requireAllowed(RateLimitDecision decision) {
        if (!decision.allowed()) {
            throw new RateLimitExceededException(decision.retryAfter());
        }
    }

    private String requirePendingRegistrationToken(String pendingToken) {
        if (pendingToken == null || pendingToken.isBlank()) {
            throw new OtpInvalidException();
        }
        return pendingToken;
    }

    private String requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new AuthenticationFailedException();
        }
        return refreshToken;
    }

    private ResponseEntity<Void> oauthFailureRedirect() {
        return ResponseEntity.status(303)
                .location(googleOAuthProperties.failureEndpoint())
                .header("Set-Cookie", authCookieService.clearOauthStateCookie().toString())
                .build();
    }
}
