package com.tinyroute.controller;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.model.*;
import com.tinyroute.service.*;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class RedirectController {
    private final RedirectService redirects;
    private final RateLimitService limits;
    private final LinkProperties properties;
    private final RedirectPageRenderer pages;

    public RedirectController(
            RedirectService redirects,
            RateLimitService limits,
            LinkProperties properties,
            RedirectPageRenderer pages) {
        this.redirects = redirects;
        this.limits = limits;
        this.properties = properties;
        this.pages = pages;
    }

    /** Spring's catch-all also makes literal malformed candidates a safe 404. */
    @GetMapping("/{*path}")
    public ResponseEntity<String> redirect(@PathVariable String path, HttpServletRequest request) {
        final ShortCode code;
        try {
            if (!path.startsWith("/")
                    || !request.getRequestURI().equals(path)
                    || !properties
                            .shortHost()
                            .equals(DestinationUrl.canonicalHost(request.getServerName())))
                return page(404, request, null);
            code = ShortCode.forAlias(path.substring(1));
        } catch (IllegalArgumentException e) {
            return page(404, request, null);
        }
        try {
            var limit = limits.allowRedirect(request);
            if (!limit.allowed()) return page(429, request, limit.retryAfter());
            var outcome = redirects.resolve(code);
            if (outcome.kind() == RedirectOutcome.Kind.REDIRECT) {
                var headers = headers();
                headers.set("Location", outcome.destinationUrl());
                return new ResponseEntity<>("", headers, HttpStatus.FOUND);
            }
            int status =
                    switch (outcome.kind()) {
                        case NOT_FOUND -> 404;
                        case UNAVAILABLE -> 403;
                        default -> 503;
                    };
            return page(status, request, null);
        } catch (ServiceUnavailableException e) {
            return page(503, request, null);
        }
    }

    private ResponseEntity<String> page(int status, HttpServletRequest request, Duration retry) {
        var headers = headers();
        headers.setContentType(MediaType.TEXT_HTML);
        if (retry != null)
            headers.set(
                    "Retry-After",
                    Long.toString(Math.max(1, retry.getSeconds() + (retry.getNano() > 0 ? 1 : 0))));
        return new ResponseEntity<>(
                "HEAD".equals(request.getMethod()) ? "" : pages.render(status),
                headers,
                HttpStatusCode.valueOf(status));
    }

    private HttpHeaders headers() {
        var headers = new HttpHeaders();
        headers.setCacheControl("no-store");
        return headers;
    }
}
