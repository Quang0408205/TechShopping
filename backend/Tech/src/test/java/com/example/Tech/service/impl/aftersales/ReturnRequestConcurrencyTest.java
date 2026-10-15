package com.example.Tech.service.impl.aftersales;

import com.example.Tech.dto.request.aftersales.ReturnRequestCreateRequest;
import com.example.Tech.entity.aftersales.ReturnReasonType;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.service.aftersales.AfterSalesService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two return requests for the only unit of a line at the same moment: the order row lock makes the second one see
 * the first, so exactly one succeeds. Not @Transactional (the lock only matters between committed transactions).
 */
@SpringBootTest
@ActiveProfiles("test")
class ReturnRequestConcurrencyTest {

    private static final String TAG = "test-return-concurrency";

    @Autowired
    private AfterSalesService afterSalesService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;
    private Long orderId;
    private Long lineId;

    @BeforeEach
    void setUp() {
        cleanUp();
        userId = jdbc.queryForObject("insert into users (email, username, password_hash, fullname) "
                + "values (?, ?, '{test}hash', 'Return Concurrency') returning user_id", Long.class,
                TAG + "@techshopping.vn", TAG);
        Integer categoryId = jdbc.queryForObject("insert into categories (name, slug) values ('Loa', ?) "
                + "returning category_id", Integer.class, TAG);
        Long productId = jdbc.queryForObject("insert into products (name, slug, category_id, base_price) "
                + "values ('Loa Trả Cùng Lúc', ?, ?, 900000) returning product_id", Long.class, TAG, categoryId);
        Long variantId = jdbc.queryForObject("insert into product_variants (product_id, variant_name, price) "
                + "values (?, 'Mặc định', 900000) returning variant_id", Long.class, productId);
        orderId = jdbc.queryForObject("insert into orders (user_id, recipient_name, recipient_phone, shipping_address, "
                + "total_amount, status, payment_method, delivered_at) values (?, 'Khách', '0901234567', ?, 900000, "
                + "'DELIVERED', 'COD', now()) returning order_id", Long.class, userId, TAG);
        lineId = jdbc.queryForObject("insert into order_items (order_id, variant_id, quantity, unit_price, subtotal) "
                + "values (?, ?, 1, 900000, 900000) returning order_item_id", Long.class, orderId, variantId);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void twoSimultaneousReturns_ofTheOnlyUnit_onlyOneIsAccepted() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    afterSalesService.createReturnRequest(userId, new ReturnRequestCreateRequest(orderId,
                            ReturnReasonType.CHANGED_MIND, "Trả cùng lúc từ hai thiết bị.",
                            List.of(new ReturnRequestCreateRequest.Item(lineId, 1)), null));
                    return null;
                }));
            }
            start.countDown();
            int accepted = 0;
            int refused = 0;
            for (Future<?> future : futures) {
                try {
                    future.get(30, TimeUnit.SECONDS);
                    accepted++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(BusinessException.class);
                    refused++;
                }
            }
            assertThat(accepted).isEqualTo(1);
            assertThat(refused).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select coalesce(sum(quantity), 0) from return_items where order_item_id = ?",
                Integer.class, lineId)).isEqualTo(1);
    }

    /** Removes the rows of this test (also leftovers of an interrupted run). */
    private void cleanUp() {
        jdbc.update("delete from return_items where return_request_id in (select return_request_id from return_requests "
                + "where order_id in (select order_id from orders where shipping_address = ?))", TAG);
        jdbc.update("delete from return_requests where order_id in (select order_id from orders where shipping_address = ?)", TAG);
        jdbc.update("delete from orders where shipping_address = ?", TAG);
        jdbc.update("delete from product_variants where product_id in (select product_id from products where slug = ?)", TAG);
        jdbc.update("delete from products where slug = ?", TAG);
        jdbc.update("delete from categories where slug = ?", TAG);
        jdbc.update("delete from users where username = ?", TAG);
    }
}
