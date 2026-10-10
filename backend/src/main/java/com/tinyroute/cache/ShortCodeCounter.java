package com.tinyroute.cache;

import java.util.OptionalLong;

/** External atomic allocator; only a missing key yields empty (FR-CRE-04). */
public interface ShortCodeCounter {
    OptionalLong nextValueIfInitialized();

    long advanceAndIncrement(long committedFloor);
}
