package com.example.Tech.repository.order;

import com.example.Tech.entity.cart.Cart;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.CustomerProfile;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.cart.CartRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.CustomerProfileRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Checks the Order group mapping (incl. the recipient columns added in Phase 4) against the real PostgreSQL
 * test database (techshopping_test). Every test is rolled back.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository variantRepository;

    @Autowired
    private EntityManager entityManager;

    private User customer;
    private User otherCustomer;
    private ProductVariant black;
    private ProductVariant white;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(user("order-test"));
        otherCustomer = userRepository.save(user("order-test-2"));

        Category category = new Category();
        category.setName("Điện thoại");
        category.setSlug("test-order-dien-thoai");
        category = categoryRepository.save(category);

        Product product = new Product();
        product.setName("Điện thoại Test Đơn Hàng");
        product.setSlug("test-order-dien-thoai-test-don-hang");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("15990000"));
        product = productRepository.save(product);

        black = variantRepository.save(variant(product, "Đen 128GB", "15990000"));
        white = variantRepository.save(variant(product, "Trắng 256GB", "18990000"));

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void save_persistsTheOrderAndItsItems_enumsAsUpperCaseText() {
        Order order = order(customer, OrderStatus.PENDING);
        order.addItem(item(black, 2, "14990000", "15990000"));
        order.addItem(item(white, 1, "18990000", "18990000"));
        Long orderId = orderRepository.save(order).getId();
        entityManager.flush();
        entityManager.clear();

        Order saved = orderRepository.findById(orderId).orElseThrow();
        assertThat(saved.getRecipientName()).isEqualTo("Nguyễn Văn Nhận");
        assertThat(saved.getRecipientPhone()).isEqualTo("0901234567");
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(saved.getPaymentMethod()).isEqualTo(PaymentMethod.COD);
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("48970000");
        assertThat(saved.getTaxAmount()).isEqualByComparingTo("0");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems()).extracting(OrderItem::getQuantity).containsExactly(2, 1);
        assertThat(saved.getItems().getFirst().getDiscountAmount()).isEqualByComparingTo("2000000");

        Object[] raw = (Object[]) entityManager
                .createNativeQuery("select status, payment_method from orders where order_id = :id")
                .setParameter("id", orderId)
                .getSingleResult();
        assertThat(raw).containsExactly("PENDING", "COD");
    }

    @Test
    void findByIdAndUserId_onlyFindsTheCustomersOwnOrder() {
        Long orderId = orderRepository.save(order(customer, OrderStatus.PENDING)).getId();

        assertThat(orderRepository.findByIdAndUserId(orderId, customer.getId())).isPresent();
        assertThat(orderRepository.findByIdAndUserId(orderId, otherCustomer.getId())).isEmpty();
        assertThat(orderRepository.findByIdAndUserId(-1L, customer.getId())).isEmpty();
    }

    @Test
    void findAllByUserId_pagesTheCustomersOrdersOnly() {
        Long first = orderRepository.save(order(customer, OrderStatus.DELIVERED)).getId();
        Long second = orderRepository.save(order(customer, OrderStatus.PENDING)).getId();
        orderRepository.save(order(otherCustomer, OrderStatus.PENDING));

        Page<Order> page = orderRepository.findAllByUserId(customer.getId(),
                PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "id")));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Order::getId).containsExactly(second, first);
    }

    @Test
    void findAllWithProductByOrderIdIn_loadsItemsOfSeveralOrdersInOneQuery() {
        Order first = order(customer, OrderStatus.PENDING);
        first.addItem(item(white, 1, "18990000", "18990000"));
        first.addItem(item(black, 1, "15990000", "15990000"));
        Order second = order(customer, OrderStatus.PENDING);
        second.addItem(item(black, 3, "15990000", "15990000"));
        Long firstId = orderRepository.save(first).getId();
        Long secondId = orderRepository.save(second).getId();
        entityManager.flush();
        entityManager.clear();

        List<OrderItem> items = orderItemRepository.findAllWithProductByOrderIdIn(List.of(secondId, firstId));

        assertThat(items).extracting(item -> item.getOrder().getId()).containsExactly(firstId, firstId, secondId);
        assertThat(items).extracting(item -> item.getVariant().getId())
                .containsExactly(white.getId(), black.getId(), black.getId());
        assertThat(Hibernate.isInitialized(items.getFirst().getVariant())).isTrue();
        assertThat(Hibernate.isInitialized(items.getFirst().getVariant().getProduct())).isTrue();
        assertThat(items.getFirst().getVariant().getProduct().getName()).isEqualTo("Điện thoại Test Đơn Hàng");
        assertThat(orderItemRepository.findAllWithProductByOrderIdIn(List.of(-1L))).isEmpty();
    }

    @Test
    void findByIdForUpdate_returnsTheOrder() {
        Long orderId = orderRepository.save(order(customer, OrderStatus.CONFIRMED)).getId();
        entityManager.flush();
        entityManager.clear();

        assertThat(orderRepository.findByIdForUpdate(orderId)).get()
                .extracting(Order::getStatus).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(orderRepository.findByIdForUpdate(-1L)).isEmpty();
    }

    @Test
    void cartFindByUserIdForUpdate_returnsTheCartOrEmpty() {
        assertThat(cartRepository.findByUserIdForUpdate(customer.getId())).isEmpty();
        cartRepository.insertIfMissing(customer.getId());

        Cart cart = cartRepository.findByUserIdForUpdate(customer.getId()).orElseThrow();

        assertThat(cart.getUser().getId()).isEqualTo(customer.getId());
    }

    @Test
    void addToTotalSpent_addsToTheCustomerProfile() {
        CustomerProfile profile = new CustomerProfile(userRepository.getReferenceById(customer.getId()));
        customerProfileRepository.save(profile);
        entityManager.flush();

        assertThat(customerProfileRepository.addToTotalSpent(customer.getId(), new BigDecimal("1500000"))).isEqualTo(1);
        assertThat(customerProfileRepository.addToTotalSpent(customer.getId(), new BigDecimal("250000.50"))).isEqualTo(1);
        assertThat(customerProfileRepository.addToTotalSpent(otherCustomer.getId(), BigDecimal.TEN)).isZero();

        assertThat(customerProfileRepository.findById(customer.getId()).orElseThrow().getTotalSpent())
                .isEqualByComparingTo("1750000.50");
    }

    @Test
    void variantInAnOrder_existsByVariantId_andTheDatabaseRefusesItsHardDelete() {
        Order order = order(customer, OrderStatus.PENDING);
        order.addItem(item(black, 1, "15990000", "15990000"));
        orderRepository.save(order);
        entityManager.flush();

        assertThat(orderItemRepository.existsByVariantId(black.getId())).isTrue();
        assertThat(orderItemRepository.existsByVariantId(white.getId())).isFalse();

        // order_items.variant_id has no ON DELETE action: this is why ProductVariantService checks first (409)
        assertThatThrownBy(() -> entityManager.createNativeQuery("delete from product_variants where variant_id = :id")
                .setParameter("id", black.getId())
                .executeUpdate())
                .hasMessageContaining("order_items");
    }

    private Order order(User owner, OrderStatus status) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(owner.getId()));
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName("Nguyễn Văn Nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        order.setShippingCost(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("48970000"));
        order.setStatus(status);
        order.setPaymentMethod(PaymentMethod.COD);
        return order;
    }

    private OrderItem item(ProductVariant variant, int quantity, String unitPrice, String listPrice) {
        OrderItem item = new OrderItem();
        item.setVariant(variantRepository.getReferenceById(variant.getId()));
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setDiscountAmount(new BigDecimal(listPrice).subtract(new BigDecimal(unitPrice))
                .multiply(BigDecimal.valueOf(quantity)));
        item.setSubtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("Order Test");
        return user;
    }

    private static ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variant;
    }
}
