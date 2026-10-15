package com.example.Tech.service.impl.order;

import com.example.Tech.dto.request.order.OrderStatusUpdateRequest;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.exception.BusinessException;
import com.example.Tech.exception.ErrorCode;
import com.example.Tech.service.order.AdminOrderService;
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
 * Two orders confirmed at the same time while the store has the last unit: the inventory row is locked
 * (SELECT … FOR UPDATE), so exactly one confirmation takes it and the other gets 409 INSUFFICIENT_STOCK; the stock
 * never goes below 0. Not @Transactional: the lock only matters between committed transactions, so this test
 * writes real rows in techshopping_test and deletes them afterwards.
 */
@SpringBootTest
@ActiveProfiles("test")
class OrderConfirmConcurrencyTest {

    private static final String TAG = "test-confirm-concurrency";

    @Autowired
    private AdminOrderService adminOrderService;

    @Autowired
    private JdbcTemplate jdbc;

    private Long staffId;
    private Integer storeId;
    private Long variantId;
    private final List<Long> orderIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        cleanUp();
        staffId = jdbc.queryForObject("insert into users (email, username, password_hash, fullname) "
                + "values (?, ?, '{test}hash', 'Confirm Concurrency') returning user_id", Long.class,
                TAG + "@techshopping.vn", TAG);
        jdbc.update("insert into user_roles (user_id, role_id) select ?, role_id from roles where name = 'STAFF'", staffId);
        storeId = jdbc.queryForObject("insert into stores (name, address, district, city) "
                + "values (?, '1 Đường Test', 'Quận 1', 'Hồ Chí Minh') returning store_id", Integer.class, TAG);
        Long employeeId = jdbc.queryForObject("insert into employees (user_id) values (?) returning employee_id",
                Long.class, staffId);
        jdbc.update("insert into employee_assignments (employee_id, store_id, start_date) values (?, ?, current_date)",
                employeeId, storeId);
        Integer categoryId = jdbc.queryForObject("insert into categories (name, slug) values ('Điện thoại', ?) "
                + "returning category_id", Integer.class, TAG);
        Long productId = jdbc.queryForObject("insert into products (name, slug, category_id, base_price) "
                + "values ('Điện thoại Hàng Cuối', ?, ?, 5000000) returning product_id", Long.class, TAG, categoryId);
        variantId = jdbc.queryForObject("insert into product_variants (product_id, variant_name, price) "
                + "values (?, 'Mặc định', 5000000) returning variant_id", Long.class, productId);
        jdbc.update("insert into inventory (store_id, variant_id, quantity) values (?, ?, 1)", storeId, variantId);
        for (int i = 0; i < 2; i++) {
            Long orderId = jdbc.queryForObject("insert into orders (user_id, recipient_name, recipient_phone, "
                    + "shipping_address, total_amount, status, payment_method, store_id) "
                    + "values (?, 'Nguyễn Văn An', '0901234567', '1 Lê Lợi, Quận 1', 5000000, 'PENDING', 'COD', ?) "
                    + "returning order_id", Long.class, staffId, storeId);
            jdbc.update("insert into order_items (order_id, variant_id, quantity, unit_price, discount_amount, subtotal) "
                    + "values (?, ?, 1, 5000000, 0, 5000000)", orderId, variantId);
            orderIds.add(orderId);
        }
    }

    @AfterEach
    void tearDown() {
        cleanUp();
    }

    @Test
    void twoSimultaneousConfirmations_ofTheLastUnit_onlyOneTakesIt() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Object> results = Collections.synchronizedList(new ArrayList<>());
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (Long orderId : orderIds) {
                futures.add(pool.submit(() -> {
                    start.await();
                    try {
                        adminOrderService.updateStatus(staffId, orderId,
                                new OrderStatusUpdateRequest(OrderStatus.CONFIRMED, null));
                        results.add(OrderStatus.CONFIRMED);
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

        assertThat(results).containsExactlyInAnyOrder(OrderStatus.CONFIRMED, ErrorCode.INSUFFICIENT_STOCK);
        assertThat(jdbc.queryForObject("select quantity from inventory where store_id = ? and variant_id = ?",
                Integer.class, storeId, variantId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from orders where store_id = ? and status = 'CONFIRMED'",
                Long.class, storeId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from stock_movements where store_id = ? and movement_type = 'OUT'",
                Long.class, storeId)).isEqualTo(1);
    }

    /** Removes the rows of this test (also leftovers of an interrupted run). */
    private void cleanUp() {
        String storeIds = "(select store_id from stores where name = ?)";
        jdbc.update("delete from stock_movements where store_id in " + storeIds, TAG);
        jdbc.update("delete from inventory where store_id in " + storeIds, TAG);
        jdbc.update("delete from order_items where order_id in (select order_id from orders where store_id in "
                + storeIds + ")", TAG);
        jdbc.update("delete from orders where store_id in " + storeIds, TAG);
        jdbc.update("delete from employee_assignments where store_id in " + storeIds, TAG);
        jdbc.update("delete from employees where user_id in (select user_id from users where username = ?)", TAG);
        jdbc.update("delete from stores where name = ?", TAG);
        jdbc.update("delete from users where username = ?", TAG);                // cascades user_roles
        jdbc.update("delete from products where slug = ?", TAG);                 // cascades variants
        jdbc.update("delete from categories where slug = ?", TAG);
        orderIds.clear();
    }
}
