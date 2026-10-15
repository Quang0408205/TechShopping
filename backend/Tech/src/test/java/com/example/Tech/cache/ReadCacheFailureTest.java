package com.example.Tech.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Redis down or a broken entry must never break a request: the loader (database) answers. */
@ExtendWith(MockitoExtension.class)
class ReadCacheFailureTest {

    private static final TypeReference<String> TEXT = new TypeReference<>() { };

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private ReadCache cache;

    @BeforeEach
    void setUp() {
        cache = new ReadCache(redisTemplate, JsonMapper.builder().build(),
                new CacheProperties(true, null, null, null));
    }

    @Test
    void redisDown_readsThroughToTheLoader_andEvictionsDoNotThrow() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("down"));
        AtomicInteger loads = new AtomicInteger();

        String value = cache.get("x", 1, TEXT, () -> {
            loads.incrementAndGet();
            return "from db";
        });

        assertThat(value).isEqualTo("from db");
        assertThat(loads.get()).isEqualTo(1);
        assertThatCode(() -> {
            cache.evict("x", 1);
            cache.evictAll("x", "y");
        }).doesNotThrowAnyException();
    }

    @Test
    void brokenEntry_isTreatedAsAMiss() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:x:ver")).thenReturn(null);
        when(valueOperations.get("cache:x:0:1")).thenReturn("{not json");

        assertThat(cache.get("x", 1, TEXT, () -> "from db")).isEqualTo("from db");
    }

    @Test
    void disabled_goesStraightToTheLoader() {
        ReadCache off = new ReadCache(redisTemplate, JsonMapper.builder().build(),
                new CacheProperties(false, null, null, null));

        assertThat(off.get("x", 1, TEXT, () -> "from db")).isEqualTo("from db");
        off.evict("x", 1);
        off.evictAll("x");
        verifyNoInteractions(redisTemplate);
    }
}
