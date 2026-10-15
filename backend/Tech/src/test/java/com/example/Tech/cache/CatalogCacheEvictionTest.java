package com.example.Tech.cache;

import com.example.Tech.dto.request.product.CategoryCreateRequest;
import com.example.Tech.dto.request.product.CategoryUpdateRequest;
import com.example.Tech.dto.response.product.CategoryResponse;
import com.example.Tech.service.product.CategoryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * With the cache on, a change committed through the service shows up in the next read, and the second read of the
 * same data is served from Redis. Not transactional on purpose: evictions run after the real commit.
 */
@SpringBootTest(properties = "app.cache.enabled=true")
@ActiveProfiles("test")
class CatalogCacheEvictionTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final List<Integer> createdIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        createdIds.forEach(id -> categoryService.delete(id, false));
    }

    @Test
    void updatingACategory_isVisibleOnTheNextRead_andReadsAreCached() {
        String slug = "test-cache-" + UUID.randomUUID().toString().substring(0, 8);
        CategoryResponse created = categoryService.create(
                new CategoryCreateRequest("Tên cũ", slug, null, null, null, 0, true));
        createdIds.add(created.id());

        assertThat(categoryService.getById(created.id()).name()).isEqualTo("Tên cũ");
        assertThat(redisTemplate.keys("cache:categories:*:" + created.id())).isNotEmpty();
        assertThat(categoryService.getById(created.id()).name()).isEqualTo("Tên cũ");

        categoryService.update(created.id(), new CategoryUpdateRequest("Tên mới", slug, null, null, null, 0, true));

        assertThat(categoryService.getById(created.id()).name()).isEqualTo("Tên mới");
    }
}
