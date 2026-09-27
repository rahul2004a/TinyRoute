package com.tinyroute.repository.jpa;

import com.tinyroute.model.Link;
import com.tinyroute.repository.LinkRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface JpaLinkRepository extends JpaRepository<Link, UUID>, LinkRepository {
    @Override
    @Modifying(flushAutomatically = true)
    @Query("""
            update Link link
               set link.status = com.tinyroute.model.LinkStatus.DELETED,
                   link.deletedAt = coalesce(link.deletedAt, :now),
                   link.updatedAt = :now
             where link.owner.id = :ownerId
            """)
    int tombstoneAllByOwnerId(@Param("ownerId") UUID ownerId, @Param("now") Instant now);

    @Override
    @Query(value = """
            select code
              from links
             where owner_id = :ownerId
               and code > :afterCode
             order by code asc
             limit 100
            """, nativeQuery = true)
    List<String> findDeletionCleanupCodes(
            @Param("ownerId") UUID ownerId,
            @Param("afterCode") String afterCode
    );
}
