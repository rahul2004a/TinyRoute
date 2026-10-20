package com.tinyroute.controller;

import com.tinyroute.config.LinkProperties;
import com.tinyroute.config.VerificationTimingCollector;
import com.tinyroute.model.*;
import com.tinyroute.security.PasswordHasher;
import java.time.Instant;
import java.util.*;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@TestComponent
@RestController
@RequestMapping("/__verification")
public class LinkVerificationController {
    public record Account(String email, String password) {}

    public record Bootstrap(
            String apiBaseUrl, String shortBaseUrl, List<Account> accounts, List<String> codes) {}

    public record StateChange(String code, LinkStatus status, Instant expiresAt) {}

    private final JdbcTemplate jdbc;
    private final Bootstrap bootstrap;
    private final VerificationTimingCollector timings;
    private final io.micrometer.core.instrument.MeterRegistry metrics;
    private final org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
            analyticsExecutor;
    private final UUID analyticsOwner;

    public LinkVerificationController(
            JdbcTemplate jdbc,
            PasswordHasher passwords,
            LinkProperties properties,
            VerificationTimingCollector timings,
            io.micrometer.core.instrument.MeterRegistry metrics,
            org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor analyticsExecutor) {
        this.jdbc = jdbc;
        this.timings = timings;
        this.metrics = metrics;
        this.analyticsExecutor = analyticsExecutor;
        var accounts = new ArrayList<Account>();
        var owners = new ArrayList<UUID>();
        for (int i = 0; i < 3; i++) {
            UUID owner = UUID.randomUUID();
            String email = "benchmark-" + owner + "@example.com",
                    password = UUID.randomUUID().toString() + "-Password";
            jdbc.update(
                    "insert into users(id,email_normalized,token_version,created_at,updated_at) values(?,?,0,now(),now())",
                    owner,
                    email);
            jdbc.update(
                    "insert into auth_identities(id,user_id,provider,subject,secret_hash,created_at) values(?,?,'PASSWORD',?,?,now())",
                    UUID.randomUUID(),
                    owner,
                    email,
                    passwords.hash(password));
            accounts.add(new Account(email, password));
            owners.add(owner);
        }
        var codes = new ArrayList<String>();
        for (int i = 0; i < 100; i++) {
            String code = "Bench" + UUID.randomUUID().toString().replace("-", "");
            jdbc.update(
                    "insert into links(id,code,owner_id,destination_url,status,created_at,updated_at) values(?,?,?,?,'ACTIVE',now(),now())",
                    UUID.randomUUID(),
                    code,
                    owners.get(0),
                    "https://example.com/docs?q=java#setup");
            codes.add(code);
        }
        analyticsOwner = owners.getFirst();
        bootstrap =
                new Bootstrap(
                        properties.getShortBaseUrl(),
                        properties.getShortBaseUrl(),
                        List.copyOf(accounts),
                        List.copyOf(codes));
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<Bootstrap> bootstrap() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(bootstrap);
    }

    @GetMapping("/analytics-state")
    public ResponseEntity<Map<String, Long>> analyticsState() {
        var state = new LinkedHashMap<String, Long>();
        state.put(
                "events",
                jdbc.queryForObject(
                        "select count(*) from click_events e join links l on l.id=e.link_id where l.owner_id=?",
                        Long.class,
                        analyticsOwner));
        state.put(
                "lifetime",
                jdbc.queryForObject(
                        "select sum(click_count) from links where owner_id=?",
                        Long.class,
                        analyticsOwner));
        for (String kind : List.of("failed", "rejected", "stale"))
            state.put(kind, (long) metrics.counter("tinyroute.analytics." + kind).count());
        state.put(
                "maxRecordingDelayMillis",
                (long)
                        metrics.timer("tinyroute.analytics.recording.delay")
                                .max(java.util.concurrent.TimeUnit.MILLISECONDS));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(state);
    }

    @GetMapping("/runtime-state")
    public ResponseEntity<Map<String, Long>> runtimeState() {
        var state = new LinkedHashMap<String, Long>();
        state.put("sampledAtEpochMillis", System.currentTimeMillis());
        for (String kind : List.of("active", "idle", "pending", "max")) {
            var gauge = metrics.find("hikaricp.connections." + kind).gauge();
            state.put(
                    "pool"
                            + (kind.equals("max")
                                    ? "Maximum"
                                    : kind.substring(0, 1).toUpperCase(java.util.Locale.ROOT)
                                            + kind.substring(1)),
                    gauge == null ? -1L : (long) gauge.value());
        }
        state.put("analyticsActive", (long) analyticsExecutor.getActiveCount());
        state.put(
                "analyticsQueued",
                (long) analyticsExecutor.getThreadPoolExecutor().getQueue().size());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(state);
    }

    @GetMapping("/timings")
    public ResponseEntity<VerificationTimingCollector.Readout> timings() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(timings.readout());
    }

    @PostMapping("/timings/reset")
    public ResponseEntity<Void> resetTimings() {
        timings.reset();
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }

    @PostMapping("/links/state")
    public ResponseEntity<Void> state(@RequestBody StateChange change) {
        if (change.status() == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        ShortCode.forAlias(change.code());
        int updated =
                jdbc.update(
                        "update links set status=?,expires_at=?,deleted_at=case when ?='DELETED' then now() else null end,updated_at=now() where code=?",
                        change.status().name(),
                        change.expiresAt() == null
                                ? null
                                : java.sql.Timestamp.from(change.expiresAt()),
                        change.status().name(),
                        change.code());
        if (updated != 1) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
