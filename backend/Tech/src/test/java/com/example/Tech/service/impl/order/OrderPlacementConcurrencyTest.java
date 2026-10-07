package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.OrderCreateRequest;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.order.OrderService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Two "Đặt hàng" clicks at the same time must give one order: the cart row is locked (SELECT … FOR UPDATE),
 * so the second checkout waits and then finds the emptied cart (409 CART_EMPTY).
 * Not @Transactional: the lock only matters between committed transactions, so this test writes real rows
 * in techshopping_test and deletes them afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderPlacementConcurrencyTest {

    private static final String TAG = "test-order-concurrency";

    @Autowired
    private OrderService orderService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long userId;

    @BeforeEach
    void setUp() {
        cleanUp();
        userId = jdbc.queryForObject("insert into users (email, username, password_hash, fullname) "
                + "values (?, ?, '{test}hash', 'Concurrency Test') returning user_id", Long.class,
                TAG + "@techshopping.vn", TAG);
        Integer categoryId = jdbc.queryForObject("insert into categories (name, slug) values ('Điện thoại', ?) "
                + "returning category_id", Integer.class, TAG);
        Long productId = jdbc.queryForObject("insert into products (name, slug, category_id, base_price) "
                + "values ('Điện thoại Đặt Đồng Thời', ?, ?, 5000000) returning product_id", Long.class, TAG, categoryId);
        Long variantId = jdbc.queryForObject("insert into product_variants (product_id, variant_name, price) "
                + "values (?, 'Mặc định', 5000000) returning variant_id", Long.class, productId);
        Long cartId = jdbc.queryForObject("insert into carts (user_id) values (?) returning cart_id", Long.class, userId);
        jdbc.update("insert into cart_items (cart_id, variant_id, quantity) values (?, ?, 2)", cartId, variantId);
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void twoSimultaneousCheckouts_createOneOrder_theOtherGetsCartEmpty() throws Exception {
        OrderCreateRequest request = new OrderCreateRequest("Nguyễn Văn An", "0901234567", "12 Nguyễn Trãi",
                null, PaymentMethod.COD);
        CountDownLatch start = new CountDownLatch(1);
        List<Object> results = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        results.add(orderService.placeOrder(userId, request).id());
                    } catch (BusinessException e) {
                        results.add(e.getErrorCode());
                    }
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

        assertThat(results).hasSize(2);
        assertThat(results).filteredOn(Long.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(ErrorCode.CART_EMPTY::equals).hasSize(1);
        assertThat(jdbc.queryForObject("select count(*) from orders where user_id = ?", Long.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from order_items i join orders o on o.order_id = i.order_id "
                + "where o.user_id = ?", Long.class, userId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from cart_items i join carts c on c.cart_id = i.cart_id "
                + "where c.user_id = ?", Long.class, userId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from payments p join orders o on o.order_id = p.order_id "
                + "where o.user_id = ? and p.status = 'PENDING'", Long.class, userId)).isEqualTo(1);
    }

    /** Removes the rows of this test (also leftovers of an interrupted run). */
    private void cleanUp() {
        jdbc.update("delete from payments where order_id in (select o.order_id from orders o "
                + "join users u on u.user_id = o.user_id where u.username = ?)", TAG);
        jdbc.update("delete from order_items where order_id in (select o.order_id from orders o "
                + "join users u on u.user_id = o.user_id where u.username = ?)", TAG);
        jdbc.update("delete from orders where user_id in (select user_id from users where username = ?)", TAG);
        jdbc.update("delete from users where username = ?", TAG);                // cascades carts, cart_items
        jdbc.update("delete from products where slug = ?", TAG);                 // cascades variants
        jdbc.update("delete from categories where slug = ?", TAG);
    }
}
