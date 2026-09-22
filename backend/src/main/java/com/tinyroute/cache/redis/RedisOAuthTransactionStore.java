package com.tinyroute.cache.redis;

import com.tinyroute.cache.OAuthTransactionStore;
import com.tinyroute.model.OAuthTransaction;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class RedisOAuthTransactionStore implements OAuthTransactionStore {

    private static final Duration TRANSACTION_TTL = Duration.ofMinutes(10);
    private static final String KEY_PREFIX = "oauth-transaction:";

    private final StringRedisTemplate redisTemplate;

    public RedisOAuthTransactionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void create(String stateHash, OAuthTransaction transaction) {
        Boolean created = redisTemplate.opsForValue().setIfAbsent(
                key(stateHash), transaction.nonce() + ":" + transaction.pkceVerifier(), TRANSACTION_TTL
        );
        if (!Boolean.TRUE.equals(created)) {
            throw new IllegalStateException("OAuth transaction state collision");
        }
    }

    @Override
    public Optional<OAuthTransaction> consume(String stateHash) {
        String value = redisTemplate.opsForValue().getAndDelete(key(stateHash));
        if (value == null) {
            return Optional.empty();
        }
        String[] values = value.split(":", -1);
        if (values.length != 2 || values[0].isBlank() || values[1].isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new OAuthTransaction(values[0], values[1]));
    }

    private String key(String stateHash) {
        return KEY_PREFIX + stateHash;
    }
}
