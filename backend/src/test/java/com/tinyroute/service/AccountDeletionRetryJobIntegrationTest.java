package com.tinyroute.service;

import com.tinyroute.config.TestJwtTokenConfiguration;
import com.tinyroute.model.AccountDeletionCleanup;
import com.tinyroute.repository.AccountDeletionCleanupRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.task.scheduling.enabled=false"
)
@ActiveProfiles("dev")
@Import(TestJwtTokenConfiguration.class)
class AccountDeletionRetryJobIntegrationTest {
    @Autowired
    private AccountDeletionRetryJob retryJob;

    @Autowired
    private AccountDeletionCleanupRepository cleanupRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID ownerId;

    @BeforeEach
    void createDeletedOwnerAndLinks() {
        jdbcTemplate.update("delete from account_deletion_cleanup");
        ownerId = UUID.randomUUID();
        Instant now = Instant.now();
        jdbcTemplate.update("""
                insert into users (id, email_normalized, token_version, deleted_at, created_at, updated_at)
                values (?, ?, 1, ?, ?, ?)
                """, ownerId, "deleted-" + ownerId + "@invalid.local",
                Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        for (int index = 0; index < 101; index++) {
            jdbcTemplate.update("""
                    insert into links
                        (id, code, owner_id, destination_url, status, click_count,
                         created_at, updated_at, deleted_at)
                    values (?, ?, ?, ?, 'DELETED', 0, ?, ?, ?)
                    """, UUID.randomUUID(), "cleanup-%03d".formatted(index), ownerId,
                    "https://example.org/" + index,
                    Timestamp.from(now), Timestamp.from(now), Timestamp.from(now));
        }
        cleanupRepository.save(AccountDeletionCleanup.redirectBatch(ownerId, now));
    }

    @AfterEach
    void removeRecords() {
        jdbcTemplate.update("delete from account_deletion_cleanup where user_id = ?", ownerId);
        jdbcTemplate.update("delete from links where owner_id = ?", ownerId);
        jdbcTemplate.update("delete from users where id = ?", ownerId);
    }

    @Test
    void persistsTheCursorAcrossTransactionalInvocations() {
        retryJob.processPending();

        assertThat(jdbcTemplate.queryForObject(
                "select redirect_code from account_deletion_cleanup where user_id = ? and kind = 'REDIRECT_CACHE'",
                String.class,
                ownerId
        )).isEqualTo("cleanup-099");

        retryJob.processPending();

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from account_deletion_cleanup where user_id = ? and kind = 'REDIRECT_CACHE'",
                Integer.class,
                ownerId
        )).isZero();
    }
}
