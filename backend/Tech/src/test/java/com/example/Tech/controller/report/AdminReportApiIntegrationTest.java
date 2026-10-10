package com.example.Tech.controller.report;

import com.example.Tech.entity.employee.Employee;
import com.example.Tech.entity.order.Order;
import com.example.Tech.entity.order.OrderItem;
import com.example.Tech.entity.order.OrderStatus;
import com.example.Tech.entity.order.PaymentMethod;
import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.sales.SalesRecord;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.order.OrderRepository;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
import com.example.Tech.repository.sales.SalesRecordRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import com.example.Tech.support.StoreFixtures;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sales report + dashboard over HTTP against the test database (rolled back). The report data sits in 2031 so
 * nothing else in techshopping_test falls into the period; figures are computed by hand in the comments.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminReportApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String SALES = "/api/v1/admin/reports/sales";
    private static final String PERIOD = "?fromDate=2031-03-01&toDate=2031-04-30";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private SalesRecordRepository salesRecordRepository;
    @Autowired private EntityManager entityManager;
    @Autowired private Clock clock;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private Long customerId;
    private String customerToken;
    private String managerAToken;
    private String staffAToken;
    private String adminToken;
    private Store storeA;
    private Store storeB;
    private Employee employeeA;
    private Employee employeeB;
    private ProductVariant phone;
    private ProductVariant cover;
    private Order order1;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("rpt.khach", "Khách Báo Cáo");
        customerId = customer.get("user").get("id").asLong();
        customerToken = customer.get("accessToken").asString();
        managerAToken = registerWithRole("rpt.qla", "Quản Lý A", "STAFF");
        Long managerAId = registeredUserIds.getLast();
        staffAToken = registerWithRole("rpt.nva", "Nhân Viên A", "STAFF");
        Long staffAId = registeredUserIds.getLast();
        registerWithRole("rpt.nvb", "Nhân Viên B", "STAFF");
        Long staffBId = registeredUserIds.getLast();
        adminToken = registerWithRole("rpt.admin", "Quản Trị", "ADMIN");

        Category phones = category("Điện thoại BC", "test-rpt-phones");
        Category accessories = category("Phụ kiện BC", "test-rpt-accessories");
        phone = variant(phones, "Điện thoại Báo Cáo", "test-rpt-phone", "10000000");
        cover = variant(accessories, "Ốp lưng Báo Cáo", "test-rpt-cover", "200000");
        storeA = StoreFixtures.store(entityManager, "ZZ CN Báo Cáo A", "Quận 5");
        storeB = StoreFixtures.store(entityManager, "ZZ CN Báo Cáo B", "Quận 9");
        StoreFixtures.assign(entityManager, managerAId, storeA, "Quản lý chi nhánh");
        employeeA = StoreFixtures.assign(entityManager, staffAId, storeA, "Nhân viên bán hàng");
        employeeB = StoreFixtures.assign(entityManager, staffBId, storeB, "Thu ngân");

        // 1: A, 5/3 10:00, COD, 2 phones = 20.000.000, employee A (commission 200.000); refunded 10.000.000 on 2/4
        order1 = delivered(storeA, "2031-03-05T10:00", PaymentMethod.COD, "0", employeeA, "200000", line(phone, 2));
        refund(order1, "10000000", "2031-04-02T09:00");
        // 2: A, 5/3 15:00, bank transfer, 1 cover + 30.000 shipping = 230.000, employee A (2.300)
        delivered(storeA, "2031-03-05T15:00", PaymentMethod.BANK_TRANSFER, "30000", employeeA, "2300", line(cover, 1));
        // 3: B, 10/4, installment, 1 phone = 10.000.000, employee B (100.000)
        delivered(storeB, "2031-04-10T11:00", PaymentMethod.INSTALLMENT, "0", employeeB, "100000", line(phone, 1));
        // 4: A, 20/3, COD, 2 covers + 30.000 = 430.000, confirmed before Phase 10: no sales record
        delivered(storeA, "2031-03-20T08:00", PaymentMethod.COD, "30000", null, null, line(cover, 2));
        // 5: delivered 28/2 (outside), employee A; refunded 1.000.000 on 10/3 (inside)
        Order early = delivered(storeA, "2031-02-28T18:00", PaymentMethod.COD, "0", employeeA, "100000", line(phone, 1));
        refund(early, "1000000", "2031-03-10T10:00");
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void admin_everyStore_totalsSeriesPiesStoresEmployeesTopProducts() throws Exception {
        JsonNode report = data(send(get(SALES + PERIOD + "&groupBy=MONTH"), adminToken).andExpect(status().isOk()));

        JsonNode summary = report.get("summary");
        // 20.000.000 + 230.000 + 10.000.000 + 430.000; refunds 10.000.000 + 1.000.000
        assertMoney(summary.get("grossRevenue"), "30660000");
        assertMoney(summary.get("refundAmount"), "11000000");
        assertMoney(summary.get("netRevenue"), "19660000");
        assertThat(summary.get("deliveredOrders").asLong()).isEqualTo(4);
        assertThat(summary.get("unitsSold").asLong()).isEqualTo(6);
        assertMoney(summary.get("averageOrderValue"), "7665000");
        assertMoney(summary.get("shippingFees"), "60000");
        assertThat(summary.get("refundCount").asLong()).isEqualTo(2);
        assertMoney(summary.get("totalCommission"), "302300");
        assertThat(report.get("storeId").isNull()).isTrue();
        assertThat(report.get("groupBy").asString()).isEqualTo("MONTH");

        JsonNode series = report.get("series");
        assertThat(series).hasSize(2);
        assertThat(series.get(0).get("start").asString()).isEqualTo("2031-03-01");
        assertThat(series.get(0).get("orders").asLong()).isEqualTo(3);
        assertMoney(series.get(0).get("grossRevenue"), "20660000");
        assertMoney(series.get(0).get("refundAmount"), "1000000");
        assertMoney(series.get(1).get("netRevenue"), "0");

        assertThat(report.get("byCategory")).hasSize(2);
        assertThat(report.get("byCategory").get(0).get("categoryName").asString()).isEqualTo("Điện thoại BC");
        assertMoney(report.get("byCategory").get(0).get("revenue"), "30000000");
        assertMoney(report.get("byCategory").get(1).get("revenue"), "600000");
        assertThat(texts(report.get("byPaymentMethod"), "paymentMethod")).containsExactly("COD", "INSTALLMENT", "BANK_TRANSFER");
        assertMoney(report.get("byPaymentMethod").get(0).get("revenue"), "20430000");

        Map<Integer, JsonNode> stores = new HashMap<>();
        report.get("byStore").forEach(row -> stores.put(row.get("storeId").asInt(), row));
        assertMoney(stores.get(storeA.getId()).get("grossRevenue"), "20660000");
        assertMoney(stores.get(storeA.getId()).get("refundAmount"), "11000000");
        assertMoney(stores.get(storeA.getId()).get("netRevenue"), "9660000");
        assertMoney(stores.get(storeB.getId()).get("netRevenue"), "10000000");

        Map<String, JsonNode> employees = employeeRows(report);
        assertThat(employees.get(String.valueOf(employeeA.getId())).get("orders").asLong()).isEqualTo(2);
        assertMoney(employees.get(String.valueOf(employeeA.getId())).get("salesAmount"), "20230000");
        assertMoney(employees.get(String.valueOf(employeeA.getId())).get("commission"), "202300");
        assertMoney(employees.get(String.valueOf(employeeA.getId())).get("refundAmount"), "11000000");
        assertThat(employees.get(String.valueOf(employeeA.getId())).get("fullname").asString()).isEqualTo("Nhân Viên A");
        assertMoney(employees.get(String.valueOf(employeeB.getId())).get("commission"), "100000");
        assertThat(employees.get("none").get("orders").asLong()).isEqualTo(1);
        assertMoney(employees.get("none").get("salesAmount"), "430000");

        assertThat(texts(report.get("topProducts"), "productName")).containsExactly("Điện thoại Báo Cáo", "Ốp lưng Báo Cáo");
        assertThat(report.get("topProducts").get(1).get("units").asLong()).isEqualTo(3);
    }

    @Test
    void branchManager_onlyTheirStore_defaultDailySeries() throws Exception {
        JsonNode report = data(send(get(SALES + PERIOD), managerAToken).andExpect(status().isOk()));

        assertThat(report.get("storeId").asInt()).isEqualTo(storeA.getId());
        assertThat(report.get("storeName").asString()).isEqualTo("ZZ CN Báo Cáo A");
        assertThat(report.get("groupBy").asString()).isEqualTo("DAY");
        assertMoney(report.get("summary").get("grossRevenue"), "20660000");
        assertMoney(report.get("summary").get("netRevenue"), "9660000");
        assertThat(report.get("byStore")).isEmpty();
        assertThat(employeeRows(report).keySet()).containsExactlyInAnyOrder(String.valueOf(employeeA.getId()), "none");
        JsonNode series = report.get("series");
        assertThat(series).hasSize(61);
        assertThat(series.get(4).get("start").asString()).isEqualTo("2031-03-05");
        assertThat(series.get(4).get("orders").asLong()).isEqualTo(2);
        assertMoney(series.get(4).get("grossRevenue"), "20230000");

        // storeId of their own store is fine, another one is refused
        send(get(SALES + PERIOD + "&storeId=" + storeA.getId()), managerAToken).andExpect(status().isOk());
        send(get(SALES + PERIOD + "&storeId=" + storeB.getId()), managerAToken).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));

        // only the refund of order 5 falls in 8–12/3: employee A still gets a row, with no sale
        JsonNode refundOnly = data(send(get(SALES + "?fromDate=2031-03-08&toDate=2031-03-12"), managerAToken));
        assertMoney(refundOnly.get("summary").get("netRevenue"), "-1000000");
        JsonNode row = employeeRows(refundOnly).get(String.valueOf(employeeA.getId()));
        assertThat(row.get("orders").asLong()).isZero();
        assertThat(row.get("fullname").asString()).isEqualTo("Nhân Viên A");
        assertThat(row.get("storeName").asString()).isEqualTo("ZZ CN Báo Cáo A");
        assertMoney(row.get("refundAmount"), "1000000");
    }

    @Test
    void access_andValidation() throws Exception {
        send(get(SALES), null).andExpect(status().isUnauthorized());
        send(get(SALES), customerToken).andExpect(status().isForbidden());
        // a store employee who is not the branch manager does not see the reports
        send(get(SALES), staffAToken).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.message").value("Chỉ quản lý chi nhánh và quản trị viên xem được báo cáo"));

        send(get(SALES + "?fromDate=2031-04-02&toDate=2031-04-01"), adminToken).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.fromDate").exists());
        send(get(SALES + "?fromDate=2030-01-01&toDate=2031-04-01"), adminToken).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.toDate").exists());
        send(get(SALES + "?fromDate=2031-01-01&toDate=2031-04-01&groupBy=DAY"), adminToken)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.details.groupBy").exists());
        send(get(SALES + "?groupBy=WEEK"), adminToken).andExpect(status().isBadRequest());
        send(get(SALES + "?storeId=999999"), adminToken).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));

        // default period = the last 30 days, daily
        JsonNode defaults = data(send(get(SALES + "?storeId=" + storeB.getId()), adminToken).andExpect(status().isOk()));
        assertThat(defaults.get("series")).hasSize(30);
        assertThat(defaults.get("toDate").asString()).isEqualTo(LocalDateTime.now(clock).toLocalDate().toString());
        assertMoney(defaults.get("summary").get("grossRevenue"), "0");
    }

    @Test
    void dashboard_storeStaffSeeTheirStore_adminSeesEverything() throws Exception {
        LocalDateTime now = LocalDateTime.now(clock);
        delivered(storeA, now.withHour(0).withMinute(5).toString(), PaymentMethod.COD, "0", employeeA, "10000",
                line(cover, 5));
        Order pending = order(storeA, OrderStatus.PENDING, null, PaymentMethod.COD, "0", line(cover, 1));
        orderRepository.save(pending);
        StoreFixtures.stock(entityManager, storeA, phone, 3);
        StoreFixtures.stock(entityManager, storeA, cover, 0);
        entityManager.flush();
        entityManager.createNativeQuery("insert into maintenance_requests (user_id, order_item_id, maintenance_type, "
                        + "description, status) values (:user, :item, 'CLEANING', 'Vệ sinh máy', 'PENDING')")
                .setParameter("user", customerId).setParameter("item", order1.getItems().getFirst().getId())
                .executeUpdate();

        for (String token : List.of(staffAToken, managerAToken)) {
            JsonNode cards = data(send(get("/api/v1/admin/dashboard/summary"), token).andExpect(status().isOk()));
            assertThat(cards.get("storeId").asInt()).isEqualTo(storeA.getId());
            assertMoney(cards.get("revenueToday"), "1000000");
            assertThat(cards.get("deliveredToday").asLong()).isEqualTo(1);
            assertMoney(cards.get("revenueThisMonth"), "1000000");
            assertMoney(cards.get("revenuePreviousMonthSamePeriod"), "0");
            assertThat(cards.get("pendingOrders").asLong()).isEqualTo(1);
            assertThat(cards.get("openServiceRequests").asLong()).isEqualTo(1);
            assertThat(cards.get("lowStockVariants").asLong()).isEqualTo(1);
            assertThat(cards.get("outOfStockVariants").asLong()).isEqualTo(1);
            assertThat(cards.get("openStores").isNull()).isTrue();
        }
        JsonNode all = data(send(get("/api/v1/admin/dashboard/summary"), adminToken).andExpect(status().isOk()));
        assertThat(all.get("storeId").isNull()).isTrue();
        assertThat(all.get("openStores").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(all.get("activeEmployees").asLong()).isGreaterThanOrEqualTo(3);
        send(get("/api/v1/admin/dashboard/summary"), customerToken).andExpect(status().isForbidden());
    }

    private Order delivered(Store store, String deliveredAt, PaymentMethod method, String shipping, Employee seller,
                            String commission, OrderItem... items) {
        Order order = order(store, OrderStatus.DELIVERED, LocalDateTime.parse(deliveredAt), method, shipping, items);
        order = orderRepository.save(order);
        if (seller != null) {
            SalesRecord record = new SalesRecord();
            record.setStore(store);
            record.setEmployee(seller);
            record.setOrder(order);
            record.setSalesAmount(order.getTotalAmount());
            record.setCommission(new BigDecimal(commission));
            record.setRecordedAt(order.getDeliveredAt());
            salesRecordRepository.save(record);
        }
        return order;
    }

    private Order order(Store store, OrderStatus status, LocalDateTime deliveredAt, PaymentMethod method,
                        String shipping, OrderItem... items) {
        Order order = new Order();
        order.setUser(userRepository.getReferenceById(customerId));
        order.setRecipientName("Khách Báo Cáo");
        order.setRecipientPhone("0907777777");
        order.setShippingAddress("1 Lê Lợi, Quận 5, Hồ Chí Minh");
        order.setStatus(status);
        order.setPaymentMethod(method);
        order.setDeliveredAt(deliveredAt);
        order.setStore(store);
        order.setShippingCost(new BigDecimal(shipping));
        BigDecimal total = new BigDecimal(shipping);
        for (OrderItem item : items) {
            order.addItem(item);
            total = total.add(item.getSubtotal());
        }
        order.setTotalAmount(total);
        return order;
    }

    private void refund(Order order, String amount, String refundedAt) {
        entityManager.flush();
        entityManager.createNativeQuery("insert into return_requests (order_id, user_id, reason, status, refund_amount, "
                        + "reason_type, completed_at) values (:order, :user, 'Lỗi màn hình', 'REFUNDED', :amount, "
                        + "'DEFECTIVE', :at)")
                .setParameter("order", order.getId()).setParameter("user", customerId)
                .setParameter("amount", new BigDecimal(amount)).setParameter("at", LocalDateTime.parse(refundedAt))
                .executeUpdate();
    }

    private static OrderItem line(ProductVariant variant, int quantity) {
        OrderItem item = new OrderItem();
        item.setVariant(variant);
        item.setQuantity(quantity);
        item.setUnitPrice(variant.getPrice());
        item.setSubtotal(variant.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return item;
    }

    private Category category(String name, String slug) {
        Category category = new Category();
        category.setName(name);
        category.setSlug(slug);
        return categoryRepository.save(category);
    }

    private ProductVariant variant(Category category, String name, String slug, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        product = productRepository.save(product);
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName("Mặc định");
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    /** Keyed by employee id, "none" for the row of orders without a sales record. */
    private static Map<String, JsonNode> employeeRows(JsonNode report) {
        Map<String, JsonNode> rows = new HashMap<>();
        report.get("byEmployee").forEach(row -> rows.put(
                row.get("employeeId").isNull() ? "none" : row.get("employeeId").asString(), row));
        return rows;
    }

    private static List<String> texts(JsonNode array, String field) {
        List<String> values = new ArrayList<>();
        array.forEach(row -> values.add(row.get(field).asString()));
        return values;
    }

    private static void assertMoney(JsonNode value, String expected) {
        assertThat(new BigDecimal(value.asString())).isEqualByComparingTo(expected);
    }

    private JsonNode register(String username, String fullname) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", fullname)).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String fullname, String role) throws Exception {
        Long id = register(username, fullname).get("user").get("id").asLong();
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id),
                roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token) throws Exception {
        return send(builder, token, null);
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String token, Object body) throws Exception {
        if (token != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (body != null) {
            builder.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
        }
        return mockMvc.perform(builder);
    }

    private JsonNode data(ResultActions result) throws Exception {
        return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
    }
}
