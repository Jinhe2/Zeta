package com.zeta.service;

import com.zeta.config.JwtProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RefreshTokenStore {

    private static final String REDIS_PREFIX = "zeta:refresh:";

    private final StringRedisTemplate redis;
    private final long ttlSeconds;
    private final Map<String, Long> memoryStore = new ConcurrentHashMap<>();

    public RefreshTokenStore(
            @Autowired(required = false) StringRedisTemplate redis,
            JwtProperties jwtProperties) {
        this.redis = redis;
        this.ttlSeconds = jwtProperties.getExpiration().getRefresh();
    }

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        store(token, userId);
        return token;
    }

    public Optional<Long> resolve(String token) {
        if (token == null || token.isEmpty()) {
            return Optional.empty();
        }
        if (redis != null) {
            try {
                String value = redis.opsForValue().get(REDIS_PREFIX + token);
                if (value != null) {
                    return Optional.of(Long.parseLong(value));
                }
            } catch (Exception ignored) {
                // fall through
            }
        }
        return Optional.ofNullable(memoryStore.get(token));
    }

    public void revoke(String token) {
        if (token == null || token.isEmpty()) {
            return;
        }
        if (redis != null) {
            try {
                redis.delete(REDIS_PREFIX + token);
            } catch (Exception ignored) {
                // fall through
            }
        }
        memoryStore.remove(token);
    }

    /** 轮换 refreshToken：作废旧令牌并签发新令牌 */
    public String rotate(String oldToken, Long userId) {
        revoke(oldToken);
        return issue(userId);
    }

    private void store(String token, Long userId) {
        if (redis != null) {
            try {
                redis.opsForValue().set(
                        REDIS_PREFIX + token,
                        String.valueOf(userId),
                        Duration.ofSeconds(ttlSeconds));
                return;
            } catch (Exception ignored) {
                // Redis 不可用时降级内存
            }
        }
        memoryStore.put(token, userId);
    }
}
