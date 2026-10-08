package com.tinyroute.repository.jpa;

import com.tinyroute.model.Link;
import com.tinyroute.model.RedirectLinkState;
import com.tinyroute.repository.LinkRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface JpaLinkRepository extends JpaRepository<Link, UUID>, LinkRepository {
    @Override
    @Query(
            value =
                    "select token_version from users where id = :ownerId and deleted_at is null for update",
            nativeQuery = true)
    Optional<Integer> lockActiveOwnerTokenVersion(@Param("ownerId") UUID ownerId);

    @Override
    @Modifying
    @Query(
            value =
                    """
        insert into links (id,code,owner_id,destination_url,status,click_count,created_at,updated_at,expires_at)
        values (:id,:code,:ownerId,:destinationUrl,'ACTIVE',0,:createdAt,:createdAt,:expiresAt)
        on conflict (code) do nothing
        """,
            nativeQuery = true)
    int insertIfCodeAvailable(
            @Param("id") UUID id,
            @Param("code") String code,
            @Param("ownerId") UUID ownerId,
            @Param("destinationUrl") String destinationUrl,
            @Param("createdAt") Instant createdAt,
            @Param("expiresAt") Instant expiresAt);

    @Override
    @Query(
            """
        select new com.tinyroute.model.RedirectLinkState(link.code,link.status,link.destinationUrl,link.deletedAt,link.expiresAt)
          from Link link where link.code = :code
        """)
    Optional<RedirectLinkState> findRedirectStateByCode(@Param("code") String code);

    @Override
    @Modifying(flushAutomatically = true)
    @Query(
            """
            update Link link
               set link.status = com.tinyroute.model.LinkStatus.DELETED,
                   link.deletedAt = coalesce(link.deletedAt, :now),
                   link.updatedAt = :now
             where link.owner.id = :ownerId
            """)
    int tombstoneAllByOwnerId(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

    @Override
    @Query(
            value =
                    """
            select code
              from links
             where owner_id = :ownerId
               and code > :afterCode
             order by code asc
             limit 100
            """,
            nativeQuery = true)
    List<String> findDeletionCleanupCodes(
            @Param("ownerId") UUID ownerId, @Param("afterCode") String afterCode);
}
