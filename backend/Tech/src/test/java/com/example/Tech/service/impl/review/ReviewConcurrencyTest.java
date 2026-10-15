package com.example.Tech.service.impl.review;

import com.example.Tech.dto.request.review.ReviewRequest;
import com.example.Tech.service.review.ReviewService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two accounts review the same product at the same time: the product row is locked while the rating is recomputed,
 * so neither recompute misses the other review (without the lock both could store "1 review"). Not @Transactional:
 * the lock only matters between committed transactions, so the rows are real and deleted afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class ReviewConcurrencyTest {

    private static final String TAG = "test-review-concurrency";

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long productId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cleanUp();
        for (int i = 0; i < 2; i++) {
            userIds.add(jdbc.queryForObject("insert into users (email, username, password_hash, fullname) "
                    + "values (?, ?, '{test}hash', 'Review Concurrency') returning user_id", Long.class,
                    TAG + i + "@techshopping.vn", TAG + i));
        }
        Integer categoryId = jdbc.queryForObject("insert into categories (name, slug) values ('Tai nghe', ?) "
                + "returning category_id", Integer.class, TAG);
        productId = jdbc.queryForObject("insert into products (name, slug, category_id, base_price) "
                + "values ('Tai nghe Hai Người', ?, ?, 500000) returning product_id", Long.class, TAG, categoryId);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void twoSimultaneousReviews_ofOneProduct_bothCountInItsRating() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            int[] ratings = {5, 2};
            for (int i = 0; i < 2; i++) {
                Long userId = userIds.get(i);
                int rating = ratings[i];
                futures.add(pool.submit(() -> {
                    start.await();
                    reviewService.create(userId, new ReviewRequest(productId, rating, "Đánh giá cùng lúc số " + rating, null));
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        Map<String, Object> product = jdbc.queryForMap("select rating, total_reviews from products where product_id = ?",
                productId);
        assertThat(product.get("total_reviews")).isEqualTo(2);
        assertThat((BigDecimal) product.get("rating")).isEqualByComparingTo("3.50");
    }

    /** Removes the rows of this test (also leftovers of an interrupted run). */
    private void cleanUp() {
        jdbc.update("delete from reviews where product_id in (select product_id from products where slug = ?)", TAG);
        jdbc.update("delete from products where slug = ?", TAG);
        jdbc.update("delete from categories where slug = ?", TAG);
        jdbc.update("delete from users where username like ?", TAG + "%");
        userIds.clear();
    }
}
