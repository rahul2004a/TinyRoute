package com.tinyroute.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.exception.InvalidAccessTokenException;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.ObjectMapper;

class JwtAuthenticationFilterTest {

    private static final UUID USER_ID = UUID.fromString("a4d548f0-4c0d-45f1-8db3-c43445272b84");
    private static final UUID TOKEN_ID = UUID.fromString("6e3fa19d-1699-43a5-92a5-3ea06a471c7f");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void publicRedirectDoesNotConsultAnyAuthenticationDependency() throws Exception {
        var jwt = mock(JwtTokenService.class);
        var revocations = mock(JwtRevocationStore.class);
        var users = mock(UserRepository.class);
        var request = new MockHttpServletRequest("GET", "/Abc");
        request.setCookies(new Cookie("__Host-tinyroute_access", "invalid"));
        var reached = new java.util.concurrent.atomic.AtomicBoolean();
        filter(jwt, revocations, users)
                .doFilter(request, new MockHttpServletResponse(), (req, res) -> reached.set(true));
        assertThat(reached).isTrue();
        verifyNoInteractions(jwt, revocations, users);
    }

    @Test
    void authenticatesOnlyAnActiveUnrevokedAccessTokenForTheCurrentUserVersion() throws Exception {
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        JwtRevocationStore revocationStore = mock(JwtRevocationStore.class);
        UserRepository userRepository = mock(UserRepository.class);
        AccessToken accessToken = accessToken();
        User user = mock(User.class);
        when(jwtTokenService.verifyAccessToken("signed-access-token")).thenReturn(accessToken);
        when(revocationStore.isRevoked(TOKEN_ID)).thenReturn(false);
        when(userRepository.findById(USER_ID)).thenReturn(java.util.Optional.of(user));
        when(user.deletedAt()).thenReturn(null);
        when(user.tokenVersion()).thenReturn(3);
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter(jwtTokenService, revocationStore, userRepository)
                .doFilter(
                        requestWithAccessCookie(),
                        new MockHttpServletResponse(),
                        (request, response) ->
                                authentication.set(
                                        SecurityContextHolder.getContext().getAuthentication()));

        assertThat(authentication.get()).isNotNull();
        assertThat(authentication.get().getPrincipal()).isEqualTo(accessToken);
        assertThat(authentication.get().isAuthenticated()).isTrue();
    }

    @Test
    void leavesTheRequestUnauthenticatedWhenTheAccessTokenIsInvalidOrRevoked() throws Exception {
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        JwtRevocationStore revocationStore = mock(JwtRevocationStore.class);
        UserRepository userRepository = mock(UserRepository.class);
        when(jwtTokenService.verifyAccessToken("signed-access-token"))
                .thenThrow(new InvalidAccessTokenException());
        AtomicReference<Authentication> authentication = new AtomicReference<>();

        filter(jwtTokenService, revocationStore, userRepository)
                .doFilter(
                        requestWithAccessCookie(),
                        new MockHttpServletResponse(),
                        (request, response) ->
                                authentication.set(
                                        SecurityContextHolder.getContext().getAuthentication()));

        assertThat(authentication.get()).isNull();
    }

    @Test
    void failsClosedWhenTheRevocationStoreIsUnavailable() throws Exception {
        JwtTokenService jwtTokenService = mock(JwtTokenService.class);
        JwtRevocationStore revocationStore = mock(JwtRevocationStore.class);
        UserRepository userRepository = mock(UserRepository.class);
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(jwtTokenService.verifyAccessToken("signed-access-token")).thenReturn(accessToken());
        when(revocationStore.isRevoked(TOKEN_ID))
                .thenThrow(new IllegalStateException("Redis unavailable"));

        filter(jwtTokenService, revocationStore, userRepository)
                .doFilter(
                        requestWithAccessCookie(),
                        response,
                        (request, servletResponse) -> {
                            throw new AssertionError(
                                    "A failed revocation check must stop the request");
                        });

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("SESSION_UNAVAILABLE");
    }

    private AccessToken accessToken() {
        return new AccessToken(USER_ID, TOKEN_ID, Instant.now(), Instant.now().plusSeconds(60), 3);
    }

    private JwtAuthenticationFilter filter(
            JwtTokenService jwtTokenService,
            JwtRevocationStore revocationStore,
            UserRepository userRepository) {
        return new JwtAuthenticationFilter(
                jwtTokenService,
                revocationStore,
                userRepository,
                new SecurityErrorResponseWriter(new ObjectMapper()),
                new PublicRedirectRequestMatcher());
    }

    private MockHttpServletRequest requestWithAccessCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/me");
        request.setCookies(new Cookie("__Host-tinyroute_access", "signed-access-token"));
        return request;
    }
}
