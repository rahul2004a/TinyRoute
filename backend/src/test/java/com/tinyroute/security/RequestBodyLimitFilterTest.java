package com.tinyroute.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;

import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;

class RequestBodyLimitFilterTest {

    private final RequestBodyLimitFilter filter = new RequestBodyLimitFilter(
            new SecurityErrorResponseWriter(new ObjectMapper())
    );

    @Test
    void rejectsAnApiRequestWhoseDeclaredBodyExceedsSixteenKiB() throws Exception {
        MockHttpServletRequest request = requestWithBody(RequestBodyLimitFilter.MAX_REQUEST_BODY_BYTES + 1);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            throw new AssertionError("Oversized request must not reach the application");
        });

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("VALIDATION_ERROR");
    }

    @Test
    void rejectsAnOversizedChunkedApiRequestWhileItIsRead() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }
        };
        request.setContent(new String(
                new byte[(int) RequestBodyLimitFilter.MAX_REQUEST_BODY_BYTES + 1], StandardCharsets.UTF_8
        ).getBytes(StandardCharsets.UTF_8));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                servletRequest.getInputStream().readAllBytes());

        assertThat(response.getStatus()).isEqualTo(413);
        assertThat(response.getContentAsString()).contains("VALIDATION_ERROR");
    }

    @Test
    void allowsAnApiRequestAtTheSixteenKiBLimit() throws Exception {
        MockHttpServletRequest request = requestWithBody(RequestBodyLimitFilter.MAX_REQUEST_BODY_BYTES);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            assertThat(servletRequest.getInputStream().readAllBytes()).hasSize((int) RequestBodyLimitFilter.MAX_REQUEST_BODY_BYTES);
            ((jakarta.servlet.http.HttpServletResponse) servletResponse).setStatus(204);
        });

        assertThat(response.getStatus()).isEqualTo(204);
    }

    private MockHttpServletRequest requestWithBody(long size) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/register");
        request.setContent(new byte[(int) size]);
        return request;
    }
}
