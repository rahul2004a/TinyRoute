package com.tinyroute.cache;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.tinyroute.cache.redis.RedisShortCodeCounter;
import com.tinyroute.exception.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

@SuppressWarnings("unchecked")
class RedisShortCodeCounterTest {
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final ShortCodeCounter counter = new RedisShortCodeCounter(redis);

    @Test
    void missingReplyIsDistinctFromUnsafeState() {
        doReturn(-1L)
                .when(redis)
                .execute(any(RedisScript.class), eq(List.of("code:global")), eq(""));
        assertThat(counter.nextValueIfInitialized()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {-3, -4, 0, 218340105584896L})
    void invalidReplyCannotBecomeAPublishedCode(long reply) {
        doReturn(reply)
                .when(redis)
                .execute(any(RedisScript.class), eq(List.of("code:global")), eq(""));
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void missingRedisReplyFailsClosed() {
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void redisFailureDoesNotRestartAllocation() {
        doThrow(new DataAccessResourceFailureException("unavailable"))
                .when(redis)
                .execute(any(RedisScript.class), eq(List.of("code:global")), eq(""));
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void capacityReplyIsRecognizedWithoutWrapping() {
        doReturn(-2L)
                .when(redis)
                .execute(any(RedisScript.class), eq(List.of("code:global")), eq(""));
        assertThatThrownBy(counter::nextValueIfInitialized)
                .isInstanceOf(ShortCodeCounterExhaustedException.class);
    }

    @Test
    void recoveryCannotAcceptAMissingResult() {
        doReturn(-1L)
                .when(redis)
                .execute(any(RedisScript.class), eq(List.of("code:global")), eq("62"));
        assertThatThrownBy(() -> counter.advanceAndIncrement(62))
                .isInstanceOf(ServiceUnavailableException.class);
    }
}
