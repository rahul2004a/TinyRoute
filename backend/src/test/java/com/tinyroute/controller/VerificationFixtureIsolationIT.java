package com.tinyroute.controller;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

class VerificationFixtureIsolationIT extends LinkHttpTestSupport {
    @Test
    void normalApplicationDoesNotExposeTestFixtures() throws Exception {
        mvc.perform(redirect("/__verification/bootstrap"))
                .andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }
}
