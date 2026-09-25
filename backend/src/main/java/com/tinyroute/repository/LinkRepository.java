package com.tinyroute.repository;

import com.tinyroute.model.Link;

import java.util.List;
import java.util.UUID;

public interface LinkRepository {
    List<Link> findAllByOwnerId(UUID ownerId);
}
