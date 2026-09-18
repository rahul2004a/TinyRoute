package com.tinyroute.security;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityCookieTest {

    private final AuthCookieService cookies = new AuthCookieService();

    @Test
    void issuesHostOnlySecureHttpOnlyLaxAuthCookies() {
        ResponseCookie accessCookie = cookies.accessCookie("access-token");
        ResponseCookie refreshCookie = cookies.refreshCookie("refresh-token");

        assertThat(accessCookie.getName()).isEqualTo("__Host-tinyroute_access");
        assertThat(accessCookie.getValue()).isEqualTo("access-token");
        assertThat(accessCookie.isHttpOnly()).isTrue();
        assertThat(accessCookie.isSecure()).isTrue();
        assertThat(accessCookie.getSameSite()).isEqualTo("Lax");
        assertThat(accessCookie.getPath()).isEqualTo("/");
        assertThat(accessCookie.getDomain()).isNull();
        assertThat(accessCookie.getMaxAge()).isEqualTo(Duration.ofMinutes(15));

        assertThat(refreshCookie.getName()).isEqualTo("__Host-tinyroute_refresh");
        assertThat(refreshCookie.getMaxAge()).isEqualTo(Duration.ofDays(30));
        assertThat(refreshCookie.getDomain()).isNull();
    }

    @Test
    void clearsAuthCookiesUsingTheSameHostOnlyAttributes() {
        ResponseCookie clearedAccessCookie = cookies.clearAccessCookie();
        ResponseCookie clearedRefreshCookie = cookies.clearRefreshCookie();

        assertThat(clearedAccessCookie.getName()).isEqualTo("__Host-tinyroute_access");
        assertThat(clearedRefreshCookie.getName()).isEqualTo("__Host-tinyroute_refresh");
        assertThat(clearedAccessCookie.getMaxAge()).isZero();
        assertThat(clearedRefreshCookie.getMaxAge()).isZero();
        assertThat(clearedAccessCookie.isSecure()).isTrue();
        assertThat(clearedRefreshCookie.isHttpOnly()).isTrue();
        assertThat(clearedAccessCookie.getSameSite()).isEqualTo("Lax");
        assertThat(clearedRefreshCookie.getPath()).isEqualTo("/");
        assertThat(clearedRefreshCookie.getDomain()).isNull();
    }
}
