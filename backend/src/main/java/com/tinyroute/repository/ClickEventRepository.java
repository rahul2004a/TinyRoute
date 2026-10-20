package com.tinyroute.repository;

import com.tinyroute.model.ClickMetadata;
import java.time.*;
import java.util.*;

public interface ClickEventRepository {
    void record(String code, Instant clickedAt, ClickMetadata metadata);

    long lifetime(UUID ownerId, UUID linkId);

    List<DailyCount> daily(UUID ownerId, UUID linkId, Instant from, Instant to);

    List<BucketCount> breakdown(
            UUID ownerId, UUID linkId, Instant from, Instant to, String dimension);

    interface DailyCount {
        LocalDate getDate();

        long getCount();
    }

    interface BucketCount {
        String getKind();

        String getLabel();

        long getCount();
    }
}
