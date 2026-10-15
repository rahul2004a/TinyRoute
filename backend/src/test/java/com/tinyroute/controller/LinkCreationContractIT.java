package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tinyroute.security.AuthCookieService;
import jakarta.servlet.http.Cookie;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class LinkCreationContractIT extends LinkHttpTestSupport {
    static final String INPUT = "{\"destinationUrl\":\"https://example.com/docs?q=java#setup\"}";

    @Test
    void listsOnlyOwnedNonDeletedLinksWithStableCursorPagesAndNoCreationQuota() throws Exception {
        var sameTime = java.sql.Timestamp.from(Instant.parse("2026-10-01T12:00:00Z"));
        for (int index = 1; index <= 5; index++) {
            String code = "page-" + index + "-" + ownerId;
            row(
                    code,
                    index == 5 ? "DELETED" : index == 3 ? "DISABLED" : "ACTIVE",
                    index == 2 ? Instant.parse("2020-01-01T00:00:00Z") : null);
            jdbc.update(
                    "update links set id=?,created_at=? where code=?",
                    UUID.fromString("00000000-0000-0000-0000-%012d".formatted(index)),
                    sameTime,
                    code);
        }
        UUID otherOwner = UUID.randomUUID();
        try {
            jdbc.update(
                    "insert into users(id,email_normalized,token_version,created_at,updated_at) values(?,?,0,?,?)",
                    otherOwner,
                    otherOwner + "@example.com",
                    sameTime,
                    sameTime);
            jdbc.update(
                    "insert into links(id,code,owner_id,destination_url,status,created_at,updated_at) values(?,?,?,?, 'ACTIVE',?,?)",
                    UUID.randomUUID(),
                    "other-" + otherOwner,
                    otherOwner,
                    "https://other.example/",
                    sameTime,
                    sameTime);
            var first =
                    mvc.perform(
                                    get("/api/links")
                                            .secure(true)
                                            .cookie(access)
                                            .param("size", "2")
                                            .param("ownerId", otherOwner.toString()))
                            .andExpect(status().isOk())
                            .andExpect(header().string("Cache-Control", "no-store"))
                            .andExpect(jsonPath("$.items.length()").value(2))
                            .andExpect(jsonPath("$.items[0].code").value("page-4-" + ownerId))
                            .andExpect(jsonPath("$.items[1].status").value("DISABLED"))
                            .andExpect(jsonPath("$.items[0].ownerId").doesNotExist())
                            .andReturn();
            String cursor =
                    json.readTree(first.getResponse().getContentAsString())
                            .get("nextCursor")
                            .asString();
            mvc.perform(
                            get("/api/links")
                                    .secure(true)
                                    .cookie(access)
                                    .param("size", "2")
                                    .param("cursor", cursor))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(2))
                    .andExpect(jsonPath("$.items[0].code").value("page-2-" + ownerId))
                    .andExpect(jsonPath("$.items[0].status").value("EXPIRED"))
                    .andExpect(jsonPath("$.items[1].code").value("page-1-" + ownerId))
                    .andExpect(jsonPath("$.nextCursor").isEmpty());
            var forged =
                    Base64.getUrlEncoder()
                            .withoutPadding()
                            .encodeToString(
                                    ("2099-01-01T00:00:00Z|" + UUID.randomUUID())
                                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
            mvc.perform(get("/api/links").secure(true).cookie(access).param("cursor", forged))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.items.length()").value(4));
            assertThat(redis.hasKey("rl:create:" + ownerId)).isFalse();
        } finally {
            jdbc.update("delete from links where owner_id=?", otherOwner);
            jdbc.update("delete from users where id=?", otherOwner);
        }
    }

    @Test
    void listingRequiresValidCredentialsAndBoundsMalformedPageInputs() throws Exception {
        mvc.perform(get("/api/links").secure(true)).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/links")
                                .secure(true)
                                .cookie(
                                        new Cookie(
                                                AuthCookieService.ACCESS_COOKIE_NAME, "invalid")))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/links").secure(true).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isEmpty())
                .andExpect(jsonPath("$.nextCursor").isEmpty());
        for (String size : List.of("0", "-1", "51", "10000000000", "invalid")) {
            mvc.perform(get("/api/links").secure(true).cookie(access).param("size", size))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }
        for (String cursor :
                List.of(
                        "",
                        "malformed",
                        "x".repeat(257),
                        Base64.getUrlEncoder()
                                .encodeToString(
                                        ("+10000-01-01T00:00:00Z|" + UUID.randomUUID())
                                                .getBytes(
                                                        java.nio.charset.StandardCharsets
                                                                .UTF_8)))) {
            mvc.perform(get("/api/links").secure(true).cookie(access).param("cursor", cursor))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.fieldErrors.cursor").isNotEmpty());
        }
        var token = jwt.verifyAccessToken(access.getValue());
        revocations.revoke(token.tokenId(), token.expiresAt());
        mvc.perform(get("/api/links").secure(true).cookie(access))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createsACommittedCopyableGeneratedLinkWithAnExplicitNullExpiry() throws Exception {
        var response =
                mvc.perform(creation(INPUT, access))
                        .andExpect(status().isCreated())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andExpect(header().doesNotExist("Location"))
                        .andExpect(
                                jsonPath("$.destinationUrl")
                                        .value("https://example.com/docs?q=java#setup"))
                        .andExpect(jsonPath("$.ownerId").doesNotExist())
                        .andReturn();
        var body = json.readTree(response.getResponse().getContentAsString());
        assertThat(body.get("code").asString()).matches("[A-Za-z0-9]{8}");
        assertThat(body.get("shortUrl").asString())
                .isEqualTo("https://localhost:8443/" + body.get("code").asString());
        assertThat(body.has("expiresAt")).isTrue();
        assertThat(body.get("expiresAt").isNull()).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select destination_url from links where code=?",
                                String.class,
                                body.get("code").asString()))
                .isEqualTo("https://example.com/docs?q=java#setup");
    }

    @Test
    void rejectsMissingInvalidRevokedAndStaleCredentialsWithoutQuota() throws Exception {
        mvc.perform(creation(INPUT, null)).andExpect(status().isUnauthorized());
        mvc.perform(creation(INPUT, new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, "invalid")))
                .andExpect(status().isUnauthorized());
        var token = jwt.verifyAccessToken(access.getValue());
        revocations.revoke(token.tokenId(), token.expiresAt());
        mvc.perform(creation(INPUT, access)).andExpect(status().isUnauthorized());
        access = new Cookie(AuthCookieService.ACCESS_COOKIE_NAME, jwt.issueAccessToken(ownerId, 0));
        jdbc.update("update users set token_version=1 where id=?", ownerId);
        mvc.perform(creation(INPUT, access)).andExpect(status().isUnauthorized());
        jdbc.update("update users set token_version=0,deleted_at=now() where id=?", ownerId);
        mvc.perform(creation(INPUT, access)).andExpect(status().isUnauthorized());
        assertThat(redis.hasKey("rl:create:" + ownerId)).isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }

    @Test
    void requiresCsrfBeforeCreationQuotaOrPersistence() throws Exception {
        mvc.perform(
                        post("/api/links")
                                .cookie(access)
                                .contentType("application/json")
                                .content(INPUT))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"));
        assertThat(redis.hasKey("rl:create:" + ownerId)).isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }

    @Test
    void returnsActionableSafeDestinationErrorsAndCreatesNoRow() throws Exception {
        for (String value :
                List.of("http://example.com", "invalid", "https://LOCALHOST.:443/self")) {
            mvc.perform(creation(json.writeValueAsString(Map.of("destinationUrl", value)), access))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                    .andExpect(jsonPath("$.error.fieldErrors.destinationUrl").isNotEmpty());
        }
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }

    @Test
    void acceptsAvailableAliasAndReturnsConflictWithoutOverwriting() throws Exception {
        String alias = "alias-" + UUID.randomUUID();
        String body =
                json.writeValueAsString(
                        Map.of("destinationUrl", "https://example.com/one", "alias", alias));
        mvc.perform(creation(body, access))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(alias));
        mvc.perform(creation(body.replace("/one", "/two"), access))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALIAS_UNAVAILABLE"));
        assertThat(
                        jdbc.queryForObject(
                                "select destination_url from links where code=?",
                                String.class,
                                alias))
                .isEqualTo("https://example.com/one");
    }

    @Test
    void rejectsReservedAliasesAndInvalidExpiryWithTheirFieldErrors() throws Exception {
        mvc.perform(
                        creation(
                                "{\"destinationUrl\":\"https://example.com\",\"alias\":\"API\"}",
                                access))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.alias").isNotEmpty());
        mvc.perform(
                        creation(
                                "{\"destinationUrl\":\"https://example.com\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}",
                                access))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.expiresAt").isNotEmpty());
    }

    @Test
    void rejectsJsonCoercionUnknownFieldsAndOversizedBodies() throws Exception {
        for (String body :
                List.of(
                        "{\"destinationUrl\":123}",
                        "{\"destinationUrl\":true}",
                        "{\"destinationUrl\":[]}",
                        "{\"destinationUrl\":{}}",
                        "{\"destinationUrl\":null}",
                        "{\"destinationUrl\":\"https://example.com\",\"alias\":123}",
                        "{\"destinationUrl\":\"https://example.com\",\"expiresAt\":false}",
                        "{\"destinationUrl\":\"https://example.com\",\"ownerId\":\"fake\"}"))
            mvc.perform(creation(body, access)).andExpect(status().isBadRequest());
        mvc.perform(
                        creation(
                                "{\"destinationUrl\":\"https://example.com/"
                                        + "x".repeat(16_384)
                                        + "\"}",
                                access))
                .andExpect(status().isPayloadTooLarge());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }

    @Test
    void countsAuthorizedValidationAttemptsAndGivesMatchingRetryGuidance() throws Exception {
        for (int i = 0; i < 100; i++)
            mvc.perform(creation("{\"destinationUrl\":\"http://example.com\"}", access))
                    .andExpect(status().isBadRequest());
        var result =
                mvc.perform(creation(INPUT, access))
                        .andExpect(status().isTooManyRequests())
                        .andReturn();
        long retry =
                json.readTree(result.getResponse().getContentAsString())
                        .get("error")
                        .get("retryAfterSeconds")
                        .asLong();
        assertThat(retry).isBetween(1L, 3600L);
        assertThat(result.getResponse().getHeader("Retry-After")).isEqualTo(Long.toString(retry));
    }

    @Test
    void unsafeCounterReturnsSafe503WithoutAnInsert() throws Exception {
        redis.opsForValue().set("code:global", "private-invalid-counter");
        try {
            mvc.perform(creation(INPUT, access))
                    .andExpect(status().isServiceUnavailable())
                    .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                    .andExpect(header().doesNotExist("Location"));
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from links where owner_id=?",
                                    Integer.class,
                                    ownerId))
                    .isZero();
        } finally {
            redis.delete("code:global");
        }
    }

    @Test
    void generatedCollisionRetriesConsumeOnlyOneExternalCreationAttempt() throws Exception {
        var properties = new com.tinyroute.config.LinkProperties();
        properties.setCodeKey("AAECAwQFBgcICQoLDA0ODxAREhMUFRYXGBkaGxwdHh8=");
        properties.setCodeSalt("tinyroute-local-v1");
        var encoder =
                new com.tinyroute.service.ShortCodeEncoder(
                        properties.codeKeyBytes(), properties.codeSaltBytes());
        long floor =
                jdbc.queryForObject(
                        "select coalesce(max(generation_value),0) from links", Long.class);
        redis.opsForValue().set("code:global", Long.toString(floor));
        String conflict = encoder.encode(floor + 1).value();
        row(conflict, "DELETED", null);
        mvc.perform(creation(INPUT, access)).andExpect(status().isCreated());
        assertThat(redis.opsForValue().get("rl:create:" + ownerId)).isEqualTo("1");
        assertThat(
                        jdbc.queryForObject(
                                "select destination_url from links where code=?",
                                String.class,
                                conflict))
                .isEqualTo("https://example.com/docs?q=java#setup");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isEqualTo(2);
    }
}
