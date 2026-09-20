package com.tinyroute.security;

import com.tinyroute.config.TestJwtTokenConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class SecurityPerimeterTest {

    private static final String ALLOWED_ORIGIN = "https://localhost:3000";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void exposesANoStoreCsrfTokenOnlyToTheConfiguredCredentialedOrigin() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(jsonPath("$.csrfToken").isNotEmpty());
    }

    @Test
    void csrfBootstrapUsesASecureHostOnlyCookieInsteadOfAnApplicationSession() throws Exception {
        mockMvc.perform(get("/api/auth/csrf").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .httpOnly("__Host-tinyroute_csrf", true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .secure("__Host-tinyroute_csrf", true))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .path("__Host-tinyroute_csrf", "/"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie()
                        .doesNotExist("JSESSIONID"));
    }

    @Test
    void rejectsCorsRequestsFromOriginsOutsideTheExactAllowlist() throws Exception {
        mockMvc.perform(options("/api/auth/csrf")
                        .header(HttpHeaders.ORIGIN, "https://attacker.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void rejectsStateChangingRequestsWithoutTheCsrfHeader() throws Exception {
        mockMvc.perform(post("/api/auth/register").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("CSRF_INVALID"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
    }

    @Test
    void returnsASafeJsonAuthenticationErrorForAProtectedApiRequestWithoutAnAccessCookie() throws Exception {
        mockMvc.perform(get("/api/protected").header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("AUTHENTICATION_FAILED"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
    }
}
