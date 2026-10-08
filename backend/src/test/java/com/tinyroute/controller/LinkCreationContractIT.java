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
