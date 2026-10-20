package com.tinyroute.controller;

import com.tinyroute.dto.AnalyticsResponse;
import com.tinyroute.model.AccessToken;
import com.tinyroute.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@RestController
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("/api/analytics")
    public ResponseEntity<AnalyticsResponse> overview(
            @AuthenticationPrincipal AccessToken principal,
            @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(analytics.overview(principal, parameters));
    }

    @GetMapping("/api/links/{id}/analytics")
    public ResponseEntity<AnalyticsResponse> detail(
            @AuthenticationPrincipal AccessToken principal,
            @PathVariable String id,
            @RequestParam MultiValueMap<String, String> parameters) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(analytics.detail(principal, id, parameters));
    }
}
