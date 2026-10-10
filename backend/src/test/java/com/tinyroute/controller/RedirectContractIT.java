package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class RedirectContractIT extends LinkHttpTestSupport {
    String code() {
        return "R" + UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void exactStoredLocationAndCaseSensitiveCodesNeedNoAccountAndIgnoreQuery() throws Exception {
        String upper = row(code(), "ACTIVE", null),
                lower = row(upper.toLowerCase(), "ACTIVE", null);
        String exact = "https://example.com/a%2Fb?q=java%2B21&v=%25#setup%20here";
        jdbc.update("update links set destination_url=? where code=?", exact, upper);
        mvc.perform(
                        redirect("/" + upper)
                                .queryParam("ignored", "value")
                                .cookie(new Cookie("__Host-tinyroute_access", "invalid")))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", exact))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().string(""));
        mvc.perform(redirect("/" + lower))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/docs?q=java#setup"));
        mvc.perform(head("/" + upper).secure(true).header("X-Forwarded-For", clientAddress))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", exact))
                .andExpect(content().string(""));
        assertThat(
                        jdbc.queryForObject(
                                "select sum(click_count) from links where owner_id=?",
                                Long.class,
                                ownerId))
                .isZero();
    }

    @Test
    void inactiveUnknownAndCaseMismatchAreSafeAndNeverDiscloseDestinationOrOwner()
            throws Exception {
        String active = row(code(), "ACTIVE", null);
        safe(redirect("/" + active.toLowerCase()), 404, "Link not found");
        safe(redirect("/" + code()), 404, "Link not found");
        safe(redirect("/" + row(code(), "DISABLED", null)), 403, "Link unavailable");
        safe(redirect("/" + row(code(), "DELETED", null)), 404, "Link not found");
        safe(
                redirect("/" + row(code(), "ACTIVE", Instant.now().minusSeconds(1))),
                404,
                "Link not found");
        safe(
                redirect("/" + row(code(), "DISABLED", Instant.now().minusSeconds(1))),
                404,
                "Link not found");
        String marker = row(code(), "ACTIVE", null);
        jdbc.update("update links set deleted_at=now() where code=?", marker);
        safe(redirect("/" + marker), 404, "Link not found");
        safe(
                head("/" + row(code(), "DISABLED", null))
                        .secure(true)
                        .header("X-Forwarded-For", clientAddress),
                403,
                null);
    }

    @Test
    void hostAndLiteralPathAreCheckedBeforeResolution() throws Exception {
        String code = row(code(), "ACTIVE", null);
        safe(
                redirect("/" + code)
                        .with(
                                r -> {
                                    r.setServerName("api.other.example");
                                    return r;
                                }),
                404,
                "Link not found");
        for (String path :
                new String[] {
                    "/" + code + "/", "/" + code + "/nested", "/login", "/a", "/" + code + ".foo"
                }) safe(redirect(path), 404, "Link not found");
        var encoded =
                get("/" + code)
                        .secure(true)
                        .with(
                                r -> {
                                    r.setRequestURI("/%52" + code.substring(1));
                                    return r;
                                })
                        .header("X-Forwarded-For", clientAddress);
        safe(encoded, 404, "Link not found");
        for (String path : new String[] {"/" + code + ";x=1", "/" + code + "%2Fextra"})
            mvc.perform(redirect(path))
                    .andExpect(status().isBadRequest())
                    .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void cachedExpiryStopsRedirectingAndCreationWorksOnTheNextRequest() throws Exception {
        String created =
                json.readTree(
                                mvc.perform(
                                                creation(
                                                        "{\"destinationUrl\":\"https://example.com/docs?q=java#setup\"}",
                                                        access))
                                        .andExpect(status().isCreated())
                                        .andReturn()
                                        .getResponse()
                                        .getContentAsString())
                        .get("code")
                        .asString();
        mvc.perform(redirect("/" + created)).andExpect(status().isFound());
        String expiring = row(code(), "ACTIVE", Instant.now().plusMillis(800));
        mvc.perform(redirect("/" + expiring)).andExpect(status().isFound());
        Thread.sleep(900);
        safe(redirect("/" + expiring), 404, "Link not found");
    }

    @Test
    void getAndHeadShareTheCapWhileAnotherVisitorStillRedirects() throws Exception {
        String code = row(code(), "ACTIVE", null);
        for (int i = 0; i < 600; i++)
            mvc.perform(
                            i % 2 == 0
                                    ? redirect("/" + code)
                                    : head("/" + code)
                                            .secure(true)
                                            .header("X-Forwarded-For", clientAddress))
                    .andExpect(status().isFound());
        var response = safe(redirect("/" + code), 429, "Too many requests");
        assertThat(Integer.parseInt(response.getHeader("Retry-After"))).isBetween(1, 60);
        mvc.perform(get("/" + code).secure(true).header("X-Forwarded-For", "198.19.0.1"))
                .andExpect(status().isFound());
    }

    org.springframework.mock.web.MockHttpServletResponse safe(
            MockHttpServletRequestBuilder request, int expected, String message) throws Exception {
        var response =
                mvc.perform(request)
                        .andExpect(status().is(expected))
                        .andExpect(header().doesNotExist("Location"))
                        .andExpect(header().string("Cache-Control", "no-store"))
                        .andReturn()
                        .getResponse();
        assertThat(response.getContentAsString())
                .doesNotContain("example.com", ownerId.toString(), "destination", "token");
        if (message != null) assertThat(response.getContentAsString()).contains(message);
        else assertThat(response.getContentAsString()).isEmpty();
        return response;
    }
}
