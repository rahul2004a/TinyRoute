package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyroute.client.PasswordResetMailAdapter;
import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.security.PasswordHasher;
import com.tinyroute.security.TokenHashing;
import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({
    TestJwtTokenConfiguration.class,
    TestInfrastructureConfiguration.class,
    PasswordResetIT.PasswordResetMailConfiguration.class
})
class PasswordResetIT {

    private static final String CURRENT_PASSWORD = "valid-password-12";
    private static final String NEW_PASSWORD = "replacement-password-12";

    @Autowired private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    @Autowired private AuthIdentityRepository authIdentityRepository;

    @Autowired private PasswordHasher passwordHasher;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private CapturingPasswordResetMailAdapter passwordResetMailAdapter;

    @Autowired
    @Qualifier("passwordResetExecutor")
    private ThreadPoolTaskExecutor passwordResetExecutor;

    private final String clientAddress =
            "198.18."
                    + ThreadLocalRandom.current().nextInt(1, 255)
                    + "."
                    + ThreadLocalRandom.current().nextInt(1, 255);

    @BeforeEach
    void clearCapturedMail() {
        passwordResetMailAdapter.clear();
    }

    @AfterEach
    void removeCreatedRecords() throws InterruptedException {
        awaitWorkerIdle();
        jdbcTemplate.update("delete from password_reset_tokens");
        jdbcTemplate.update("delete from auth_identities");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void acceptsPasswordResetRequestsWithoutReturningAccountState() throws Exception {
        requestReset("unknown@example.com")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.token").doesNotExist());

        awaitWorkerIdle();
        assertThat(passwordResetMailAdapter.lastResetUrl()).isNull();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from password_reset_tokens", Integer.class))
                .isZero();
    }

    @Test
    void storesOnlyAHashedFragmentTokenAndConsumesItOnceWhileInvalidatingSessions()
            throws Exception {
        User user = createPasswordUser("user@example.com");
        MvcResult priorLogin =
                login("user@example.com", CURRENT_PASSWORD).andExpect(status().isOk()).andReturn();

        requestReset(" USER@example.com ")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        String rawToken = tokenFrom(awaitResetUrl());
        String storedHash =
                jdbcTemplate.queryForObject(
                        "select token_hash from password_reset_tokens", String.class);
        assertThat(rawToken).hasSize(43);
        assertThat(storedHash).isEqualTo(TokenHashing.sha256(rawToken)).isNotEqualTo(rawToken);
        assertThat(passwordResetMailAdapter.lastEmail()).isEqualTo("user@example.com");

        confirm(rawToken, NEW_PASSWORD).andExpect(status().isNoContent());

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select token_version from users where id = ?",
                                Integer.class,
                                user.id()))
                .isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from password_reset_tokens", Integer.class))
                .isZero();
        mockMvc.perform(
                        get("/api/auth/me")
                                .cookie(
                                        priorLogin
                                                .getResponse()
                                                .getCookie(AuthCookieService.ACCESS_COOKIE_NAME)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        refresh(priorLogin.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        login("user@example.com", CURRENT_PASSWORD)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"));
        login("user@example.com", NEW_PASSWORD).andExpect(status().isOk());
        confirm(rawToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESET_TOKEN_INVALID"));
    }

    @Test
    void rejectsAndRemovesAnExpiredResetTokenWithTheGenericInvalidResponse() throws Exception {
        createPasswordUser("expired@example.com");
        requestReset("expired@example.com").andExpect(status().isAccepted());
        String rawToken = tokenFrom(awaitResetUrl());
        jdbcTemplate.update(
                "update password_reset_tokens set expires_at = now() - interval '1 second'");

        confirm(rawToken, NEW_PASSWORD)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("RESET_TOKEN_INVALID"));

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from password_reset_tokens", Integer.class))
                .isZero();
    }

    private String csrfToken(MvcResult csrf) throws Exception {
        return new tools.jackson.databind.json.JsonMapper()
                .readTree(csrf.getResponse().getContentAsString())
                .get("csrfToken")
                .asString();
    }

    private User createPasswordUser(String email) {
        User user = userRepository.save(User.create(email));
        authIdentityRepository.save(
                AuthIdentity.password(user, email, passwordHasher.hash(CURRENT_PASSWORD)));
        return user;
    }

    private org.springframework.test.web.servlet.ResultActions requestReset(String email)
            throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(
                post("/api/auth/password-reset")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions confirm(
            String token, String password) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(
                post("/api/auth/password-reset/confirm")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content(
                                "{\"token\":\""
                                        + token
                                        + "\",\"newPassword\":\""
                                        + password
                                        + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password)
            throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(
                post("/api/auth/login")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content(
                                "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(Cookie refreshCookie)
            throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(
                post("/api/auth/refresh")
                        .cookie(
                                refreshCookie,
                                csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf)));
    }

    private String tokenFrom(String resetUrl) {
        URI uri = URI.create(resetUrl);
        assertThat(uri.getQuery()).isNull();
        assertThat(uri.getFragment()).startsWith("token=");
        return uri.getFragment().substring("token=".length());
    }

    private String awaitResetUrl() throws InterruptedException {
        for (int attempt = 0;
                attempt < 100 && passwordResetMailAdapter.lastResetUrl() == null;
                attempt++) {
            Thread.sleep(25);
        }
        assertThat(passwordResetMailAdapter.lastResetUrl()).isNotNull();
        return passwordResetMailAdapter.lastResetUrl();
    }

    private void awaitWorkerIdle() throws InterruptedException {
        for (int attempt = 0; attempt < 100; attempt++) {
            if (passwordResetExecutor.getActiveCount() == 0
                    && passwordResetExecutor.getThreadPoolExecutor().getQueue().isEmpty()) {
                return;
            }
            Thread.sleep(25);
        }
        assertThat(passwordResetExecutor.getActiveCount()).isZero();
        assertThat(passwordResetExecutor.getThreadPoolExecutor().getQueue()).isEmpty();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PasswordResetMailConfiguration {

        @Bean
        @Primary
        CapturingPasswordResetMailAdapter passwordResetMailAdapter() {
            return new CapturingPasswordResetMailAdapter();
        }
    }

    static class CapturingPasswordResetMailAdapter implements PasswordResetMailAdapter {

        private volatile String email;
        private volatile String resetUrl;

        @Override
        public CompletableFuture<Void> sendPasswordReset(String email, String resetUrl) {
            this.email = email;
            this.resetUrl = resetUrl;
            return CompletableFuture.completedFuture(null);
        }

        String lastEmail() {
            return email;
        }

        String lastResetUrl() {
            return resetUrl;
        }

        void clear() {
            email = null;
            resetUrl = null;
        }
    }
}
