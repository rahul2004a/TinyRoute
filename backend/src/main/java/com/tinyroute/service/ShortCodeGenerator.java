package com.tinyroute.service;

import com.tinyroute.cache.ShortCodeCounter;
import com.tinyroute.exception.*;
import com.tinyroute.model.GeneratedShortCode;
import com.tinyroute.repository.LinkRepository;
import java.util.Objects;
import org.springframework.dao.DataAccessException;

public final class ShortCodeGenerator {
    private final ShortCodeCounter counter;
    private final LinkRepository links;
    private final ShortCodeEncoder encoder;

    public ShortCodeGenerator(
            ShortCodeCounter counter, LinkRepository links, ShortCodeEncoder encoder) {
        this.counter = Objects.requireNonNull(counter);
        this.links = Objects.requireNonNull(links);
        this.encoder = Objects.requireNonNull(encoder);
    }

    public GeneratedShortCode nextCandidate() {
        return allocate(false);
    }

    public GeneratedShortCode nextCandidateAfterConflict() {
        return allocate(true);
    }

    private GeneratedShortCode allocate(boolean recover) {
        try {
            long value;
            if (recover) value = counter.advanceAndIncrement(committedFloor());
            else {
                var allocated = counter.nextValueIfInitialized();
                value =
                        allocated.isPresent()
                                ? allocated.getAsLong()
                                : counter.advanceAndIncrement(committedFloor());
            }
            if (value < 1 || value > GeneratedShortCode.MAX_VALUE)
                throw new ServiceUnavailableException();
            return new GeneratedShortCode(encoder.encode(value), value);
        } catch (ShortCodeCounterExhaustedException failure) {
            throw new CodeAllocationFailedException();
        } catch (DataAccessException failure) {
            throw new ServiceUnavailableException(failure);
        }
    }

    private long committedFloor() {
        long floor = links.findMaxGenerationValue();
        if (floor < 0 || floor > GeneratedShortCode.MAX_VALUE)
            throw new ServiceUnavailableException();
        return floor;
    }
}
