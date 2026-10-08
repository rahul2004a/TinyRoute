package com.tinyroute.repository;

import static org.assertj.core.api.Assertions.*;

import com.tinyroute.config.TestInfrastructureConfiguration;
import com.tinyroute.config.TestJwtTokenConfiguration;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("dev")
@Import({TestJwtTokenConfiguration.class, TestInfrastructureConfiguration.class})
class LinkGenerationMigrationIT {
    @Autowired private DataSource dataSource;

    @Test
    void upgradesV5WithoutChangingLegacyAliasesDestinationsOrTombstones() {
        String schema = "migration_" + UUID.randomUUID().toString().replace("-", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .target(MigrationVersion.fromVersion("5"))
                    .load()
                    .migrate();
            UUID owner = UUID.randomUUID();
            jdbc.update(
                    "insert into "
                            + schema
                            + ".users (id, email_normalized, created_at, updated_at) values (?, ?, now(), now())",
                    owner,
                    "migration@example.test");
            for (String code : java.util.List.of("zzzzzzzz", "OldCode1", "deleted-alias")) {
                jdbc.update(
                        "insert into "
                                + schema
                                + ".links (id, code, owner_id, destination_url, status, created_at, updated_at, expires_at) values (?, ?, ?, ?, 'DELETED', now(), now(), now() - interval '1 hour')",
                        UUID.randomUUID(),
                        code,
                        owner,
                        "https://example.test/docs?q=java#setup");
            }
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .load()
                    .migrate();
            assertThat(
                            jdbc.queryForList(
                                    "select code from " + schema + ".links order by code",
                                    String.class))
                    .containsExactly("OldCode1", "deleted-alias", "zzzzzzzz");
            assertThat(
                            jdbc.queryForObject(
                                    "select count(*) from "
                                            + schema
                                            + ".links where generation_value is null and status = 'DELETED' and expires_at < now() and destination_url = 'https://example.test/docs?q=java#setup'",
                                    Integer.class))
                    .isEqualTo(3);
            assertThat(
                            jdbc.queryForObject(
                                    "select coalesce(max(generation_value), 0) from "
                                            + schema
                                            + ".links",
                                    Long.class))
                    .isZero();
            jdbc.update(
                    "update "
                            + schema
                            + ".links set generation_value = 218340105584895 where code = 'OldCode1'");
            assertThat(
                            jdbc.queryForObject(
                                    "select max(generation_value) from " + schema + ".links",
                                    Long.class))
                    .isEqualTo(218_340_105_584_895L);
            assertThatThrownBy(
                            () ->
                                    jdbc.update(
                                            "update "
                                                    + schema
                                                    + ".links set generation_value = 0 where code = 'OldCode1'"))
                    .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
            assertThat(
                            jdbc.queryForObject(
                                    "select indexdef from pg_indexes where schemaname = ? and indexname = 'links_generation_value_idx'",
                                    String.class,
                                    schema))
                    .contains("generation_value DESC", "WHERE (generation_value IS NOT NULL)");
        } finally {
            jdbc.execute("drop schema if exists " + schema + " cascade");
        }
    }
}
