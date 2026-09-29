package com.tinyroute.controller;

import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.AuthIdentity;
import com.tinyroute.model.User;
import com.tinyroute.repository.AuthIdentityRepository;
import com.tinyroute.repository.UserRepository;
import com.tinyroute.security.AuthCookieService;
import com.tinyroute.service.AccountDeletionRetryJob;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class AccountDeletionTest {

    private static final String PASSWORD = "valid-password-12";
    private final String clientAddress = "198.18."
            + ThreadLocalRandom.current().nextInt(1, 255) + "."
            + ThreadLocalRandom.current().nextInt(1, 255);

    @Autowired MockMvc mockMvc;
    @Autowired UserRepository userRepository;
    @Autowired AuthIdentityRepository authIdentityRepository;
    @Autowired com.tinyroute.security.PasswordHasher passwordHasher;
    @Autowired JdbcTemplate jdbcTemplate;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired AccountDeletionRetryJob accountDeletionRetryJob;

    @AfterEach
    void cleanup() {
        jdbcTemplate.update("delete from account_deletion_cleanup");
        jdbcTemplate.update("delete from links");
        jdbcTemplate.update("delete from password_reset_tokens");
        jdbcTemplate.update("delete from auth_identities");
        jdbcTemplate.update("delete from pending_registrations");
        jdbcTemplate.update("delete from users");
    }

    @Test
    void deletesAccountAndTombstonesOnlyOwnedLinks() throws Exception {
        String ownerEmail = "owner-" + UUID.randomUUID() + "@example.com";
        User owner = user(ownerEmail);
        User other = user("other-" + UUID.randomUUID() + "@example.com");
        authIdentityRepository.save(AuthIdentity.google(owner, "google-" + UUID.randomUUID()));
        jdbcTemplate.update("insert into password_reset_tokens (id, user_id, token_hash, expires_at, created_at) "
                        + "values (?, ?, ?, now() + interval '30 minutes', now())",
                UUID.randomUUID(), owner.id(), "a".repeat(64));
        jdbcTemplate.update("insert into pending_registrations "
                        + "(id, token_hash, email_normalized, password_hash, otp_hash, otp_expires_at, "
                        + "failed_attempts, resend_count, resend_window_started_at, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?, now() + interval '10 minutes', 0, 0, now(), now(), now())",
                UUID.randomUUID(), "b".repeat(64), ownerEmail, "hash", "hash");
        String ownedCode = "owned" + UUID.randomUUID().toString().substring(0, 8);
        String otherCode = "other" + UUID.randomUUID().toString().substring(0, 8);
        link(owner, ownedCode);
        link(other, otherCode);
        redisTemplate.opsForValue().set("redirect:" + ownedCode, "cached destination");
        MvcResult login = login(ownerEmail);
        Cookie access = login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME);
        Cookie refresh = login.getResponse().getCookie(AuthCookieService.REFRESH_COOKIE_NAME);
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();

        mockMvc.perform(delete("/api/auth/account")
                        .cookie(access, refresh, csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf)))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(AuthCookieService.ACCESS_COOKIE_NAME, 0))
                .andExpect(cookie().maxAge(AuthCookieService.REFRESH_COOKIE_NAME, 0));

        assertThat(jdbcTemplate.queryForObject("select status from links where code = ?", String.class, ownedCode))
                .isEqualTo("DELETED");
        assertThat(jdbcTemplate.queryForObject("select status from links where code = ?", String.class, otherCode))
                .isEqualTo("ACTIVE");
        accountDeletionRetryJob.processPending();
        assertThat(redisTemplate.hasKey("redirect:" + ownedCode)).isFalse();
        assertThat(jdbcTemplate.queryForObject("select count(*) from auth_identities where user_id = ?", Integer.class, owner.id()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from password_reset_tokens where user_id = ?", Integer.class, owner.id()))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from pending_registrations where email_normalized = ?", Integer.class, ownerEmail))
                .isZero();
        assertThat(jdbcTemplate.queryForObject("select email_normalized from users where id = ?", String.class, owner.id()))
                .isNotEqualTo(ownerEmail);
        assertThat(jdbcTemplate.queryForObject("select token_version from users where id = ?", Integer.class, owner.id()))
                .isEqualTo(1);
        mockMvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isUnauthorized());
        MvcResult secondCsrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refresh, secondCsrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(secondCsrf)))
                .andExpect(status().isUnauthorized());
        assertThat(login(ownerEmail).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        String email = "reject-" + UUID.randomUUID() + "@example.com";
        User owner = user(email);
        mockMvc.perform(delete("/api/auth/account")).andExpect(status().isForbidden());
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        mockMvc.perform(delete("/api/auth/account")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf)))
                .andExpect(status().isUnauthorized());
        MvcResult login = login(email);
        mockMvc.perform(delete("/api/auth/account")
                        .cookie(login.getResponse().getCookie(AuthCookieService.ACCESS_COOKIE_NAME)))
                .andExpect(status().isForbidden());
        assertThat(userRepository.findById(owner.id())).get().extracting(User::deletedAt).isNull();
    }

    private User user(String email) {
        User user = userRepository.save(User.create(email));
        authIdentityRepository.save(AuthIdentity.password(user, email, passwordHasher.hash(PASSWORD)));
        return user;
    }

    private void link(User user, String code) {
        jdbcTemplate.update("insert into links (id, code, owner_id, destination_url, status, click_count, created_at, updated_at) "
                        + "values (?, ?, ?, ?, 'ACTIVE', 0, now(), now())",
                UUID.randomUUID(), code, user.id(), "https://example.org/path?value=1");
    }

    private MvcResult login(String email) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(post("/api/auth/login")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", csrfToken(csrf))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andReturn();
    }

    private String csrfToken(MvcResult csrf) throws Exception {
        return new tools.jackson.databind.json.JsonMapper().readTree(csrf.getResponse().getContentAsString())
                .get("csrfToken").asString();
    }
}
