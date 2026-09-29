package com.tinyroute.controller;

import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class RegistrationMailSaturationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RegistrationMailCapacity registrationMailCapacity;

    @Autowired
    private UserRepository userRepository;

    private int reservedPermits;
    private UUID knownUserId;
    private final String knownEmail = "known-" + UUID.randomUUID() + "@example.com";
    private final String clientAddress = "198.18."
            + ThreadLocalRandom.current().nextInt(1, 255)
            + "." + ThreadLocalRandom.current().nextInt(1, 255);

    @AfterEach
    void removeCreatedRecords() {
        if (knownUserId != null) {
            jdbcTemplate.update("delete from users where id = ?", knownUserId);
        }
        for (int index = 0; index < reservedPermits; index++) {
            registrationMailCapacity.release();
        }
    }

    @Test
    void rejectsRegistrationBeforePersistingStateWhenMailCapacityIsSaturated() throws Exception {
        String email = "capacity-" + UUID.randomUUID() + "@example.com";
        for (int index = 0; index < RegistrationMailCapacity.MAX_PENDING_DELIVERIES; index++) {
            assertThat(registrationMailCapacity.tryReserve()).isTrue();
            reservedPermits++;
        }
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", new tools.jackson.databind.json.JsonMapper()
                                .readTree(csrf.getResponse().getContentAsString()).get("csrfToken").asString())
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from pending_registrations where email_normalized = ?", Integer.class, email)).isZero();
    }

    @Test
    void returnsTheSameUnavailableStatusForKnownAndUnknownEmailsWhenMailCapacityIsSaturated() throws Exception {
        String unknownEmail = "unknown-" + UUID.randomUUID() + "@example.com";
        knownUserId = userRepository.save(User.create(knownEmail)).id();
        for (int index = 0; index < RegistrationMailCapacity.MAX_PENDING_DELIVERIES; index++) {
            assertThat(registrationMailCapacity.tryReserve()).isTrue();
            reservedPermits++;
        }

        assertThat(register(knownEmail).getResponse().getStatus()).isEqualTo(503);
        assertThat(register(unknownEmail).getResponse().getStatus()).isEqualTo(503);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from pending_registrations where email_normalized in (?, ?)",
                Integer.class, knownEmail, unknownEmail)).isZero();
    }

    private MvcResult register(String email) throws Exception {
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();
        return mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", new tools.jackson.databind.json.JsonMapper()
                                .readTree(csrf.getResponse().getContentAsString()).get("csrfToken").asString())
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"valid-password-12\"}"))
                .andReturn();
    }
}
