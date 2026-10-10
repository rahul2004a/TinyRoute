package com.tinyroute.cache.redis;

import com.tinyroute.cache.ShortCodeCounter;
import com.tinyroute.exception.ServiceUnavailableException;
import com.tinyroute.exception.ShortCodeCounterExhaustedException;
import com.tinyroute.model.GeneratedShortCode;
import java.util.List;
import java.util.Objects;
import java.util.OptionalLong;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

@Component
public class RedisShortCodeCounter implements ShortCodeCounter {
    private static final DefaultRedisScript<Long> ALLOCATE =
            new DefaultRedisScript<>(
                    """
        local maximum = 218340105584895
        local function parse(value)
          if #value > 15 then return nil end
          if value ~= '0' and not string.match(value, '^[1-9]%d*$') then return nil end
          local number = tonumber(value)
          if not number or number < 0 or number > maximum then return nil end
          return number
        end
        local floor = nil
        if ARGV[1] ~= '' then
          floor = parse(ARGV[1])
          if not floor then return -3 end
        end
        local raw = redis.call('GET', KEYS[1])
        local current = 0
        if raw then
          current = parse(raw)
          if not current or redis.call('PTTL', KEYS[1]) ~= -1 then return -3 end
        elseif not floor then
          return -1
        end
        local advance = floor and floor > current
        local value = advance and floor or current
        if value >= maximum then return -2 end
        if not raw or advance then redis.call('SET', KEYS[1], ARGV[1]) end
        return redis.call('INCR', KEYS[1])
        """,
                    Long.class);
    private final StringRedisTemplate redis;

    public RedisShortCodeCounter(StringRedisTemplate redis) {
        this.redis = Objects.requireNonNull(redis);
    }

    @Override
    public OptionalLong nextValueIfInitialized() {
        long value = allocate("");
        return value == -1 ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Override
    public long advanceAndIncrement(long committedFloor) {
        if (committedFloor < 0 || committedFloor > GeneratedShortCode.MAX_VALUE)
            throw new ServiceUnavailableException();
        long value = allocate(Long.toString(committedFloor));
        if (value == -1) throw new ServiceUnavailableException();
        return value;
    }

    private long allocate(String floor) {
        final Long value;
        try {
            value = redis.execute(ALLOCATE, List.of("code:global"), floor);
        } catch (DataAccessException failure) {
            throw new ServiceUnavailableException(failure);
        }
        if (value == null) throw new ServiceUnavailableException();
        if (value == -2) throw new ShortCodeCounterExhaustedException();
        if (value == -1 || (value >= 1 && value <= GeneratedShortCode.MAX_VALUE)) return value;
        throw new ServiceUnavailableException();
    }
}
