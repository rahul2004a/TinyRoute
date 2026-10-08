package com.tinyroute.controller;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.dto.*;
import com.tinyroute.exception.RateLimitExceededException;
import com.tinyroute.model.*;
import com.tinyroute.service.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/links")
public class LinkController {
    private final LinkService links;
    private final RateLimitService limits;
    private final LinkProperties properties;

    public LinkController(LinkService links, RateLimitService limits, LinkProperties properties) {
        this.links = links;
        this.limits = limits;
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<CreateLinkResponse> create(
            @AuthenticationPrincipal AccessToken principal,
            @RequestBody CreateLinkRequest request) {
        var decision = limits.allowCreation(principal.userId());
        if (!decision.allowed()) throw new RateLimitExceededException(decision.retryAfter());
        var created =
                links.create(
                        principal,
                        new CreateLinkCommand(
                                request.destinationUrl(), request.alias(), request.expiresAt()));
        return ResponseEntity.status(201)
                .header("Cache-Control", "no-store")
                .body(
                        new CreateLinkResponse(
                                created.id(),
                                created.code().value(),
                                properties.shortUrl(created.code()),
                                created.destinationUrl().value(),
                                created.createdAt(),
                                created.expiresAt()));
    }
}
