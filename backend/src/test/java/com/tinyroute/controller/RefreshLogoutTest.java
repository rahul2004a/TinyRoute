package com.tinyroute.controller;

import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.TokenHashing;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.ThreadLocalRandom;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class RefreshLogoutTest {

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String clientAddress = "198.18."
            + ThreadLocalRandom.current().nextInt(1, 255)
            + "."
            + ThreadLocalRandom.current().nextInt(1, 255);

    @AfterEach
    void removeCreatedRecords() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "auth_identities", "users");
    }

    @Test
    void rotatesAnActiveRefreshSessionWithoutReturningTokensAndRejectsReuse() throws Exception {
        createPasswordUser("refresh@example.com");
        MvcResult login = successfulLogin("refresh@example.com");
        Cookie originalRefresh = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);

        MvcResult refreshed = refresh(originalRefresh)
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("refresh@example.com"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andExpect(cookie().httpOnly(AuthCookieService.ACCESS_COOKIE_NAME, true))
                .andExpect(cookie().httpOnly(AuthCookieService.REFRESH_COOKIE_NAME, true))
                .andReturn();

        Cookie replacementRefresh = refreshed.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        assertThat(replacementRefresh.getValue()).isNotEqualTo(originalRefresh.getValue());

        refresh(originalRefresh)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("REFRESH_CONCURRENT"));

        redisTemplate.opsForHash().put("refresh-used:" + TokenHashing.sha256(originalRefresh.getValue()),
                "rotatedAt", Long.toString(Instant.now().minusSeconds(6).toEpochMilli()));

        refresh(originalRefresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));

        refresh(replacementRefresh)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void rejectsMissingExpiredOrStaleRefreshSessionsAndBadCsrf() throws Exception {
        User staleUser = createPasswordUser("stale-refresh@example.com");
        MvcResult login = successfulLogin("stale-refresh@example.com");
        Cookie refreshCookie = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        jdbcTemplate.update("update users set token_version = token_version + 1 where id = ?", staleUser.id());

        refresh(refreshCookie)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));

        refresh(new Cookie(AuthCookieService.REFRESH_COOKIE_NAME, "expired-or-unknown"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"));
    }

    @Test
    void logoutRevokesTheCurrentAccessTokenThenClearsCookies() throws Exception {
        createPasswordUser("logout@example.com");
        MvcResult login = successfulLogin("logout@example.com");

        logout(
                login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME),
                login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME)
        )
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(cookie().maxAge(AuthCookieService.ACCESS_COOKIE_NAME, 0))
                .andExpect(cookie().maxAge(AuthCookieService.REFRESH_COOKIE_NAME, 0));

        mockMvc.perform(get("/api/auth/me")
                        .cookie(login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));

        refresh(login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
    }

    @Test
    void logoutDoesNotDeleteAnotherBrowserSessionForTheSameUser() throws Exception {
        createPasswordUser("two-sessions@example.com");
        MvcResult firstLogin = successfulLogin("two-sessions@example.com");
        MvcResult secondLogin = successfulLogin("two-sessions@example.com");

        logout(
                firstLogin.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME),
                secondLogin.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME)
        ).andExpect(status().isUnauthorized());

        refresh(secondLogin.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME))
                .andExpect(status().isOk());
    }

    @Test
    void logoutWithOldAccessAndNewRefreshCookieDeletesRotatedCurrentSession() throws Exception {
        createPasswordUser("rotated-logout@example.com");
        MvcResult login = successfulLogin("rotated-logout@example.com");
        Cookie oldAccess = login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME);
        Cookie oldRefresh = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        Cookie newRefresh = refresh(oldRefresh).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);

        logout(oldAccess, newRefresh).andExpect(status().isNoContent());
        refresh(newRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithOldAccessAndOldRefreshCookieFollowsRotation() throws Exception {
        createPasswordUser("stale-cookie-logout@example.com");
        MvcResult login = successfulLogin("stale-cookie-logout@example.com");
        Cookie oldAccess = login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME);
        Cookie oldRefresh = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        Cookie newRefresh = refresh(oldRefresh).andExpect(status().isOk())
                .andReturn().getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);

        logout(oldAccess, oldRefresh).andExpect(status().isNoContent());
        refresh(newRefresh).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutWithoutRefreshCookieStillRevokesTheAccessBoundSession() throws Exception {
        createPasswordUser("missing-cookie-logout@example.com");
        MvcResult login = successfulLogin("missing-cookie-logout@example.com");
        Cookie access = login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME);
        Cookie refresh = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(access, csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", csrfToken(csrf)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        refresh(refresh).andExpect(status().isUnauthorized());
    }

    @Test
    void rateLimitsRefreshesPerSessionFamily() throws Exception {
        createPasswordUser("rate-limited-refresh@example.com");
        Cookie refreshCookie = successfulLogin("rate-limited-refresh@example.com")
                .getResponse()
                .getCookie(AuthCookieService.REFRESH_COOKIE_NAME);

        for (int attempt = 0; attempt < 30; attempt++) {
            refreshCookie = refresh(refreshCookie)
                    .andExpect(status().isOk())
                    .andReturn()
                    .getResponse()
                    .getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        }

        refresh(refreshCookie)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    private User createPasswordUser(String email) {
        User user = userRepository.save(User.create(email));
        authIdentityRepository.save(AuthIdentity.password(user, email, passwordHasher.hash(PASSWORD)));
        return user;
    }

    private MvcResult successfulLogin(String email) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie refreshCookie) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshCookie, csrfCookie(csrf))
                .header("X-CSRF-TOKEN", csrfToken(csrf)));
    }

    private org.springframework.test.web.servlet.ResultActions logout(Cookie accessCookie, Cookie refreshCookie) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(post("/api/auth/logout")
                .cookie(accessCookie, refreshCookie, csrfCookie(csrf))
                .header("X-CSRF-TOKEN", csrfToken(csrf)));
    }

    private MvcResult csrf() throws Exception {
        return mockMvc.perform(get("/api/auth/csrf")).andReturn();
    }

    private Cookie csrfCookie(MvcResult csrf) {
        return csrf.getResponse().getCookie("__Host-tinyroute_csrf");
    }

    private String csrfToken(MvcResult csrf) throws Exception {
        return new tools.jackson.databind.json.JsonMapper().readTree(csrf.getResponse().getContentAsString())
                .get("csrfToken")
                .asString();
    }
}
