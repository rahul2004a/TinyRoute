package com.tinyroute.cache;

import com.tinyroute.model.RefreshSessionRotation;
import com.tinyroute.model.RefreshSession;

import java.util.Optional;
import java.util.UUID;

public interface RefreshSessionStore {

    void create(String tokenHash, UUID userId, int tokenVersion, UUID accessTokenId);

    Optional<RefreshSession> find(String tokenHash);

    Optional<String> findFamilyId(String tokenHash);

    RefreshSessionRotation rotate(String currentTokenHash, String replacementTokenHash, UUID replacementAccessTokenId);

    void deleteCurrent(String tokenHash, UUID userId, UUID accessTokenId);

    void deleteAllByUserId(UUID userId);
}
