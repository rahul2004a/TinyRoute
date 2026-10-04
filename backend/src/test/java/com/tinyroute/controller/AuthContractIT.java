package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
class AuthContractIT {

    private final String clientAddress =
            "198.18."
                    + ThreadLocalRandom.current().nextInt(1, 255)
                    + "."
                    + ThreadLocalRandom.current().nextInt(1, 255);

    @Autowired MockMvc mockMvc;
    @Autowired MeterRegistry meterRegistry;

    @Test
    void recordsCsrfAndAuthenticationRejectionsWithoutReturningSecrets() throws Exception {
        String secret = "contract-password-12345";
        MvcResult csrf =
                mockMvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();

        MvcResult missingCsrf =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .contentType("application/json")
                                        .content(
                                                "{\"email\":\"nobody@example.com\",\"password\":\""
                                                        + secret
                                                        + "\"}"))
                        .andExpect(status().isForbidden())
                        .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"))
                        .andExpect(jsonPath("$.error.requestId").isNotEmpty())
                        .andReturn();

        MvcResult badCredentials =
                mockMvc.perform(
                                post("/api/auth/login")
                                        .cookie(
                                                csrf.getResponse()
                                                        .getCookie("__Host-tinyroute_csrf"))
                                        .header(
                                                "X-CSRF-TOKEN",
                                                new tools.jackson.databind.json.JsonMapper()
                                                        .readTree(
                                                                csrf.getResponse()
                                                                        .getContentAsString())
                                                        .get("csrfToken")
                                                        .asString())
                                        .header("X-Forwarded-For", clientAddress)
                                        .contentType("application/json")
                                        .content(
                                                "{\"email\":\"nobody@example.com\",\"password\":\""
                                                        + secret
                                                        + "\"}"))
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"))
                        .andReturn();

        assertThat(missingCsrf.getResponse().getContentAsString()).doesNotContain(secret);
        assertThat(badCredentials.getResponse().getContentAsString()).doesNotContain(secret);
        assertThat(
                        meterRegistry
                                .get("tinyroute.auth.requests")
                                .tags("route", "login", "status", "403")
                                .counter()
                                .count())
                .isGreaterThanOrEqualTo(1);
        assertThat(
                        meterRegistry
                                .get("tinyroute.auth.requests")
                                .tags("route", "login", "status", "401")
                                .counter()
                                .count())
                .isGreaterThanOrEqualTo(1);
        assertThat(
                        meterRegistry.getMeters().stream()
                                .filter(
                                        meter ->
                                                meter.getId()
                                                        .getName()
                                                        .startsWith("tinyroute.auth."))
                                .map(meter -> meter.getId().toString())
                                .toList()
                                .toString())
                .doesNotContain(secret);
    }
}
