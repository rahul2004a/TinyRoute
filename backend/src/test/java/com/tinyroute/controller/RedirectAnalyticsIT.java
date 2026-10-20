package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RedirectAnalyticsIT extends LinkHttpTestSupport {
    @Test
    void databaseAndWarmCacheSuccessCountButHeadHealthAndFailedRedirectsDoNot() throws Exception {
        String code =
                row(
                        "ana" + UUID.randomUUID().toString().replace("-", "").substring(0, 12),
                        "ACTIVE",
                        null);
        mvc.perform(
                        redirect("/" + code)
                                .header("Referer", "https://EXAMPLE.org/private?visitor=secret")
                                .header(
                                        "User-Agent",
                                        "Mozilla/5.0 (Windows NT 10.0) Chrome/123 Safari/537 Edg/123"))
                .andExpect(status().isFound());
        mvc.perform(redirect("/" + code)).andExpect(status().isFound());
        mvc.perform(head("/" + code).secure(true).header("X-Forwarded-For", clientAddress))
                .andExpect(status().isFound());
        mvc.perform(get("/actuator/health").secure(true)).andExpect(status().isOk());
        mvc.perform(
                        redirect(
                                "/"
                                        + row(
                                                "bad"
                                                        + UUID.randomUUID()
                                                                .toString()
                                                                .replace("-", "")
                                                                .substring(0, 12),
                                                "DISABLED",
                                                null)))
                .andExpect(status().isForbidden());
        mvc.perform(redirect("/" + UUID.randomUUID().toString())).andExpect(status().isNotFound());
        Instant deadline = Instant.now().plusSeconds(8);
        while (Instant.now().isBefore(deadline)
                && jdbc.queryForObject(
                                "select click_count from links where code=?", Long.class, code)
                        < 2) Thread.sleep(25);
        assertThat(
                        jdbc.queryForObject(
                                "select click_count from links where code=?", Long.class, code))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from click_events e join links l on l.id=e.link_id where l.owner_id=?",
                                Long.class,
                                ownerId))
                .isEqualTo(2);
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from click_events e join links l on l.id=e.link_id where l.code=? and referrer_domain='example.org' and browser='Edge'",
                                Long.class,
                                code))
                .isEqualTo(1);
    }
}
