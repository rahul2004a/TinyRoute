package com.tinyroute.controller;

import com.tinyroute.client.RegistrationMailCapacity;
import com.tinyroute.config.TestJwtTokenConfiguration;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
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

    private int reservedPermits;

    @AfterEach
    void removeCreatedRecords() {
        jdbcTemplate.update("delete from pending_registrations");
        for (int index = 0; index < reservedPermits; index++) {
            registrationMailCapacity.release();
        }
    }

    @Test
    void rejectsRegistrationBeforePersistingStateWhenMailCapacityIsSaturated() throws Exception {
        for (int index = 0; index < RegistrationMailCapacity.MAX_PENDING_DELIVERIES; index++) {
            assertThat(registrationMailCapacity.tryReserve()).isTrue();
            reservedPermits++;
        }
        MvcResult csrf = mockMvc.perform(get("/api/auth/csrf")).andReturn();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrf.getResponse().getCookie("__Host-tinyroute_csrf"))
                        .header("X-CSRF-TOKEN", new tools.jackson.databind.json.JsonMapper()
                                .readTree(csrf.getResponse().getContentAsString()).get("csrfToken").asString())
                        .header("X-Forwarded-For", "198.18.0.42")
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from pending_registrations", Integer.class)).isZero();
    }
}
