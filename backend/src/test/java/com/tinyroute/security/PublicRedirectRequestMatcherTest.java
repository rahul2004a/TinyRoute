package com.tinyroute.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockHttpServletRequest;

class PublicRedirectRequestMatcherTest {
    private final PublicRedirectRequestMatcher matcher = new PublicRedirectRequestMatcher();

    @ParameterizedTest
    @ValueSource(
            strings = {"/Abc", "/abc", "/Abc/", "/Abc/def", "/%41bc", "/a", "/login", "/Abc.foo"})
    void admitsPublicCandidatesForSafeApplicationValidation(String path) {
        var request = new MockHttpServletRequest("GET", path);
        request.setServerName("wrong.example");
        assertThat(matcher.matches(request)).isTrue();
        request.setMethod("HEAD");
        assertThat(matcher.matches(request)).isTrue();
        request.setMethod("POST");
        assertThat(matcher.matches(request)).isFalse();
        request.setMethod("GET");
        request.setDispatcherType(DispatcherType.ERROR);
        assertThat(matcher.matches(request)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "/api",
                "/api/links",
                "/api/auth/me",
                "/actuator",
                "/actuator/health",
                "/error",
                "/error/nested",
                "/%61pi/auth/me",
                "/%61pi",
                "/%65rror",
                "/%61ctuator",
                "/api%2fauth%2fme",
                "/Abc//def",
                "/"
            })
    void neverOpensPrivateNamespacesOrAmbiguousNestedPaths(String path) {
        assertThat(matcher.matches(new MockHttpServletRequest("GET", path))).isFalse();
    }
}
