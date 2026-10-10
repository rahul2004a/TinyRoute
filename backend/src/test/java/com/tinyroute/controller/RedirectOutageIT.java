package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.tinyroute.cache.*;
import com.tinyroute.repository.*;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

class RedirectOutageIT extends LinkHttpTestSupport {
    @MockitoBean RedirectCache cache;
    @MockitoBean RateLimitStore rateLimits;
    @MockitoBean JwtRevocationStore revocationStore;
    @MockitoSpyBean UserRepository users;
    @MockitoSpyBean LinkRepository links;

    @Test
    void redisAndAuthenticationStoreOutagesStillAllowKnownAnonymousRedirects() throws Exception {
        String code = row("R" + UUID.randomUUID().toString().replace("-", ""), "ACTIVE", null);
        when(cache.get(anyString()))
                .thenThrow(new DataAccessResourceFailureException("private cache error"));
        when(rateLimits.increment(anyString(), any()))
                .thenThrow(new DataAccessResourceFailureException("private limiter error"));
        when(revocationStore.isRevoked(any()))
                .thenThrow(new DataAccessResourceFailureException("private session error"));
        doThrow(new DataAccessResourceFailureException("private user error"))
                .when(users)
                .findById(any());
        mvc.perform(redirect("/" + code).cookie(access))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs?q=java#setup"));
        mvc.perform(redirect("/" + code).cookie(new Cookie("__Host-tinyroute_access", "invalid")))
                .andExpect(status().isFound());
        verifyNoInteractions(revocationStore);
        verify(users, never()).findById(any());
        for (int i = 2; i < 600; i++)
            mvc.perform(redirect("/" + code)).andExpect(status().isFound());
        mvc.perform(redirect("/" + code))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().doesNotExist("Location"));
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/" + code)
                                .secure(true)
                                .header("X-Forwarded-For", "198.19.1.1"))
                .andExpect(status().isFound());
    }

    @Test
    void indeterminateDatabaseStateIsSafeAndNeverProvidesALocation() throws Exception {
        when(cache.get(anyString())).thenReturn(java.util.Optional.empty());
        when(rateLimits.increment(anyString(), any()))
                .thenReturn(
                        new com.tinyroute.model.RateLimitCounter(
                                1, java.time.Duration.ofMinutes(1)));
        doThrow(new DataAccessResourceFailureException("private SQL destination owner error"))
                .when(links)
                .findRedirectStateByCode(anyString());
        var response =
                mvc.perform(redirect("/UnknownCode"))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(header().doesNotExist("Location"))
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn()
                        .getResponse();
        assertThat(response.getContentAsString())
                .contains("Service temporarily unavailable")
                .doesNotContain("private", "destination", "owner", "SQL");
    }
}
