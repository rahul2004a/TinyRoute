package com.tinyroute.cache;

import com.tinyroute.model.OAuthTransaction;

import java.util.Optional;

public interface OAuthTransactionStore {

    void create(String stateHash, OAuthTransaction transaction);

    Optional<OAuthTransaction> consume(String stateHash);
}
