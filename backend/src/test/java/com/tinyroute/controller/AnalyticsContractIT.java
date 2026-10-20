package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.Timestamp;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AnalyticsContractIT extends LinkHttpTestSupport {
    @Test
    void overviewRequiresAuthenticationAndDefaultsToThirtyDays() throws Exception {
        mvc.perform(get("/api/analytics").secure(true)).andExpect(status().isUnauthorized());
        var node =
                json.readTree(
                        mvc.perform(get("/api/analytics").secure(true).cookie(access))
                                .andExpect(status().isOk())
                                .andExpect(header().string("Cache-Control", "no-store"))
                                .andReturn()
                                .getResponse()
                                .getContentAsString());
        assertThat(node.get("totalClicks").asLong()).isZero();
        assertThat(node.get("dailyClicks").size()).isEqualTo(30);
        assertThat(node.get("range").get("timeZone").asString()).isEqualTo("UTC");
    }

    @Test
    void missingNonOwnedDeletedAndMalformedIdsHaveSameGeneric404EvenWithInvalidRange()
            throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        UUID id = jdbc.queryForObject("select id from links where code=?", UUID.class, code);
        jdbc.update("update links set status='DELETED' where id=?", id);
        for (String candidate :
                new String[] {id.toString(), UUID.randomUUID().toString(), "malformed"}) {
            mvc.perform(
                            get("/api/links/" + candidate + "/analytics?from=bad")
                                    .secure(true)
                                    .cookie(access))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        }
    }

    @Test
    void rangesAreValidatedAndEveryBucketMatchesTheSelectedScope() throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        UUID id = jdbc.queryForObject("select id from links where code=?", UUID.class, code);
        jdbc.update("update links set click_count=9 where id=?", id);
        for (String day :
                new String[] {
                    "2024-02-28T23:59:59Z",
                    "2024-02-29T00:00:00Z",
                    "2024-02-29T23:59:59Z",
                    "2024-03-01T00:00:00Z"
                })
            jdbc.update(
                    "insert into click_events(id,link_id,clicked_at,device,operating_system,browser) values(?,?,?,'Mobile','iOS','Safari')",
                    UUID.randomUUID(),
                    id,
                    Timestamp.from(Instant.parse(day)));
        var response =
                mvc.perform(
                                get("/api/links/" + id + "/analytics?from=2024-02-29&to=2024-02-29")
                                        .secure(true)
                                        .cookie(access))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalClicks").value(2))
                        .andExpect(jsonPath("$.lifetimeClicks").value(9))
                        .andExpect(jsonPath("$.dailyClicks[0].count").value(2))
                        .andExpect(jsonPath("$.breakdowns.devices[0].label").value("Mobile"))
                        .andReturn();
        var node = json.readTree(response.getResponse().getContentAsString());
        for (String dimension :
                new String[] {
                    "referrers", "devices", "operatingSystems", "browsers", "countries", "cities"
                })
            assertThat(node.get("breakdowns").get(dimension).get(0).get("count").asLong())
                    .isEqualTo(2);
        for (String query :
                new String[] {
                    "from=bad&to=2024-02-29",
                    "from=2024-02-29",
                    "from=2024-02-29&from=2024-02-28&to=2024-02-29"
                })
            mvc.perform(get("/api/links/" + id + "/analytics?" + query).secure(true).cookie(access))
                    .andExpect(status().isBadRequest());
    }

    @Test
    void crossAccountReadsNeverRevealLinkOrEvents() throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        UUID id = jdbc.queryForObject("select id from links where code=?", UUID.class, code);
        UUID other = UUID.randomUUID();
        var now = Timestamp.from(Instant.now());
        jdbc.update(
                "insert into users(id,email_normalized,token_version,created_at,updated_at) values(?,?,0,?,?)",
                other,
                other + "@example.com",
                now,
                now);
        try {
            var cookie =
                    new jakarta.servlet.http.Cookie(
                            com.tinyroute.security.AuthCookieService.ACCESS_COOKIE_NAME,
                            jwt.issueAccessToken(other, 0));
            mvc.perform(get("/api/links/" + id + "/analytics").secure(true).cookie(cookie))
                    .andExpect(status().isNotFound());
            mvc.perform(get("/api/analytics").secure(true).cookie(cookie))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.lifetimeClicks").value(0));
        } finally {
            jdbc.update("delete from users where id=?", other);
        }
    }

    @Test
    void overviewBoundsKnownDimensionsWithoutLosingUnknownOrOtherCounts() throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        UUID id = jdbc.queryForObject("select id from links where code=?", UUID.class, code);
        for (int i = 0; i < 27; i++)
            jdbc.update(
                    "insert into click_events(id,link_id,clicked_at,referrer_domain,device,operating_system,browser) values(?,?,?,?,'Unknown','Unknown','Unknown')",
                    UUID.randomUUID(),
                    id,
                    Timestamp.from(Instant.parse("2024-02-29T12:00:00Z")),
                    i == 25 ? null : i == 26 ? "" : "host-" + i + ".example");
        var response =
                mvc.perform(
                                get("/api/analytics?from=2024-02-28&to=2024-03-01")
                                        .secure(true)
                                        .cookie(access))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.totalClicks").value(27))
                        .andExpect(jsonPath("$.dailyClicks[0].count").value(0))
                        .andReturn();
        var rows =
                json.readTree(response.getResponse().getContentAsString())
                        .get("breakdowns")
                        .get("referrers");
        assertThat(rows.size()).isEqualTo(23);
        long sum = 0, other = 0;
        for (var item : rows) {
            sum += item.get("count").asLong();
            if (item.get("kind").asString().equals("OTHER")) other = item.get("count").asLong();
        }
        assertThat(sum).isEqualTo(27);
        assertThat(other).isEqualTo(5);
    }

    @Test
    void countryTiesSortByDisplayedNames() throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        UUID id = jdbc.queryForObject("select id from links where code=?", UUID.class, code);
        for (String country : new String[] {"DE", "ES", "FI"})
            jdbc.update(
                    "insert into click_events(id,link_id,clicked_at,device,operating_system,browser,country) values(?,?,?,'Unknown','Unknown','Unknown',?)",
                    UUID.randomUUID(),
                    id,
                    Timestamp.from(Instant.parse("2024-02-29T12:00:00Z")),
                    country);
        mvc.perform(get("/api/analytics?from=2024-02-29&to=2024-02-29").secure(true).cookie(access))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.breakdowns.countries[0].label").value("Finland"))
                .andExpect(jsonPath("$.breakdowns.countries[1].label").value("Germany"))
                .andExpect(jsonPath("$.breakdowns.countries[2].label").value("Spain"));
    }
}
