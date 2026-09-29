package com.tinyroute.controller;

import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.client.GoogleOAuthClient;
import com.tinyroute.exception.OAuthFailedException;
import com.tinyroute.model.GoogleIdentity;
import com.tinyroute.model.OAuthTransaction;
import com.tinyroute.model.User;
import com.tinyroute.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import jakarta.servlet.http.Cookie;
import java.net.URI;
import java.util.concurrent.ThreadLocalRandom;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "tinyroute.rate-limit.trusted-proxy-cidrs=127.0.0.1/32")
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, GoogleOidcTest.GoogleOAuthClientConfiguration.class})
class GoogleOidcTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void removeCreatedRecords() {
        JdbcTestUtils.deleteFromTables(jdbcTemplate, "auth_identities", "users");
    }

    @Test
    void startsGoogleAuthorizationWithANoncePkceChallengeAndShortLivedStateCookie() throws Exception {
        mockMvc.perform(get("/api/auth/google/start").header("X-Forwarded-For", clientAddress()))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("https://accounts.google.com/")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("response_type=code")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code_challenge_method=S256")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("code_challenge=")))
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("nonce=")))
                .andExpect(cookie().httpOnly("oauth_state", true))
                .andExpect(cookie().secure("oauth_state", true))
                .andExpect(cookie().maxAge("oauth_state", 600));
    }

    @Test
    void rejectsACallbackWithoutTheMatchingStateCookieAtTheFixedFailureDestination() throws Exception {
        mockMvc.perform(get("/api/auth/google/callback")
                        .param("code", "provider-code")
                        .param("state", "attacker-state"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"))
                .andExpect(cookie().maxAge("oauth_state", 0));
    }

    @Test
    void completesAValidatedGoogleCallbackWithSessionCookiesAndTheFixedSuccessDestination() throws Exception {
        MvcResult authorization = startGoogleAuthorization(clientAddress());
        Cookie stateCookie = authorization.getResponse().getCookie("oauth_state");

        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", "valid-provider-code")
                        .param("state", stateCookie.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/links"))
                .andExpect(cookie().httpOnly("__Host-tinyroute_access", true))
                .andExpect(cookie().httpOnly("__Host-tinyroute_refresh", true))
                .andExpect(cookie().maxAge("oauth_state", 0));
    }

    @Test
    void consumesEachAuthorizationTransactionOnlyOnce() throws Exception {
        MvcResult authorization = startGoogleAuthorization(clientAddress());
        Cookie stateCookie = authorization.getResponse().getCookie("oauth_state");

        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", "valid-provider-code")
                        .param("state", stateCookie.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/links"));

        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", "valid-provider-code")
                        .param("state", stateCookie.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"));
    }

    @Test
    void clearsThePendingTransactionWhenAStateCookieDoesNotMatchTheCallbackState() throws Exception {
        MvcResult authorization = startGoogleAuthorization(clientAddress());
        Cookie stateCookie = authorization.getResponse().getCookie("oauth_state");

        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", "valid-provider-code")
                        .param("state", "mismatched-state"))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"));

        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", "valid-provider-code")
                        .param("state", stateCookie.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"));
    }

    @Test
    void rejectsProviderValidationFailuresAndEmailCollisionsWithoutAutoLinking() throws Exception {
        MvcResult rejectedAuthorization = startGoogleAuthorization(clientAddress());
        Cookie rejectedState = rejectedAuthorization.getResponse().getCookie("oauth_state");
        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(rejectedState)
                        .param("code", "rejected-provider-code")
                        .param("state", rejectedState.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"))
                .andExpect(cookie().maxAge("oauth_state", 0));

        userRepository.save(User.create("google@example.com"));
        MvcResult collisionAuthorization = startGoogleAuthorization(clientAddress());
        Cookie collisionState = collisionAuthorization.getResponse().getCookie("oauth_state");
        mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(collisionState)
                        .param("code", "valid-provider-code")
                        .param("state", collisionState.getValue()))
                .andExpect(status().isSeeOther())
                .andExpect(header().string("Location", "https://localhost:3000/login?error=oauth_failed"))
                .andExpect(cookie().doesNotExist("__Host-tinyroute_access"));

        assertThat(jdbcTemplate.queryForObject("select count(*) from auth_identities", Integer.class)).isZero();
    }

    @Test
    void serializesConcurrentCallbacksThatClaimTheSameEmailWithoutReturningAServerError() throws Exception {
        assertConcurrentCallbacksAreSafe("first-subject", "second-subject");
    }

    @Test
    void serializesConcurrentCallbacksThatClaimTheSameProviderSubjectWithoutReturningAServerError() throws Exception {
        assertConcurrentCallbacksAreSafe("shared-subject-first-email", "shared-subject-second-email");
    }

    private void assertConcurrentCallbacksAreSafe(String firstCode, String secondCode) throws Exception {
        MvcResult firstAuthorization = startGoogleAuthorization(clientAddress());
        MvcResult secondAuthorization = startGoogleAuthorization(clientAddress());
        Cookie firstState = firstAuthorization.getResponse().getCookie("oauth_state");
        Cookie secondState = secondAuthorization.getResponse().getCookie("oauth_state");
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<MvcResult> first = executor.submit(() -> concurrentCallback(firstState, firstCode, ready, start));
            Future<MvcResult> second = executor.submit(() -> concurrentCallback(secondState, secondCode, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            List<Integer> statuses = List.of(
                    first.get(10, TimeUnit.SECONDS).getResponse().getStatus(),
                    second.get(10, TimeUnit.SECONDS).getResponse().getStatus()
            );
            assertThat(statuses).containsOnly(303);
        }

        assertThat(jdbcTemplate.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("select count(*) from auth_identities", Integer.class)).isEqualTo(1);
    }

    private MvcResult concurrentCallback(
            Cookie stateCookie,
            String code,
            CountDownLatch ready,
            CountDownLatch start
    ) throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Concurrent OAuth callbacks did not start");
        }
        return mockMvc.perform(get("/api/auth/google/callback")
                        .cookie(stateCookie)
                        .param("code", code)
                        .param("state", stateCookie.getValue()))
                .andReturn();
    }

    private MvcResult startGoogleAuthorization(String clientAddress) throws Exception {
        return mockMvc.perform(get("/api/auth/google/start").header("X-Forwarded-For", clientAddress))
                .andExpect(status().isFound())
                .andReturn();
    }

    private String clientAddress() {
        return "198.18." + ThreadLocalRandom.current().nextInt(1, 255)
                + "." + ThreadLocalRandom.current().nextInt(1, 255);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class GoogleOAuthClientConfiguration {

        @Bean
        @Primary
        GoogleOAuthClient googleOAuthClient() {
            return new GoogleOAuthClient() {
                @Override
                public URI authorizationUri(String state, OAuthTransaction transaction) {
                    return URI.create("https://accounts.google.com/o/oauth2/v2/auth?response_type=code"
                            + "&state=" + state
                            + "&nonce=" + transaction.nonce()
                            + "&code_challenge=placeholder"
                            + "&code_challenge_method=S256");
                }

                @Override
                public GoogleIdentity exchangeAuthorizationCode(String code, OAuthTransaction transaction) {
                    if ("rejected-provider-code".equals(code)) {
                        throw new OAuthFailedException();
                    }
                    String subject = switch (code) {
                        case "first-subject", "second-subject" -> code;
                        case "shared-subject-first-email", "shared-subject-second-email" -> "shared-subject";
                        default -> "google-subject-123";
                    };
                    String email = switch (code) {
                        case "shared-subject-first-email" -> "first-google@example.com";
                        case "shared-subject-second-email" -> "second-google@example.com";
                        default -> "google@example.com";
                    };
                    return new GoogleIdentity(subject, email);
                }
            };
        }
    }
}
