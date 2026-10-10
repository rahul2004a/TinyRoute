package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tinyroute.config.LinkVerificationConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@Import(LinkVerificationConfiguration.class)
@TestPropertySource(
        properties =
                "tinyroute.verification.token=synthetic-verification-token-at-least-32-characters")
class LinkVerificationFixtureIT extends LinkHttpTestSupport {
    static final String TOKEN = "synthetic-verification-token-at-least-32-characters";

    @AfterAll
    static void removeOnlyFixtureAccounts(
            @Autowired LinkVerificationController fixtures, @Autowired JdbcTemplate jdbc) {
        for (var account : fixtures.bootstrap().getBody().accounts()) {
            jdbc.update(
                    "delete from links where owner_id in (select id from users where email_normalized=?)",
                    account.email());
            jdbc.update(
                    "delete from auth_identities where user_id in (select id from users where email_normalized=?)",
                    account.email());
            jdbc.update("delete from users where email_normalized=?", account.email());
        }
    }

    @Test
    void timingReadoutAndResetUseTheSamePrivateFixtureGuard() throws Exception {
        mvc.perform(get("/__verification/timings")).andExpect(status().isUnauthorized());
        mvc.perform(post("/__verification/timings/reset")).andExpect(status().isUnauthorized());
        mvc.perform(post("/__verification/timings/reset").header("X-Verification-Token", TOKEN))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Cache-Control", "no-store"));
        mvc.perform(get("/__verification/timings").header("X-Verification-Token", TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.samples").isEmpty())
                .andExpect(jsonPath("$.overflowed").value(false));
    }

    @Test
    void fixtureRequiresTheTokenAndALoopbackSource() throws Exception {
        mvc.perform(get("/__verification/bootstrap"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(get("/__verification/bootstrap").header("X-Verification-Token", "wrong"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/__verification/bootstrap")
                                .header("X-Verification-Token", TOKEN)
                                .with(
                                        r -> {
                                            r.setRemoteAddr("192.0.2.1");
                                            return r;
                                        }))
                .andExpect(status().isUnauthorized());
        var result =
                mvc.perform(get("/__verification/bootstrap").header("X-Verification-Token", TOKEN))
                        .andExpect(status().isOk())
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn();
        var body = json.readTree(result.getResponse().getContentAsString());
        assertThat(body.get("codes").size()).isEqualTo(100);
        assertThat(body.get("accounts").size()).isEqualTo(3);
    }

    @Test
    void stateFixtureChangesOnlyDisposableStateWithoutShadowingASingleSegmentCode()
            throws Exception {
        String code = row("fixture-state-" + java.util.UUID.randomUUID(), "ACTIVE", null);
        mvc.perform(
                        post("/__verification/links/state")
                                .header("X-Verification-Token", TOKEN)
                                .contentType("application/json")
                                .content(
                                        json.writeValueAsString(
                                                java.util.Map.of(
                                                        "code", code, "status", "DISABLED"))))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select status from links where code=?", String.class, code))
                .isEqualTo("DISABLED");
        row("__verification", "ACTIVE", null);
        mvc.perform(redirect("/__verification")).andExpect(status().isFound());
        mvc.perform(
                        post("/__verification/links/state")
                                .contentType("application/json")
                                .content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(creation("{\"destinationUrl\":\"https://example.com\"}", null))
                .andExpect(status().isUnauthorized());
    }
}
