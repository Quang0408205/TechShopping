package com.example.Tech.repository.aftersales;

import com.example.Tech.entity.aftersales.MaintenanceRequest;
import com.example.Tech.entity.aftersales.MaintenanceType;
import com.example.Tech.entity.aftersales.ReturnItem;
import com.example.Tech.entity.aftersales.ReturnReasonType;
import com.example.Tech.entity.aftersales.ReturnRequest;
import com.example.Tech.entity.aftersales.ReturnStatus;
import com.example.Tech.entity.aftersales.ServiceRequestImage;
import com.example.Tech.entity.aftersales.ServiceRequestStatus;
import com.example.Tech.entity.aftersales.Warranty;
import com.example.Tech.entity.aftersales.WarrantyRequest;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.User;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Mapping + the CHECK / partial unique constraints of migration after_sales.sql on the real test DB. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class AfterSalesRepositoryTest {

    private static final List<ServiceRequestStatus> OPEN =
            List.of(ServiceRequestStatus.PENDING, ServiceRequestStatus.RECEIVED, ServiceRequestStatus.PROCESSING);

    @Autowired private WarrantyRepository warrantyRepository;
    @Autowired private WarrantyRequestRepository warrantyRequestRepository;
    @Autowired private MaintenanceRequestRepository maintenanceRequestRepository;
    @Autowired private ReturnRequestRepository returnRequestRepository;
    @Autowired private ReturnItemRepository returnItemRepository;
    @Autowired private ServiceRequestImageRepository imageRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private EntityManager entityManager;

    private User customer;
    private Order order;
    private OrderItem phoneLine;
    private OrderItem caseLine;

    @BeforeEach
    void setUp() {
        customer = userRepository.save(user("after-sales-test"));

        Category category = new Category();
        category.setName("After Sales Test");
        category.setSlug("test-after-sales");
        category = categoryRepository.save(category);
        Product product = new Product();
        product.setName("After Sales Test Điện thoại");
        product.setSlug("test-after-sales-dien-thoai");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("10000000"));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Đen");
        variant.setPrice(new BigDecimal("10000000"));
        variant = variantRepository.save(variant);

        order = new Order();
        order.setUser(customer);
        order.setOrderDate(LocalDateTime.now());
        order.setRecipientName("Nguyễn Văn Nhận");
        order.setRecipientPhone("0901234567");
        order.setShippingAddress("12 Nguyễn Trãi, Quận 5, TP. Hồ Chí Minh");
        order.setShippingCost(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("10600000"));
        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());
        order.setPaymentMethod(PaymentMethod.COD);
        phoneLine = line(variant, 1, "10000000");
        caseLine = line(variant, 3, "200000");
        order.addItem(phoneLine);
        order.addItem(caseLine);
        order = orderRepository.save(order);
        entityManager.flush();
    }

    @Test
    void warranty_mapsAndRejectsAnEndBeforeTheStart() {
        Warranty saved = warrantyRepository.save(warranty(phoneLine, LocalDate.now(), LocalDate.now().plusMonths(12)));
        entityManager.flush();
        entityManager.clear();

        assertThat(warrantyRepository.findByOrderItem_Id(phoneLine.getId())).get()
                .satisfies(w -> {
                    assertThat(w.getId()).isEqualTo(saved.getId());
                    assertThat(w.getActive()).isTrue();
                    assertThat(w.getWarrantyType()).isEqualTo("STANDARD");
                });
        assertThatThrownBy(() -> {
            warrantyRepository.save(warranty(caseLine, LocalDate.now(), LocalDate.now().minusDays(1)));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void warrantyRequest_withPhotos_andOnlyOneOpenRequestPerWarranty() {
        Warranty warranty = warrantyRepository.save(warranty(phoneLine, LocalDate.now(), LocalDate.now().plusMonths(12)));
        WarrantyRequest first = warrantyRequest(warranty);
        first.addImage("http://localhost:8080/uploads/service/a.jpg");
        first.addImage("http://localhost:8080/uploads/service/b.jpg");
        first = warrantyRequestRepository.save(first);
        entityManager.flush();
        entityManager.clear();

        WarrantyRequest found = warrantyRequestRepository.findById(first.getId()).orElseThrow();
        assertThat(found.getStatus()).isEqualTo(ServiceRequestStatus.PENDING);
        assertThat(found.getImages()).extracting(ServiceRequestImage::getDisplayOrder).containsExactly(1, 2);
        assertThat(warrantyRequestRepository.existsByWarranty_IdAndStatusIn(warranty.getId(), OPEN)).isTrue();

        assertThatThrownBy(() -> {
            warrantyRequestRepository.save(warrantyRequest(warrantyRepository.getReferenceById(warranty.getId())));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void warrantyRequest_aClosedRequestFreesTheWarranty_rejectedNeedsAReason() {
        Warranty warranty = warrantyRepository.save(warranty(phoneLine, LocalDate.now(), LocalDate.now().plusMonths(12)));
        WarrantyRequest done = warrantyRequest(warranty);
        done.setStatus(ServiceRequestStatus.COMPLETED);
        warrantyRequestRepository.save(done);
        warrantyRequestRepository.save(warrantyRequest(warranty));
        entityManager.flush();
        assertThat(warrantyRequestRepository.count()).isGreaterThanOrEqualTo(2);

        WarrantyRequest rejected = warrantyRequest(warranty);
        rejected.setStatus(ServiceRequestStatus.REJECTED);
        assertThatThrownBy(() -> {
            warrantyRequestRepository.save(rejected);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void maintenanceRequest_typeAndCosts_oneOpenPerLine_negativeCostRefused() {
        MaintenanceRequest request = maintenance(phoneLine);
        request.setEstimatedCost(new BigDecimal("300000"));
        request = maintenanceRequestRepository.save(request);
        entityManager.flush();
        entityManager.clear();

        MaintenanceRequest found = maintenanceRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(found.getMaintenanceType()).isEqualTo(MaintenanceType.CLEANING);
        assertThat(found.getEstimatedCost()).isEqualByComparingTo("300000");
        assertThat(maintenanceRequestRepository.existsByOrderItem_IdAndStatusIn(phoneLine.getId(), OPEN)).isTrue();
        assertThat(maintenanceRequestRepository.existsByOrderItem_IdAndStatusIn(caseLine.getId(), OPEN)).isFalse();

        MaintenanceRequest negative = maintenance(caseLine);
        negative.setActualCost(new BigDecimal("-1"));
        assertThatThrownBy(() -> {
            maintenanceRequestRepository.save(negative);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void returnRequest_items_heldQuantityIgnoresRejectedAndCancelled() {
        ReturnRequest open = returnRequest(ReturnStatus.PENDING);
        open.addItem(new ReturnItem(caseLine, 2, new BigDecimal("400000")));
        open.addImage("http://localhost:8080/uploads/service/c.jpg");
        returnRequestRepository.save(open);
        ReturnRequest rejected = returnRequest(ReturnStatus.REJECTED);
        rejected.setRejectionReason("Hàng đã qua sử dụng");
        rejected.addItem(new ReturnItem(caseLine, 1, new BigDecimal("200000")));
        rejected.addItem(new ReturnItem(phoneLine, 1, new BigDecimal("10000000")));
        returnRequestRepository.save(rejected);
        entityManager.flush();
        entityManager.clear();

        Map<Long, Long> held = returnItemRepository.sumHeldQuantities(List.of(phoneLine.getId(), caseLine.getId()))
                .stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        assertThat(held).containsExactly(Map.entry(caseLine.getId(), 2L));
        assertThat(returnRequestRepository.findAllByOrder_IdOrderByCreatedAtDesc(order.getId())).hasSize(2);
        assertThat(returnRequestRepository.findById(open.getId()).orElseThrow().getImages()).hasSize(1);
    }

    @Test
    void returnRequest_sameLineTwiceInOneRequest_isRefused() {
        ReturnRequest twice = returnRequest(ReturnStatus.PENDING);
        twice.addItem(new ReturnItem(caseLine, 1, new BigDecimal("200000")));
        twice.addItem(new ReturnItem(caseLine, 1, new BigDecimal("200000")));
        assertThatThrownBy(() -> {
            returnRequestRepository.save(twice);
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void image_withoutAnOwner_isRefused() {
        assertThatThrownBy(() -> {
            imageRepository.save(new ServiceRequestImage("http://localhost:8080/uploads/service/x.jpg", 1));
            entityManager.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void serviceRequestsView_listsTheThreeTypesWithTheOrdersStore() {
        Warranty warranty = warrantyRepository.save(warranty(phoneLine, LocalDate.now(), LocalDate.now().plusMonths(12)));
        warrantyRequestRepository.save(warrantyRequest(warranty));
        maintenanceRequestRepository.save(maintenance(caseLine));
        ReturnRequest returnRequest = returnRequest(ReturnStatus.PENDING);
        returnRequest.addItem(new ReturnItem(caseLine, 1, new BigDecimal("200000")));
        returnRequestRepository.save(returnRequest);
        entityManager.flush();

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery(
                        "select request_type, status, order_item_id from service_requests_view where order_id = ?1 order by request_type")
                .setParameter(1, order.getId()).getResultList();
        assertThat(rows).extracting(row -> row[0] + "/" + row[1] + "/" + (row[2] != null))
                .containsExactly("MAINTENANCE/PENDING/true", "RETURN/PENDING/false", "WARRANTY/PENDING/true");
    }

    private OrderItem line(ProductVariant variant, int quantity, String unitPrice) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setSubtotal(new BigDecimal(unitPrice).multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private static Warranty warranty(OrderItem item, LocalDate start, LocalDate end) {
        Warranty warranty = new Warranty();
        warranty.setOrderItem(item);
        warranty.setStartDate(start);
        warranty.setEndDate(end);
        return warranty;
    }

    private WarrantyRequest warrantyRequest(Warranty warranty) {
        WarrantyRequest request = new WarrantyRequest();
        request.setWarranty(warranty);
        request.setUser(customer);
        request.setIssueDescription("Máy tự tắt nguồn khi đang sạc pin.");
        return request;
    }

    private MaintenanceRequest maintenance(OrderItem item) {
        MaintenanceRequest request = new MaintenanceRequest();
        request.setUser(customer);
        request.setOrderItem(item);
        request.setMaintenanceType(MaintenanceType.CLEANING);
        request.setDescription("Vệ sinh loa và cổng sạc.");
        return request;
    }

    private ReturnRequest returnRequest(ReturnStatus status) {
        ReturnRequest request = new ReturnRequest();
        request.setOrder(order);
        request.setUser(customer);
        request.setReasonType(ReturnReasonType.CHANGED_MIND);
        request.setReason("Không hợp màu, muốn trả lại.");
        request.setStatus(status);
        return request;
    }

    private static User user(String username) {
        User user = new User();
        user.setEmail(username + "@techshopping.vn");
        user.setUsername(username);
        user.setPasswordHash("{test}hash");
        user.setFullname("After Sales Test");
        return user;
    }
}
