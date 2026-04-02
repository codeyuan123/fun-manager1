package com.fundmanager.service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class RemoteValueCacheService {

    private final StringRedisTemplate redisTemplate;
    private final Map<String, LocalCacheValue> localCache = new ConcurrentHashMap<>();

    public RemoteValueCacheService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public String getOrLoad(String key, Duration ttl, Supplier<String> loader) {
        String redisValue = readRedis(key);
        if (redisValue != null) {
            localCache.put(key, new LocalCacheValue(redisValue, Instant.now().plus(ttl)));
            return redisValue;
        }

        LocalCacheValue cached = localCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(Instant.now())) {
            return cached.value();
        }

        try {
            String loaded = loader.get();
            if (StringUtils.hasText(loaded)) {
                writeRedis(key, loaded, ttl);
                localCache.put(key, new LocalCacheValue(loaded, Instant.now().plus(ttl)));
            }
            return loaded;
        } catch (RuntimeException ex) {
            if (cached != null && StringUtils.hasText(cached.value())) {
                return cached.value();
            }
            throw ex;
        }
    }

    private String readRedis(String key) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private void writeRedis(String key, String value, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (RuntimeException ignored) {
        }
    }

    private record LocalCacheValue(String value, Instant expiresAt) {
    }
}
