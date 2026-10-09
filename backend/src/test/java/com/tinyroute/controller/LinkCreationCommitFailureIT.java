package com.tinyroute.controller;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class LinkCreationCommitFailureIT extends LinkHttpTestSupport {
    @Autowired PlatformTransactionManager transactions;

    @BeforeEach
    void rejectOnlyThisOwnersLinkAtCommit() {
        // Testcontainers only: insertion succeeds, then PostgreSQL rejects COMMIT.
        jdbc.execute(
                """
                create function test_link_commit_failure() returns trigger language plpgsql as $$
                begin
                    if NEW.owner_id = TG_ARGV[0]::uuid then
                        raise exception 'private commit failure' using errcode = '40001';
                    end if;
                    return NEW;
                end;
                $$
                """);
        jdbc.execute(
                """
                create constraint trigger test_link_commit_failure after insert on links
                deferrable initially deferred for each row
                execute function test_link_commit_failure('%s')
                """
                        .formatted(ownerId));
    }

    @AfterEach
    void removeCommitFailureFixture() {
        jdbc.execute("drop trigger if exists test_link_commit_failure on links");
        jdbc.execute("drop function if exists test_link_commit_failure()");
    }

    @Test
    void anActualRollbackAtTheCommitBoundaryCannotReturnCreationSuccess() throws Exception {
        var insertFinished = new AtomicBoolean();
        assertThatThrownBy(
                        () ->
                                new TransactionTemplate(transactions)
                                        .executeWithoutResult(
                                                status -> {
                                                    row("CommitProbe", "ACTIVE", null);
                                                    insertFinished.set(true);
                                                }))
                .rootCause()
                .isInstanceOf(SQLException.class)
                .hasMessageContaining("private commit failure");
        assertThat(insertFinished.get()).isTrue();

        var result =
                mvc.perform(creation("{\"destinationUrl\":\"https://example.com\"}", access))
                        .andExpect(status().isServiceUnavailable())
                        .andExpect(jsonPath("$.error.code").value("SERVICE_UNAVAILABLE"))
                        .andExpect(header().doesNotExist("Location"))
                        .andReturn();
        assertThat(result.getResponse().getContentAsString())
                .doesNotContain(
                        "private commit failure", "https://example.com", ownerId.toString());
        assertThat(
                        jdbc.queryForObject(
                                "select count(*) from links where owner_id=?",
                                Integer.class,
                                ownerId))
                .isZero();
    }
}
