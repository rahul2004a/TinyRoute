package com.tinyroute.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import com.tinyroute.cache.JwtRevocationStore;
import com.tinyroute.config.*;
import com.tinyroute.security.*;
import jakarta.servlet.http.Cookie;
import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
abstract class LinkHttpTestSupport {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired JwtTokenService jwt;
    @Autowired JwtRevocationStore revocations;
    @Autowired StringRedisTemplate redis;
    final JsonMapper json = new JsonMapper();
    UUID ownerId;
    Cookie access;
    String clientAddress;

    @BeforeEach
    void createOwner() {
        ownerId = UUID.randomUUID();
        var now = Timestamp.from(Instant.now());
        jdbc.update(
                "insert into users(id,email_normalized,token_version,created_at,updated_at) values(?,?,0,?,?)",
                ownerId,
                "http-" + ownerId + "@example.com",
                now,
                now);
        access = new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, jwt.issueAccessToken(ownerId, 0));
        clientAddress =
                "198.18."
                        + java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 255)
                        + "."
                        + java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 255);
    }

    @AfterEach
    void removeOwner() {
        jdbc.update("delete from account_deletion_cleanup where user_id=?", ownerId);
        jdbc.update("delete from links where owner_id=?", ownerId);
        jdbc.update("delete from users where id=?", ownerId);
        redis.delete("rl:create:" + ownerId);
    }

    MockHttpServletRequestBuilder creation(String body, Cookie credential) throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        var request =
                post("/api/links")
                        .secure(true)
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header(
                                "X-CSRF-TOKEN",
                                json.readTree(csrf.getResponse().getContentAsString())
                                        .get("csrfToken")
                                        .asString())
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content(body);
        if (credential != null) request.cookie(credential);
        return request;
    }

    MockHttpServletRequestBuilder redirect(String path) {
        return get(path).secure(true).header("X-Forwarded-For", clientAddress);
    }

    String row(String code, String status, Instant expiry) {
        var id = UUID.randomUUID();
        var now = Timestamp.from(Instant.now());
        jdbc.update(
                "insert into links(id,code,owner_id,destination_url,status,created_at,updated_at,expires_at) values(?,?,?,?,?,?,?,?)",
                id,
                code,
                ownerId,
                "https://example.com/docs?q=java#setup",
                status,
                now,
                now,
                expiry == null ? null : Timestamp.from(expiry));
        return code;
    }
}
