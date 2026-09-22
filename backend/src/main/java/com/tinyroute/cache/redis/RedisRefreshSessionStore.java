package com.tinyroute.cache.redis;

import com.tinyroute.cache.RefreshSessionStore;
import com.tinyroute.model.RefreshSession;
import com.tinyroute.model.RefreshSessionRotation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Component
public class RedisRefreshSessionStore implements RefreshSessionStore {

    private static final Duration IDLE_TTL = Duration.ofDays(30);
    private static final DefaultRedisScript<Long> CREATE_SESSION = new DefaultRedisScript<>(
            "if redis.call('EXISTS', KEYS[1]) == 1 or redis.call('EXISTS', KEYS[4]) == 1 then return 0; end; "
                    + "redis.call('HSET', KEYS[1], 'userId', ARGV[1], 'familyId', ARGV[2], 'tokenVersion', ARGV[3], 'accessTokenId', ARGV[4], 'lastAccessAt', ARGV[5]); "
                    + "redis.call('PEXPIRE', KEYS[1], ARGV[6]); "
                    + "redis.call('SADD', KEYS[2], ARGV[7]); redis.call('PEXPIRE', KEYS[2], ARGV[6]); "
                    + "redis.call('SADD', KEYS[3], ARGV[7]); redis.call('PEXPIRE', KEYS[3], ARGV[6]); return 1;",
            Long.class
    );
    private static final DefaultRedisScript<List> ROTATE_SESSION = new DefaultRedisScript<>(
            "local session = redis.call('HMGET', KEYS[1], 'userId', 'familyId', 'tokenVersion'); "
                    + "if not session[1] then "
                    + "  local reusedFamily = redis.call('GET', KEYS[3]); "
                    + "  if not reusedFamily then return {0}; end; "
                    + "  local familyKey = 'refresh-family:' .. reusedFamily; "
                    + "  local hashes = redis.call('SMEMBERS', familyKey); "
                    + "  for _, hash in ipairs(hashes) do "
                    + "    local userId = redis.call('HGET', 'refresh:' .. hash, 'userId'); "
                    + "    if userId then redis.call('SREM', 'refresh-user:' .. userId, hash); end; "
                    + "    redis.call('DEL', 'refresh:' .. hash); "
                    + "  end; "
                    + "  redis.call('DEL', familyKey); return {2}; "
                    + "end; "
                    + "if redis.call('EXISTS', KEYS[2]) == 1 or redis.call('EXISTS', KEYS[4]) == 1 then return {0}; end; "
                    + "local userKey = 'refresh-user:' .. session[1]; local familyKey = 'refresh-family:' .. session[2]; "
                    + "redis.call('DEL', KEYS[1]); redis.call('SET', KEYS[3], session[2], 'PX', ARGV[2]); "
                    + "redis.call('HSET', KEYS[2], 'userId', session[1], 'familyId', session[2], 'tokenVersion', session[3], 'accessTokenId', ARGV[5], 'lastAccessAt', ARGV[1]); "
                    + "redis.call('PEXPIRE', KEYS[2], ARGV[2]); redis.call('SREM', userKey, ARGV[3]); redis.call('SADD', userKey, ARGV[4]); "
                    + "redis.call('PEXPIRE', userKey, ARGV[2]); redis.call('SREM', familyKey, ARGV[3]); redis.call('SADD', familyKey, ARGV[4]); "
                    + "redis.call('PEXPIRE', familyKey, ARGV[2]); return {1, session[1], session[3], session[2]};",
            List.class
    );
    private static final DefaultRedisScript<Long> DELETE_CURRENT_SESSION = new DefaultRedisScript<>(
            "local session = redis.call('HMGET', KEYS[1], 'userId', 'accessTokenId', 'familyId'); "
                    + "if not session[1] or session[1] ~= ARGV[1] or session[2] ~= ARGV[2] then return 0; end; "
                    + "redis.call('DEL', KEYS[1]); redis.call('SREM', 'refresh-user:' .. session[1], ARGV[3]); "
                    + "redis.call('SREM', 'refresh-family:' .. session[3], ARGV[3]); return 1;",
            Long.class
    );

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshSessionStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate);
    }

    @Override
    public void create(String tokenHash, UUID userId, int tokenVersion, UUID accessTokenId) {
        requireTokenHash(tokenHash);
        Objects.requireNonNull(userId);
        requireTokenVersion(tokenVersion);
        Objects.requireNonNull(accessTokenId);
        String familyId = UUID.randomUUID().toString();
        Long created = redisTemplate.execute(
                CREATE_SESSION,
                List.of(sessionKey(tokenHash), userKey(userId), familyKey(familyId), usedKey(tokenHash)),
                userId.toString(), familyId, Integer.toString(tokenVersion), accessTokenId.toString(), Instant.now().toString(),
                Long.toString(IDLE_TTL.toMillis()), tokenHash
        );
        if (!Long.valueOf(1).equals(created)) {
            throw new IllegalStateException("Refresh token hash already exists");
        }
    }

    @Override
    public Optional<RefreshSession> find(String tokenHash) {
        requireTokenHash(tokenHash);
        List<Object> values = redisTemplate.opsForHash().multiGet(
                sessionKey(tokenHash), List.of("userId", "tokenVersion", "accessTokenId", "familyId")
        );
        if (values == null || values.size() != 4 || values.stream().anyMatch(Objects::isNull)) {
            return Optional.empty();
        }
        try {
            return Optional.of(new RefreshSession(
                    UUID.fromString(values.get(0).toString()),
                    Integer.parseInt(values.get(1).toString()),
                    UUID.fromString(values.get(2).toString()),
                    values.get(3).toString()
            ));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("Redis refresh session is invalid", exception);
        }
    }

    @Override
    public Optional<String> findFamilyId(String tokenHash) {
        requireTokenHash(tokenHash);
        Optional<RefreshSession> activeSession = find(tokenHash);
        if (activeSession.isPresent()) {
            return activeSession.map(RefreshSession::familyId);
        }
        return Optional.ofNullable(redisTemplate.opsForValue().get(usedKey(tokenHash)));
    }

    @Override
    public RefreshSessionRotation rotate(String currentTokenHash, String replacementTokenHash, UUID replacementAccessTokenId) {
        requireTokenHash(currentTokenHash);
        requireTokenHash(replacementTokenHash);
        Objects.requireNonNull(replacementAccessTokenId);
        if (currentTokenHash.equals(replacementTokenHash)) {
            throw new IllegalArgumentException("Refresh token hashes must differ");
        }
        List<?> response = redisTemplate.execute(
                ROTATE_SESSION,
                List.of(sessionKey(currentTokenHash), sessionKey(replacementTokenHash), usedKey(currentTokenHash), usedKey(replacementTokenHash)),
                Instant.now().toString(), Long.toString(IDLE_TTL.toMillis()), currentTokenHash, replacementTokenHash,
                replacementAccessTokenId.toString()
        );
        if (response == null || response.isEmpty() || !(response.getFirst() instanceof Number status)) {
            throw new IllegalStateException("Redis refresh rotation response is invalid");
        }
        return switch (status.intValue()) {
            case 0 -> RefreshSessionRotation.missing();
            case 1 -> RefreshSessionRotation.rotated(new RefreshSession(
                    UUID.fromString(response.get(1).toString()),
                    Integer.parseInt(response.get(2).toString()),
                    replacementAccessTokenId,
                    response.get(3).toString()
            ));
            case 2 -> RefreshSessionRotation.reused();
            default -> throw new IllegalStateException("Redis refresh rotation response is invalid");
        };
    }

    @Override
    public void deleteCurrent(String tokenHash, UUID userId, UUID accessTokenId) {
        requireTokenHash(tokenHash);
        Objects.requireNonNull(userId);
        Objects.requireNonNull(accessTokenId);
        redisTemplate.execute(
                DELETE_CURRENT_SESSION,
                List.of(sessionKey(tokenHash)),
                userId.toString(), accessTokenId.toString(), tokenHash
        );
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        Objects.requireNonNull(userId);
        String userKey = userKey(userId);
        java.util.Set<String> tokenHashes = redisTemplate.opsForSet().members(userKey);
        if (tokenHashes == null) {
            return;
        }
        for (String tokenHash : tokenHashes) {
            RefreshSession session = find(tokenHash).orElse(null);
            if (session != null) {
                deleteCurrent(tokenHash, session.userId(), session.accessTokenId());
            }
        }
        redisTemplate.delete(userKey);
    }

    private void requireTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            throw new IllegalArgumentException("Refresh token hash is required");
        }
    }

    private void requireTokenVersion(int tokenVersion) {
        if (tokenVersion < 0) {
            throw new IllegalArgumentException("Token version must not be negative");
        }
    }

    private String sessionKey(String tokenHash) {
        return "refresh:" + tokenHash;
    }

    private String userKey(UUID userId) {
        return "refresh-user:" + userId;
    }

    private String familyKey(String familyId) {
        return "refresh-family:" + familyId;
    }

    private String usedKey(String tokenHash) {
        return "refresh-used:" + tokenHash;
    }
}
