package com.example.Tech.repository.order;

import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.OrderStatusHistory;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks the OrderStatusHistory mapping against the real PostgreSQL test database (techshopping_test).
 * Schema only for now (v3 survey, 2026-10-05): no service writes rows here yet, this will be done by
 * application code (OrderServiceImpl / AdminOrderServiceImpl) when the order's status changes, not by a
 * Postgres trigger as in docs/techshopping_v3.sql.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderStatusHistoryRepositoryTest {

    @Autowired
    private OrderStatusHistoryRepository historyRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager entityManager;

    private User customer;
    private User staff;
    private Order order;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(user("osh-test-customer"));
        staff = userRepository.save(user("osh-test-staff"));

        Category category = new Category();
        category.setName("OSH Test Điện thoại");
        category.setSlug("test-osh-dien-thoai");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("OSH Test Sản phẩm");
        product.setSlug("test-osh-san-pham");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        productRepository.save(product);

        order = orderRepository.save(order(customer));
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_setsGeneratedIdAndTimestamp_nullOldStatusAllowed() {
        OrderStatusHistory saved = historyRepository.save(
                new OrderStatusHistory(orderRepository.getReferenceById(order.getId()), null, OrderStatus.PENDING, null));
        entityManager.flush();
        entityManager.clear();

        OrderStatusHistory found = historyRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getOldStatus()).isNull();
        assertThat(found.getNewStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(found.getChangedBy()).isNull();
        assertThat(found.getChangedAt()).isNotNull();
    }

    @Test
    void findAllByOrder_IdOrderByChangedAtAsc_returnsOnlyThatOrdersRows_inOrder() throws InterruptedException {
        Order other = orderRepository.save(order(customer));
        entityManager.flush();

        historyRepository.save(new OrderStatusHistory(
                orderRepository.getReferenceById(order.getId()), null, OrderStatus.PENDING, null));
        entityManager.flush();
        Thread.sleep(5);
        historyRepository.save(new OrderStatusHistory(
                orderRepository.getReferenceById(order.getId()), OrderStatus.PENDING, OrderStatus.CONFIRMED,
                userRepository.getReferenceById(staff.getId())));
        historyRepository.save(new OrderStatusHistory(
                orderRepository.getReferenceById(other.getId()), null, OrderStatus.PENDING, null));
        entityManager.flush();
        entityManager.clear();

        List<OrderStatusHistory> history = historyRepository.findAllByOrder_IdOrderByChangedAtAscIdAsc(order.getId());

        assertThat(history).extracting(OrderStatusHistory::getNewStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.CONFIRMED);
        assertThat(history.get(1).getChangedBy().getId()).isEqualTo(staff.getId());
    }

    private Order order(User owner) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(owner.getId()));
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName("OSH Test Nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("1 Đường Test, Quận 1, TP. Hồ Chí Minh");
        order.setTotalAmount(new BigDecimal("10000000"));
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod(PaymentMethod.COD);
        return order;
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("OSH Test User");
        return user;
    }
}
