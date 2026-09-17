package com.tinyroute.cache;

import com.tinyroute.model.RefreshSessionRotation;

import java.util.UUID;

public interface RefreshSessionStore {

    void create(String tokenHash, UUID userId);

    RefreshSessionRotation rotate(String currentTokenHash, String replacementTokenHash);

    void deleteCurrent(String tokenHash);

    void deleteAllByUserId(UUID userId);
}
