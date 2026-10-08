package com.tinyroute.cache;

import com.tinyroute.model.RedirectLookup;
import java.util.Optional;

public interface RedirectCache {
    Optional<RedirectLookup> get(String code);

    void put(RedirectLookup lookup);

    void evict(String code);
}
