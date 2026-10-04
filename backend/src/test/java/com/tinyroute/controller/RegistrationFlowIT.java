package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tinyroute.client.RegistrationMailAdapter;
import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({
    TestJwtTokenConfiguration.class,
    TestInfrastructureConfiguration.class,
    RegistrationFlowIT.RegistrationMailConfiguration.class
})
class RegistrationFlowIT {

    @Autowired private MockMvc mockMvc;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Autowired private UserRepository userRepository;

    @Autowired private CapturingRegistrationMailAdapter registrationMailAdapter;

    private final String clientAddress =
            "198.18."
                    + ThreadLocalRandom.current().nextInt(1, 255)
                    + "."
                    + ThreadLocalRandom.current().nextInt(1, 255);

    @AfterEach
    void removeCreatedRecords() {
        JdbcTestUtils.deleteFromTables(
                jdbcTemplate, "pending_registrations", "auth_identities", "users");
    }

    @Test
    void startsAPendingRegistrationWithoutIssuingAuthenticationCookies() throws Exception {
        MvcResult registration = startRegistration();

        assertThat(registration.getResponse().getHeader(HttpHeaders.SET_COOKIE))
                .contains("pending_registration=");
    }

    @Test
    void verifiesTheOtpOnceAndOnlyThenIssuesAuthenticationCookies() throws Exception {
        MvcResult registration = startRegistration();
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register/verify")
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
        mockMvc.perform(
                        post("/api/auth/register/verify")
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
    void returnsASafeOtpFailureWhenAnotherAccountCreationWinsTheEmailRace() throws Exception {
        MvcResult registration = startRegistration();
        userRepository.save(User.create("user@example.com"));
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register/verify")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .cookie(pendingCookie(registration))
                                .contentType("application/json")
                                .content("{\"otp\":\"" + registrationMailAdapter.lastOtp() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class))
                .isEqualTo(1);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from auth_identities", Integer.class))
                .isZero();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isZero();
    }

    @Test
    void deletesThePendingRegistrationAfterFiveInvalidOtpAttempts() throws Exception {
        MvcResult registration = startRegistration();
        String invalidOtp =
                registrationMailAdapter.lastOtp().equals("000000") ? "000001" : "000000";

        for (int attempt = 0; attempt < 5; attempt++) {
            MvcResult csrf = csrf();
            mockMvc.perform(
                            post("/api/auth/register/verify")
                                    .cookie(csrfCookie(csrf))
                                    .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                    .header("X-Forwarded-For", clientAddress)
                                    .cookie(pendingCookie(registration))
                                    .contentType("application/json")
                                    .content("{\"otp\":\"" + invalidOtp + "\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));
        }

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isZero();
    }

    @Test
    void rejectsAndRemovesAnExpiredOtp() throws Exception {
        MvcResult registration = startRegistration();
        jdbcTemplate.update(
                "update pending_registrations set otp_expires_at = now() - interval '1 second'");
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register/verify")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .cookie(pendingCookie(registration))
                                .contentType("application/json")
                                .content("{\"otp\":\"" + registrationMailAdapter.lastOtp() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("OTP_INVALID"));

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isZero();
    }

    @Test
    void replacesTheOtpOnResendAndRejectsAFourthResendWithinTheHour() throws Exception {
        MvcResult registration = startRegistration();
        String initialOtp = registrationMailAdapter.lastOtp();

        for (int attempt = 0; attempt < 3; attempt++) {
            MvcResult csrf = csrf();
            mockMvc.perform(
                            post("/api/auth/register/resend-otp")
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
        mockMvc.perform(
                        post("/api/auth/register/resend-otp")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .cookie(pendingCookie(registration)))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void
            returnsTheSameAcceptedResponseAndPendingCookieForAnExistingAccountWithoutCreatingAPendingRecord()
                    throws Exception {
        userRepository.save(User.create("user@example.com"));
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"user@example.com\",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(cookie().httpOnly("pending_registration", true))
                .andExpect(cookie().secure("pending_registration", true));

        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isZero();
    }

    @Test
    void knownAndNewEmailHaveTheSameRegistrationAndResendResponses() throws Exception {
        userRepository.save(User.create("known@example.com"));
        MvcResult knownRegistration = register("known@example.com");
        MvcResult newRegistration = register("new@example.com");

        assertThat(knownRegistration.getResponse().getStatus())
                .isEqualTo(newRegistration.getResponse().getStatus());
        assertThat(knownRegistration.getResponse().getContentAsString())
                .isEqualTo(newRegistration.getResponse().getContentAsString());
        assertThat(knownRegistration.getResponse().getCookie("pending_registration").isHttpOnly())
                .isTrue();
        assertThat(newRegistration.getResponse().getCookie("pending_registration").isHttpOnly())
                .isTrue();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isEqualTo(1);

        MvcResult knownResend = resend(knownRegistration);
        MvcResult newResend = resend(newRegistration);
        assertThat(knownResend.getResponse().getStatus()).isEqualTo(202);
        assertThat(knownResend.getResponse().getStatus())
                .isEqualTo(newResend.getResponse().getStatus());
        assertThat(knownResend.getResponse().getContentAsString())
                .isEqualTo(newResend.getResponse().getContentAsString());
        assertThat(knownResend.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(newResend.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isEqualTo(1);

        MvcResult knownVerify = verifyInvalidOtp(knownRegistration);
        MvcResult newVerify = verifyInvalidOtp(newRegistration);
        assertThat(knownVerify.getResponse().getStatus()).isEqualTo(400);
        assertThat(jsonValue(knownVerify, "error", "code"))
                .isEqualTo(jsonValue(newVerify, "error", "code"))
                .isEqualTo("OTP_INVALID");
    }

    @Test
    void knownAndExpiredNewRegistrationCookiesHaveTheSameResendAndVerifyResponses()
            throws Exception {
        userRepository.save(User.create("known@example.com"));
        MvcResult knownRegistration = register("known@example.com");
        MvcResult newRegistration = register("new@example.com");
        jdbcTemplate.update(
                "update pending_registrations set otp_expires_at = now() - interval '1 second'");

        MvcResult knownResend = resend(knownRegistration);
        MvcResult expiredResend = resend(newRegistration);
        assertThat(knownResend.getResponse().getStatus()).isEqualTo(202);
        assertThat(knownResend.getResponse().getStatus())
                .isEqualTo(expiredResend.getResponse().getStatus());
        assertThat(knownResend.getResponse().getContentAsString())
                .isEqualTo(expiredResend.getResponse().getContentAsString());
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from pending_registrations", Integer.class))
                .isZero();

        MvcResult knownVerify = verifyInvalidOtp(knownRegistration);
        MvcResult expiredVerify = verifyInvalidOtp(newRegistration);
        assertThat(knownVerify.getResponse().getStatus()).isEqualTo(400);
        assertThat(jsonValue(knownVerify, "error", "code"))
                .isEqualTo(jsonValue(expiredVerify, "error", "code"))
                .isEqualTo("OTP_INVALID");
    }

    @Test
    void limitsResendsByClientEvenWhenThePendingCookieChanges() throws Exception {
        String uniqueClientAddress =
                "198."
                        + ThreadLocalRandom.current().nextInt(1, 255)
                        + "."
                        + ThreadLocalRandom.current().nextInt(1, 255)
                        + "."
                        + ThreadLocalRandom.current().nextInt(1, 255);
        String uniquePendingPrefix = java.util.UUID.randomUUID().toString();
        for (int attempt = 0; attempt < 10; attempt++) {
            MvcResult csrf = csrf();
            mockMvc.perform(
                            post("/api/auth/register/resend-otp")
                                    .cookie(csrfCookie(csrf))
                                    .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                    .with(
                                            request -> {
                                                request.setRemoteAddr(uniqueClientAddress);
                                                return request;
                                            })
                                    .cookie(
                                            new Cookie(
                                                    "pending_registration",
                                                    uniquePendingPrefix + "-" + attempt)))
                    .andExpect(status().isAccepted());
        }

        MvcResult csrf = csrf();
        mockMvc.perform(
                        post("/api/auth/register/resend-otp")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .with(
                                        request -> {
                                            request.setRemoteAddr(uniqueClientAddress);
                                            return request;
                                        })
                                .cookie(
                                        new Cookie(
                                                "pending_registration",
                                                uniquePendingPrefix + "-extra")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    @Test
    void returnsASafeOtpErrorWhenThePendingRegistrationCookieIsMissing() throws Exception {
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register/verify")
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
        String passwordHash =
                jdbcTemplate.queryForObject(
                        "select password_hash from pending_registrations", String.class);
        String otpHash =
                jdbcTemplate.queryForObject(
                        "select otp_hash from pending_registrations", String.class);
        String tokenHash =
                jdbcTemplate.queryForObject(
                        "select token_hash from pending_registrations", String.class);

        assertThat(passwordHash).isNotEqualTo("valid-password-12");
        assertThat(otpHash).isNotEqualTo(registrationMailAdapter.lastOtp());
        assertThat(tokenHash).isNotEqualTo(pendingCookie(registration).getValue());
    }

    @Test
    void rejectsMalformedOrUnknownRegistrationInputWithSafeFieldErrors() throws Exception {
        MvcResult csrf = csrf();

        mockMvc.perform(
                        post("/api/auth/register")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"not-an-email\",\"password\":\"short\",\"unexpected\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.error.requestId").isNotEmpty())
                .andExpect(jsonPath("$.error.fieldErrors").isEmpty());
    }

    private MvcResult startRegistration() throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(
                        post("/api/auth/register")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\"  USER@Example.COM  \",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PENDING_VERIFICATION"))
                .andExpect(cookie().httpOnly("pending_registration", true))
                .andExpect(cookie().secure("pending_registration", true))
                .andExpect(cookie().doesNotExist("__Host-tinyroute_access"))
                .andExpect(cookie().doesNotExist("__Host-tinyroute_refresh"))
                .andReturn();
    }

    private MvcResult register(String email) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(
                        post("/api/auth/register")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .contentType("application/json")
                                .content(
                                        "{\"email\":\""
                                                + email
                                                + "\",\"password\":\"valid-password-12\"}"))
                .andExpect(status().isAccepted())
                .andReturn();
    }

    private MvcResult resend(MvcResult registration) throws Exception {
        MvcResult csrf = csrf();
        return mockMvc.perform(
                        post("/api/auth/register/resend-otp")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .cookie(pendingCookie(registration)))
                .andReturn();
    }

    private MvcResult verifyInvalidOtp(MvcResult registration) throws Exception {
        MvcResult csrf = csrf();
        String invalidOtp =
                "000000".equals(registrationMailAdapter.lastOtp()) ? "000001" : "000000";
        return mockMvc.perform(
                        post("/api/auth/register/verify")
                                .cookie(csrfCookie(csrf))
                                .header("X-CSRF-TOKEN", jsonValue(csrf, "csrfToken"))
                                .header("X-Forwarded-For", clientAddress)
                                .cookie(pendingCookie(registration))
                                .contentType("application/json")
                                .content("{\"otp\":\"" + invalidOtp + "\"}"))
                .andReturn();
    }

    private MvcResult csrf() throws Exception {
        return mockMvc.perform(get("/api/auth/csrf")).andReturn();
    }

    private Cookie pendingCookie(MvcResult registration) {
        String setCookie = registration.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String value =
                setCookie.substring("pending_registration=".length(), setCookie.indexOf(';'));
        return new Cookie("pending_registration", value);
    }

    private Cookie csrfCookie(MvcResult csrf) {
        return csrf.getResponse().getCookie("__Host-tinyroute_csrf");
    }

    private String jsonValue(MvcResult result, String field) throws Exception {
        return new tools.jackson.databind.json.JsonMapper()
                .readTree(result.getResponse().getContentAsString())
                .get(field)
                .asString();
    }

    private String jsonValue(MvcResult result, String parent, String field) throws Exception {
        return new tools.jackson.databind.json.JsonMapper()
                .readTree(result.getResponse().getContentAsString())
                .get(parent)
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
