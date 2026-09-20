package com.tinyroute.controller;

import com.tinyroute.client.RegistrationMailAdapter;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.HttpHeaders;
import jakarta.servlet.http.Cookie;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.CompletableFuture;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, RegistrationFlowTest.RegistrationMailConfiguration.class})
class RegistrationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CapturingRegistrationMailAdapter registrationMailAdapter;

    private final String clientAddress = "198.18."
            + ThreadLocalRandom.current().nextInt(1, 255)
            + "."
            + ThreadLocalRandom.current().nextInt(1, 255);

    @AfterEach
    void removeCreatedRecords() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "pending_registrations", "auth_identities", "users");
    }

    @Test
    void startsAPendingRegistrationWithoutIssuingAuthenticationCookies() throws Exception {
        MvcResult registration = startRegistration();

        assertThat(registration.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("pending_registration=");
    }

    @Test
    void verifiesTheOtpOnceAndOnlyThenIssuesAuthenticationCookies() throws Exception {
        MvcResult registration = startRegistration();
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/register/verify")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .cookie(pendingCookie(registration))
                        .contentType("application/json")
                        .content("{\"otp\":\"" + registrationMailAdapter.lastOtp() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authenticated").value(true))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(cookie().httpOnly("__Host-tinyroute_access", true))
                .andExpect(cookie().httpOnly("__Host-tinyroute_refresh", true))
                .andExpect(cookie().maxAge("pending_registration", 0));

        MvcResult secondCsrf = csrf();
        mockMvc.perform(post("/api/auth/register/verify")
                        .cookie(csrfCookie(secondCsrf))
                        .header("X-CSRF-TOKEN", jsonValue(secondCsrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .cookie(pendingCookie(registration))
                        .contentType("application/json")
                        .content("{\"otp\":\"" + registrationMailAdapter.lastOtp() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));
    }

    @Test
    void deletesThePendingRegistrationAfterFiveInvalidOtpAttempts() throws Exception {
        MvcResult registration = startRegistration();
        String invalidOtp = registrationMailAdapter.lastOtp().equals("000000") ? "000001" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            MvcResult csrf = csrf();
            mockMvc.perform(post("/api/auth/register/verify")
                            .cookie(csrfCookie(csrf))
                            .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                            .header("X-Forwarded-For", clientAddress)
                            .cookie(pendingCookie(registration))
                            .contentType("application/json")
                            .content("{\"otp\":\"" + invalidOtp + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));
        }

        assertThat(jdbcTemplate.queryForObject("select count(*) from pending_registrations", Integer.class)).isZero();
    }

    @Test
    void rejectsAndRemovesAnExpiredOtp() throws Exception {
        MvcResult registration = startRegistration();
        jdbcTemplate.update("update pending_registrations set otp_expires_at = now() - interval '1 second'");
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/register/verify")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .cookie(pendingCookie(registration))
                        .contentType("application/json")
                        .content("{\"otp\":\"" + registrationMailAdapter.lastOtp() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_EXPIRED"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from pending_registrations", Integer.class)).isZero();
    }

    @Test
    void replacesTheOtpOnResendAndRejectsAFourthResendWithinTheHour() throws Exception {
        MvcResult registration = startRegistration();
        String initialOtp = registrationMailAdapter.lastOtp();

        for (int attempt = 0; attempt < 3; attempt++) {
            MvcResult csrf = csrf();
            mockMvc.perform(post("/api/auth/register/resend-otp")
                            .cookie(csrfCookie(csrf))
                            .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                            .header("X-Forwarded-For", clientAddress)
                            .cookie(pendingCookie(registration)))
                    .andExpect(status().isAccepted())
                    .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                    .andExpect(cookie().doesNotExist("__Host-tinyroute_access"));
        }
        assertThat(registrationMailAdapter.lastOtp()).isNotEqualTo(initialOtp);

        MvcResult csrf = csrf();
        mockMvc.perform(post("/api/auth/register/resend-otp")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .cookie(pendingCookie(registration)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void returnsTheSameAcceptedResponseAndPendingCookieForAnExistingAccountWithoutCreatingAPendingRecord() throws Exception {
        userRepository.save(User.create("user@example.com"));
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"user@example.com\",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(cookie().httpOnly("pending_registration", true))
                .andExpect(cookie().secure("pending_registration", true));

        assertThat(jdbcTemplate.queryForObject("select count(*) from pending_registrations", Integer.class)).isZero();
    }

    @Test
    void returnsASafeOtpErrorWhenThePendingRegistrationCookieIsMissing() throws Exception {
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/register/verify")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"otp\":\"000000\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_INVALID"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty());
    }

    @Test
    void retainsOnlyHashesForThePasswordOtpAndPendingCookie() throws Exception {
        MvcResult registration = startRegistration();
        String passwordHash = jdbcTemplate.queryForObject("select password_hash from pending_registrations", String.class);
        String otpHash = jdbcTemplate.queryForObject("select otp_hash from pending_registrations", String.class);
        String tokenHash = jdbcTemplate.queryForObject("select token_hash from pending_registrations", String.class);

        assertThat(passwordHash).isNotEqualTo("valid-password-12");
        assertThat(otpHash).isNotEqualTo(registrationMailAdapter.lastOtp());
        assertThat(tokenHash).isNotEqualTo(pendingCookie(registration).getValue());
    }

    @Test
    void rejectsMalformedOrUnknownRegistrationInputWithSafeFieldErrors() throws Exception {
        MvcResult csrf = csrf();

        mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"not-an-email\",\"password\":\"short\",\"unexpected\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty())
                .andExpect(jsonPath("$.error.fieldErrors").isEmpty());
    }

    private MvcResult startRegistration() throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(post("/api/auth/register")
                        .cookie(csrfCookie(csrf))
                        .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                        .header("X-Forwarded-For", clientAddress)
                        .contentType("application/json")
                        .content("{\"email\":\"  USER@Example.COM  \",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(cookie().httpOnly("pending_registration", true))
                .andExpect(cookie().secure("pending_registration", true))
                .andExpect(cookie().doesNotExist("__Host-tinyroute_access"))
                .andExpect(cookie().doesNotExist("__Host-tinyroute_refresh"))
                .andReturn();
    }

    private MvcResult csrf() throws Exception {
        return mockMvc.perform(get("/api/auth/csrf")).andReturn();
    }

    private Cookie pendingCookie(MvcResult registration) {
        String setCookie = registration.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String value = setCookie.substring("pending_registration=".length(), setCookie.indexOf(';'));
        return new Cookie("pending_registration", value);
    }

    private Cookie csrfCookie(MvcResult csrf) {
        return csrf.getResponse().getCookie("__Host-tinyroute_csrf");
    }

    private String jsonValue(MvcResult result, String field) throws Exception {
        return new tools.jackson.databind.json.JsonMapper().readTree(result.getResponse().getContentAsString())
                .get(field)
                .asString();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RegistrationMailConfiguration {

        @Bean
        @Primary
        CapturingRegistrationMailAdapter registrationMailAdapter() {
            return new CapturingRegistrationMailAdapter();
        }
    }

    static class CapturingRegistrationMailAdapter implements RegistrationMailAdapter {

        private String otp;

        @Override
        public CompletableFuture<Void> sendOtp(String email, String otp) {
            this.otp = otp;
            return CompletableFuture.completedFuture(null);
        }

        String lastOtp() {
            return otp;
        }
    }
}
