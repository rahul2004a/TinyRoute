package com.tinyroute.service;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.exception.*;
import com.tinyroute.model.*;
import com.tinyroute.repository.LinkRepository;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({
    TestInfrastructureConfiguration.class,
    TestJwtTokenConfiguration.class,
    LinkCreationIT.TimeConfig.class
})
class LinkCreationIT {
    static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
    @Autowired LinkService service;
    @Autowired LinkRepository links;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;
    @Autowired MutableClock clock;
    @Autowired org.springframework.data.redis.core.StringRedisTemplate redis;
    @Autowired ShortCodeEncoder encoder;
    final List<UUID> owners = new ArrayList<>();

    @BeforeEach
    void resetClock() {
        clock.now.set(NOW);
        redis.delete("code:global");
    }

    @AfterEach
    void cleanup() {
        for (UUID id : owners) {
            jdbc.update("delete from account_deletion_cleanup where user_id = ?", id);
            jdbc.update("delete from links where owner_id = ?", id);
            jdbc.update("delete from users where id = ?", id);
        }
    }

    AccessToken owner() {
        UUID id = UUID.randomUUID();
        owners.add(id);
        jdbc.update(
                "insert into users (id,email_normalized,token_version,created_at,updated_at) values (?,?,0,?,?)",
                id,
                "fixture-" + id + "@example.com",
                Timestamp.from(NOW),
                Timestamp.from(NOW));
        return new AccessToken(id, UUID.randomUUID(), NOW, NOW.plusSeconds(900), 0);
    }

    @Test
    void committedCreationIsVisibleAndKeepsExpiredAndDeletedCodesReserved() {
        var user = owner();
        String code = "test-" + UUID.randomUUID();
        var made =
                service.create(
                        user,
                        new CreateLinkCommand("https://example.com/docs?q=java#setup", code, null));
        assertThat(links.findRedirectStateByCode(code).orElseThrow().destinationUrl())
                .isEqualTo("https://example.com/docs?q=java#setup");
        assertThat(
                        jdbc.queryForObject(
                                "select click_count from links where id=?", Long.class, made.id()))
                .isZero();
        jdbc.update(
                "update links set expires_at=? where id=?",
                Timestamp.from(NOW.minusSeconds(1)),
                made.id());
        assertThatThrownBy(
                        () ->
                                service.create(
                                        user,
                                        new CreateLinkCommand(
                                                "https://example.com/other", code, null)))
                .isInstanceOf(AliasUnavailableException.class);
        service.tombstoneOwnedLinks(user.userId());
        assertThatThrownBy(
                        () ->
                                service.create(
                                        user,
                                        new CreateLinkCommand(
                                                "https://example.com/other", code, null)))
                .isInstanceOf(AliasUnavailableException.class);
    }

    @Test
    void twoOwnersContendingForOneAliasNeverOverwrite() {
        var first = owner();
        var second = owner();
        String code = "race-" + UUID.randomUUID();
        var ready = new CountDownLatch(2);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var futures =
                    List.of(
                            pool.submit(
                                    () ->
                                            attempt(
                                                    first,
                                                    code,
                                                    "https://example.com/one",
                                                    ready,
                                                    release)),
                            pool.submit(
                                    () ->
                                            attempt(
                                                    second,
                                                    code,
                                                    "https://example.com/two",
                                                    ready,
                                                    release)));
            await(ready);
            release.countDown();
            var results = futures.stream().map(LinkCreationIT::result).toList();
            assertThat(results).containsExactlyInAnyOrder(true, false);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from links where code=?", Integer.class, code))
                    .isEqualTo(1);
            assertThat(links.findRedirectStateByCode(code).orElseThrow().destinationUrl())
                    .isIn("https://example.com/one", "https://example.com/two");
        }
    }

    boolean attempt(
            AccessToken user,
            String code,
            String destination,
            CountDownLatch ready,
            CountDownLatch release) {
        try {
            return new TransactionTemplate(transactions)
                    .execute(
                            status -> {
                                links.lockActiveOwnerTokenVersion(user.userId());
                                ready.countDown();
                                await(release);
                                service.create(
                                        user, new CreateLinkCommand(destination, code, null));
                                return true;
                            });
        } catch (AliasUnavailableException expected) {
            return false;
        }
    }

    @Test
    void caseVariantsAreDistinctAndAnUnknownVariantDoesNotResolve() {
        var user = owner();
        String suffix = UUID.randomUUID().toString();
        service.create(
                user, new CreateLinkCommand("https://example.com/one", "Abc-" + suffix, null));
        assertThat(links.findRedirectStateByCode("abc-" + suffix)).isEmpty();
        service.create(
                user, new CreateLinkCommand("https://example.com/two", "abc-" + suffix, null));
        assertThat(links.findRedirectStateByCode("Abc-" + suffix).orElseThrow().destinationUrl())
                .isEqualTo("https://example.com/one");
    }

    @Test
    void deletionWinningTheOwnerLockPreventsCreation() {
        var user = owner();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var deletion =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .execute(
                                                    status -> {
                                                        links.lockActiveOwnerTokenVersion(
                                                                user.userId());
                                                        locked.countDown();
                                                        await(release);
                                                        jdbc.update(
                                                                "update users set deleted_at=?,token_version=1 where id=?",
                                                                Timestamp.from(NOW),
                                                                user.userId());
                                                        service.tombstoneOwnedLinks(user.userId());
                                                        return true;
                                                    }));
            await(locked);
            var creation =
                    pool.submit(
                            () ->
                                    service.create(
                                            user,
                                            new CreateLinkCommand(
                                                    "https://example.com",
                                                    "delete-" + UUID.randomUUID(),
                                                    null)));
            release.countDown();
            result(deletion);
            assertThatThrownBy(() -> creation.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(AuthenticationFailedException.class);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from links where owner_id=?",
                                    Integer.class,
                                    user.userId()))
                    .isZero();
        }
    }

    @Test
    void deletionFollowingUncommittedCreationIncludesTheNewLink() {
        var user = owner();
        var inserted = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var creation =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .execute(
                                                    status -> {
                                                        service.create(
                                                                user,
                                                                new CreateLinkCommand(
                                                                        "https://example.com",
                                                                        "created-"
                                                                                + UUID.randomUUID(),
                                                                        null));
                                                        inserted.countDown();
                                                        await(release);
                                                        return true;
                                                    }));
            await(inserted);
            var deletion =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .execute(
                                                    status -> {
                                                        links.lockActiveOwnerTokenVersion(
                                                                user.userId());
                                                        service.tombstoneOwnedLinks(user.userId());
                                                        jdbc.update(
                                                                "update users set deleted_at=?,token_version=1 where id=?",
                                                                Timestamp.from(NOW),
                                                                user.userId());
                                                        return true;
                                                    }));
            release.countDown();
            result(creation);
            result(deletion);
            assertThat(
                            jdbc.queryForObject(
                                    "select status from links where owner_id=?",
                                    String.class,
                                    user.userId()))
                    .isEqualTo("DELETED");
        }
    }

    @Test
    void expiryReachedDuringOwnerWaitCannotBeInserted() {
        var user = owner();
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var blocker =
                    pool.submit(
                            () ->
                                    new TransactionTemplate(transactions)
                                            .execute(
                                                    status -> {
                                                        links.lockActiveOwnerTokenVersion(
                                                                user.userId());
                                                        locked.countDown();
                                                        await(release);
                                                        return true;
                                                    }));
            await(locked);
            var creation =
                    pool.submit(
                            () ->
                                    service.create(
                                            user,
                                            new CreateLinkCommand(
                                                    "https://example.com",
                                                    "expiry-" + UUID.randomUUID(),
                                                    NOW.plusSeconds(1).toString())));
            clock.now.set(NOW.plusSeconds(1));
            release.countDown();
            result(blocker);
            assertThatThrownBy(() -> creation.get(10, TimeUnit.SECONDS))
                    .hasCauseInstanceOf(LinkValidationException.class);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from links where owner_id=?",
                                    Integer.class,
                                    user.userId()))
                    .isZero();
        }
    }

    @Test
    void missingAndStaleCounterRecoverWithoutReusingExpiredOrDeletedCodes() {
        var user = owner();
        redis.opsForValue().set("code:global", "1000");
        var first =
                service.create(
                        user, new CreateLinkCommand("https://example.com/first", null, null));
        assertThat(
                        jdbc.queryForObject(
                                "select generation_value from links where id=?",
                                Long.class,
                                first.id()))
                .isEqualTo(1001);
        jdbc.update(
                "update links set expires_at=? where id=?",
                Timestamp.from(NOW.minusSeconds(1)),
                first.id());
        redis.delete("code:global");
        var second =
                service.create(
                        user, new CreateLinkCommand("https://example.com/second", null, null));
        assertThat(
                        jdbc.queryForObject(
                                "select generation_value from links where id=?",
                                Long.class,
                                second.id()))
                .isEqualTo(1002);
        service.tombstoneOwnedLinks(user.userId());
        redis.opsForValue().set("code:global", "1000");
        var third =
                service.create(
                        user, new CreateLinkCommand("https://example.com/third", null, null));
        assertThat(
                        jdbc.queryForObject(
                                "select generation_value from links where id=?",
                                Long.class,
                                third.id()))
                .isEqualTo(1003);
        assertThat(third.code()).isNotEqualTo(first.code()).isNotEqualTo(second.code());
        assertThat(
                        links.findRedirectStateByCode(first.code().value())
                                .orElseThrow()
                                .destinationUrl())
                .isEqualTo("https://example.com/first");
    }

    @Test
    void legacyAndAliasRowsDoNotPoisonRecoveryAndCollisionsPreserveTheirDestinations() {
        var user = owner();
        service.create(
                user, new CreateLinkCommand("https://example.com/max-alias", "zzzzzzzz", null));
        String legacy = encoder.encode(1).value();
        service.create(user, new CreateLinkCommand("https://example.com/legacy", legacy, null));
        assertThat(links.findMaxGenerationValue()).isZero();
        var made =
                service.create(user, new CreateLinkCommand("https://example.com/new", null, null));
        assertThat(
                        jdbc.queryForObject(
                                "select generation_value from links where id=?",
                                Long.class,
                                made.id()))
                .isEqualTo(2);
        assertThat(links.findRedirectStateByCode(legacy).orElseThrow().destinationUrl())
                .isEqualTo("https://example.com/legacy");
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=? and generation_value is null",
                                Integer.class,
                                user.userId()))
                .isEqualTo(2);
    }

    static <T> T result(Future<T> future) {
        try {
            return future.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    static void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }

    static class MutableClock extends Clock {
        final AtomicReference<Instant> now = new AtomicReference<>(NOW);

        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        public Clock withZone(ZoneId zone) {
            return this;
        }

        public Instant instant() {
            return now.get();
        }
    }

    @TestConfiguration
    static class TimeConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }
}
