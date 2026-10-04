package com.tinyroute.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
@Transactional
class LinkRepositoryIT {
    @Autowired private LinkRepository linkRepository;

    @Autowired private JdbcTemplate jdbcTemplate;

    @Test
    void bulkTombstonesOwnedLinksAndReadsCleanupCodesInBoundedPages() {
        UUID ownerId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-09-25T00:00:00Z");
        jdbcTemplate.update(
                """
                insert into users (id, email_normalized, token_version, created_at, updated_at)
                values (?, ?, 0, ?, ?)
                """,
                ownerId,
                "owner-" + ownerId + "@example.com",
                Timestamp.from(createdAt),
                Timestamp.from(createdAt));
        for (int index = 0; index < 150; index++) {
            jdbcTemplate.update(
                    """
                    insert into links
                        (id, code, owner_id, destination_url, status, click_count, created_at, updated_at)
                    values (?, ?, ?, ?, 'ACTIVE', 0, ?, ?)
                    """,
                    UUID.randomUUID(),
                    "code-%03d".formatted(index),
                    ownerId,
                    "https://example.org/" + index,
                    Timestamp.from(createdAt),
                    Timestamp.from(createdAt));
        }
        Instant deletedAt = createdAt.plusSeconds(60);

        int tombstoned = linkRepository.tombstoneAllByOwnerId(ownerId, deletedAt);
        var firstBatch = linkRepository.findDeletionCleanupCodes(ownerId, "");
        var secondBatch = linkRepository.findDeletionCleanupCodes(ownerId, firstBatch.getLast());

        assertThat(tombstoned).isEqualTo(150);
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select count(*) from links where owner_id = ? and status = 'DELETED'",
                                Integer.class,
                                ownerId))
                .isEqualTo(150);
        assertThat(firstBatch).hasSize(100).isSorted();
        assertThat(secondBatch).hasSize(50).isSorted();
        assertThat(firstBatch.getLast()).isLessThan(secondBatch.getFirst());
        assertThat(
                        jdbcTemplate.queryForObject(
                                "select indexdef from pg_indexes where indexname = 'links_owner_code_idx'",
                                String.class))
                .contains("(owner_id, code)");
    }
}
