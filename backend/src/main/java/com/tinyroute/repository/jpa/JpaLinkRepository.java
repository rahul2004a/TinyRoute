package com.tinyroute.repository.jpa;

import com.tinyroute.model.Link;
import com.tinyroute.repository.LinkRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface JpaLinkRepository extends JpaRepository<Link, UUID>, LinkRepository {
    @Override
    @Query("select link from Link link where link.owner.id = :ownerId")
    List<Link> findAllByOwnerId(@Param("ownerId") UUID ownerId);
}
