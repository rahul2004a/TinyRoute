package com.tinyroute.controller;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.config.JwtProperties;
import com.tinyroute.model.AccessToken;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.security.JwtTokenService;
import com.tinyroute.security.PasswordHasher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;

import java.util.concurrent.ThreadLocalRandom;
import java.security.KeyPair;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class PasswordLoginTest {

    private static final String PASSWORD = "valid-password-12";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthIdentityRepository authIdentityRepository;

    @Autowired
    private PasswordHasher passwordHasher;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Autowired
    private KeyPair testJwtKeyPair;

    @Autowired
    private JwtRevocationStore jwtRevocationStore;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final String clientAddress = "198.18."
            + ThreadLocalRandom.current().nextInt(1, 255)
            + "."
            + ThreadLocalRandom.current().nextInt(1, 255);

    @AfterEach
    void removeCreatedRecords() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "auth_identities", "users");
    }

    @Test
    void signsInAVerifiedPasswordUserAndIssuesDocumentedSessionCookies() throws Exception {
        createPasswordUser("user@example.com");

        MvcResult login = successfulLogin(" USER@example.com ", PASSWORD);

        assertThat(login.getResponse().getHeaders(HttpHeaders.SET_COOKIE))
                .allMatch(header -> !header.contains(PASSWORD));
        mockMvc.perform(get("/api/auth/me")
                        .cookie(login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("user@example.com"));
    }

    @Test
    void returnsTheSameGenericFailureForUnknownWrongAndUnavailablePasswordCredentials() throws Exception {
        createPasswordUser("known@example.com");
        userRepository.save(User.create("google-only@example.com"));

        assertGenericAuthenticationFailure(loginRequest("unknown@example.com", PASSWORD));
        assertGenericAuthenticationFailure(loginRequest("known@example.com", "incorrect-password"));
        assertGenericAuthenticationFailure(loginRequest("google-only@example.com", PASSWORD));
    }

    @Test
    void rateLimitsPasswordLoginAttemptsPerClient() throws Exception {
        for (int attempt = 0; attempt < 5; attempt++) {
            assertGenericAuthenticationFailure(loginRequest("unknown@example.com", PASSWORD));
        }

        MvcResult csrf = csrf();
        mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void rejectsStaleAndRevokedAccessTokensFromTheCurrentSessionEndpoint() throws Exception {
        User staleUser = createPasswordUser("stale@example.com");
        MvcResult staleLogin = successfulLogin("stale@example.com", PASSWORD);
        jdbcTemplate.update("update users set token_version = token_version + 1 where id = ?", staleUser.id());

        mockMvc.perform(get("/api/auth/me")
                        .cookie(staleLogin.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));

        createPasswordUser("revoked@example.com");
        MvcResult revokedLogin = successfulLogin("revoked@example.com", PASSWORD);
        Cookie accessCookie = revokedLogin.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME);
        AccessToken accessToken = jwtTokenService.verifyAccessToken(accessCookie.getValue());
        jwtRevocationStore.revoke(accessToken.tokenId(), accessToken.expiresAt());

        mockMvc.perform(get("/api/auth/me").cookie(accessCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsAnExpiredAccessTokenFromTheCurrentSessionEndpoint() throws Exception {
        User user = userRepository.save(User.create("expired@example.com"));

        mockMvc.perform(get("/api/auth/me")
                        .cookie(new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, expiredAccessToken(user))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
    }

    private User createPasswordUser(String email) {
        User user = userRepository.save(User.create(email));
        authIdentityRepository.save(AuthIdentity.password(user, email, passwordHasher.hash(PASSWORD)));
        return user;
    }

    private MvcResult successfulLogin(String email, String password) throws Exception {
        return loginRequest(email, password)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value(email.trim().toLowerCase()))
                .andExpect(cookie().httpOnly(AuthCookieService.ACCESS_COOKIE_NAME, true))
                .andExpect(cookie().secure(AuthCookieService.ACCESS_COOKIE_NAME, true))
                .andExpect(cookie().httpOnly(AuthCookieService.REFRESH_COOKIE_NAME, true))
                .andExpect(cookie().secure(AuthCookieService.REFRESH_COOKIE_NAME, true))
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions loginRequest(String email, String password) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private void assertGenericAuthenticationFailure(org.springframework.test.web.servlet.ResultActions request) throws Exception {
        MvcResult result = request.andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(result.getResponse().getContentAsString())
                .contains("\"code\":\"AUTHENTICATION_FAILED\"")
                .contains("\"message\":\"Authentication is invalid or expired.\"");
    }

    private Cookie csrfCookie(MvcResult csrf) {
        return csrf.getResponse().getCookie("__Host-tinyroute_csrf");
    }

    private String csrfToken(MvcResult csrf) throws Exception {
        return new tools.jackson.databind.json.JsonMapper().readTree(csrf.getResponse().getContentAsString())
                .get("csrfToken")
                .asString();
    }

    private MvcResult csrf() throws Exception {
        return mockMvc.perform(get("/api/auth/csrf")).andReturn();
    }

    private String expiredAccessToken(User user) {
        JwtProperties properties = new JwtProperties();
        properties.setIssuer("https://api.tinyroute.test");
        properties.setAudience("tinyroute-web");
        properties.setActiveKeyId("test");
        properties.setSigningPrivateKeyBase64(Base64.getEncoder().encodeToString(testJwtKeyPair.getPrivate().getEncoded()));
        properties.setVerificationPublicKeys(Map.of(
                "test", Base64.getEncoder().encodeToString(testJwtKeyPair.getPublic().getEncoded())
        ));
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setClockSkew(Duration.ofSeconds(60));
        return new JwtTokenService(
                properties,
                Clock.fixed(Instant.now().minus(Duration.ofMinutes(17)), ZoneOffset.UTC)
        ).issueAccessToken(user.id(), user.tokenVersion());
    }
}
