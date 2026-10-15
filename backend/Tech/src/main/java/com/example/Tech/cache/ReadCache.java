package com.example.Tech.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * JSON read cache in Redis: {@code cache:<name>:<version>:<key>}. Evicting a whole cache bumps its version
 * ({@code cache:<name>:ver}); the old entries just expire. Evictions run after the surrounding transaction commits,
 * so a reader cannot re-fill the cache with the old row. Any Redis or JSON error falls back to the database (logged
 * only): the cache can never take the shop down.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReadCache {

    public static final String CATEGORIES = "categories";
    public static final String BRANDS = "brands";
    public static final String CATEGORY_BRANDS = "category-brands";
    public static final String PRODUCT = "product";
    public static final String VARIANTS = "variants";
    public static final String IMAGES = "images";
    public static final String SPECS = "specs";
    public static final String REPORT = "report";

    private static final String PREFIX = "cache:";

    private final StringRedisTemplate redisTemplate;
    private final JsonMapper jsonMapper;
    private final CacheProperties properties;

    /** The cached value, else {@code loader}'s (stored for the cache's time to live). A null result is not stored. */
    public <T> T get(String name, Object key, TypeReference<T> type, Supplier<T> loader) {
        if (!properties.enabled()) {
            return loader.get();
        }
        String redisKey;
        try {
            redisKey = PREFIX + name + ":" + version(name) + ":" + key;
            String json = redisTemplate.opsForValue().get(redisKey);
            if (json != null) {
                return jsonMapper.readValue(json, type);
            }
        } catch (RuntimeException e) {
            log.warn("Cache read skipped ({} {}): {}", name, key, e.toString());
            return loader.get();
        }
        T value = loader.get();
        if (value != null) {
            try {
                redisTemplate.opsForValue().set(redisKey, jsonMapper.writeValueAsString(value), ttl(name));
            } catch (RuntimeException e) {
                log.warn("Cache write skipped ({} {}): {}", name, key, e.toString());
            }
        }
        return value;
    }

    /** Cache key of a page request: page, size and sort. */
    public static String pageKey(Pageable pageable) {
        return pageable.getPageNumber() + ":" + pageable.getPageSize() + ":" + pageable.getSort();
    }

    /** Drops one entry. */
    public void evict(String name, Object key) {
        afterCommit(() -> redisTemplate.delete(PREFIX + name + ":" + version(name) + ":" + key));
    }

    /** Drops every entry of these caches. */
    public void evictAll(String... names) {
        afterCommit(() -> {
            for (String name : names) {
                redisTemplate.opsForValue().increment(PREFIX + name + ":ver");
            }
        });
    }

    private String version(String name) {
        String version = redisTemplate.opsForValue().get(PREFIX + name + ":ver");
        return version == null ? "0" : version;
    }

    private Duration ttl(String name) {
        return switch (name) {
            case CATEGORIES, BRANDS, CATEGORY_BRANDS -> properties.catalogTtl();
            case REPORT -> properties.reportTtl();
            default -> properties.productTtl();
        };
    }

    private void afterCommit(Runnable eviction) {
        if (!properties.enabled()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    run(eviction);
                }
            });
        } else {
            run(eviction);
        }
    }

    private void run(Runnable eviction) {
        try {
            eviction.run();
        } catch (RuntimeException e) {
            log.warn("Cache eviction failed: {}", e.toString());
        }
    }
}
