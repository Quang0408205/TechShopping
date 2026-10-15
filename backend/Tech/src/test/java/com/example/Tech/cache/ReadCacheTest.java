package com.example.Tech.cache;

import com.example.Tech.dto.response.common.PageResponse;
import com.example.Tech.dto.response.product.CategoryResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import tools.jackson.core.type.TypeReference;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/** The read cache against the real Redis: JSON round trip, expiry key, eviction by key and by version. */
@SpringBootTest(properties = "app.cache.enabled=true")
@ActiveProfiles("test")
class ReadCacheTest {

    private static final TypeReference<PageResponse<CategoryResponse>> PAGE = new TypeReference<>() { };

    @Autowired
    private ReadCache cache;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final String name = "test-" + UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        var keys = redisTemplate.keys("cache:" + name + ":*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
    }

    private static PageResponse<CategoryResponse> page(String categoryName) {
        CategoryResponse category = new CategoryResponse(1, categoryName, "slug", null, null, null, null, 0, true,
                LocalDateTime.of(2026, 10, 15, 8, 30), LocalDateTime.of(2026, 10, 15, 8, 30));
        return new PageResponse<>(List.of(category), 0, 20, 1, 1);
    }

    @Test
    void secondRead_comesFromRedis_andSurvivesTheJsonRoundTrip() {
        AtomicInteger loads = new AtomicInteger();

        PageResponse<CategoryResponse> first = cache.get(name, "k", PAGE, () -> {
            loads.incrementAndGet();
            return page("Điện thoại");
        });
        PageResponse<CategoryResponse> second = cache.get(name, "k", PAGE, () -> {
            loads.incrementAndGet();
            return page("khác");
        });

        assertThat(loads.get()).isEqualTo(1);
        assertThat(second).isEqualTo(first);
        assertThat(redisTemplate.getExpire("cache:" + name + ":0:k")).isPositive();
    }

    @Test
    void evictKey_dropsOnlyThatEntry() {
        cache.get(name, "a", PAGE, () -> page("A"));
        cache.get(name, "b", PAGE, () -> page("B"));

        cache.evict(name, "a");

        assertThat(cache.get(name, "a", PAGE, () -> page("A mới")).content().get(0).name()).isEqualTo("A mới");
        assertThat(cache.get(name, "b", PAGE, () -> page("B mới")).content().get(0).name()).isEqualTo("B");
    }

    @Test
    void evictAll_bumpsTheVersion_soEveryEntryIsReloaded() {
        cache.get(name, "a", PAGE, () -> page("A"));
        cache.get(name, "b", PAGE, () -> page("B"));

        cache.evictAll(name);

        assertThat(cache.get(name, "a", PAGE, () -> page("A mới")).content().get(0).name()).isEqualTo("A mới");
        assertThat(cache.get(name, "b", PAGE, () -> page("B mới")).content().get(0).name()).isEqualTo("B mới");
        assertThat(redisTemplate.opsForValue().get("cache:" + name + ":ver")).isEqualTo("1");
    }

    @Test
    void nullResults_areNotStored() {
        AtomicInteger loads = new AtomicInteger();
        for (int i = 0; i < 2; i++) {
            cache.get(name, "none", PAGE, () -> {
                loads.incrementAndGet();
                return null;
            });
        }
        assertThat(loads.get()).isEqualTo(2);
    }
}
