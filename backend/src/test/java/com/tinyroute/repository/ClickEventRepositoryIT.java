package com.tinyroute.repository;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.*;
import com.tinyroute.model.ClickMetadata;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
class ClickEventRepositoryIT {
    @Autowired ClickEventRepository events;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void concurrentEventsAndCounterCommitTogetherAndRollbackTogether() throws Exception {
        UUID owner = UUID.randomUUID(), id = UUID.randomUUID();
        String code = "evt" + id.toString().replace("-", "").substring(0, 12);
        var now = Timestamp.from(Instant.now());
        jdbc.update(
                "insert into users(id,email_normalized,token_version,created_at,updated_at) values(?,?,0,?,?)",
                owner,
                owner + "@example.com",
                now,
                now);
        jdbc.update(
                "insert into links(id,code,owner_id,destination_url,status,created_at,updated_at) values(?,?,?,'https://example.com','ACTIVE',?,?)",
                id,
                code,
                owner,
                now,
                now);
        var metadata = new ClickMetadata(null, "Desktop", "Linux", "Firefox", null, null);
        try {
            try (var executor = Executors.newFixedThreadPool(4)) {
                var futures = new java.util.ArrayList<Future<?>>();
                for (int i = 0; i < 24; i++)
                    futures.add(
                            executor.submit(
                                    () ->
                                            new TransactionTemplate(transactions)
                                                    .executeWithoutResult(
                                                            s ->
                                                                    events.record(
                                                                            code,
                                                                            Instant.now(),
                                                                            metadata))));
                for (var future : futures) future.get(10, TimeUnit.SECONDS);
            }
            new TransactionTemplate(transactions)
                    .executeWithoutResult(
                            s -> {
                                events.record(code, Instant.now(), metadata);
                                s.setRollbackOnly();
                            });
            assertThat(
                            jdbc.queryForObject(
                                    "select click_count from links where id=?", Long.class, id))
                    .isEqualTo(24);
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from click_events where link_id=?",
                                    Long.class,
                                    id))
                    .isEqualTo(24);
            jdbc.update("update users set deleted_at=? where id=?", now, owner);
            new TransactionTemplate(transactions)
                    .executeWithoutResult(s -> events.record(code, Instant.now(), metadata));
            assertThat(
                            jdbc.queryForObject(
                                    "select click_count from links where id=?", Long.class, id))
                    .isEqualTo(24);
        } finally {
            jdbc.update("delete from links where id=?", id);
            jdbc.update("delete from users where id=?", owner);
        }
    }
}
