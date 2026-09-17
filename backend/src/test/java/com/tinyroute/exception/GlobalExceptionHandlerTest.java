package com.tinyroute.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ThrowingController())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void returnsASafeAuthenticationErrorForAnInvalidAccessToken() throws Exception {
        mockMvc.perform(get("/test/protected").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.error.message").value("Authentication is invalid or expired."))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
    }

    @RestController
    static final class ThrowingController {

        @GetMapping("/test/protected")
        void protectedEndpoint() {
            throw new InvalidAccessTokenException();
        }
    }
}
