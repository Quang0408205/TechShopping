package com.example.Tech.controller.combo;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.product.ProductVariant;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.product.ProductVariantRepository;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Admin combo API, the public product combos, cart + checkout with combo → voucher, delete rules. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ComboApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private ProductVariantRepository variantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String admin;
    private String staff;
    private String customer;
    private Product phoneProduct;
    private Product coverProduct;
    private Product chargerProduct;
    private ProductVariant phone;
    private ProductVariant cover;
    private ProductVariant charger;

    @BeforeEach
    void setUp() throws Exception {
        admin = registerWithRole("combo.admin", "ADMIN");
        staff = registerWithRole("combo.staff", "STAFF");
        customer = token(register("combo.khach"));
        Category category = new Category();
        category.setName("Danh mục combo");
        category.setSlug("test-combo-category");
        category = categoryRepository.save(category);
        phoneProduct = product("Điện thoại Combo", "test-combo-phone", category, "15000000");
        coverProduct = product("Ốp Combo", "test-combo-cover", category, "200000");
        chargerProduct = product("Sạc Combo", "test-combo-charger", category, "500000");
        phone = variant(phoneProduct, "Đen", "15000000");
        cover = variant(coverProduct, "Trong", "200000");
        charger = variant(chargerProduct, "20W", "500000");
        StoreFixtures.warehouse(entityManager, phone, cover, charger);
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void adminOnly_andTheRulesOfTheForm() throws Exception {
        send(get("/api/v1/admin/combos"), staff, null).andExpect(status().isForbidden());
        send(get("/api/v1/admin/combos"), customer, null).andExpect(status().isForbidden());
        send(post("/api/v1/admin/combos"), staff, body(List.of(item(cover, "PERCENT", 50)))).andExpect(status().isForbidden());
        send(get("/api/v1/admin/combos/suggestions?productId=" + phoneProduct.getId()), staff, null)
                .andExpect(status().isForbidden());

        send(post("/api/v1/admin/combos"), admin, body(List.of(item(cover, "PERCENT", 50))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.deletable").value(true));
        // the main product cannot be its own add-on; no items; > 100 %; the same add-on twice; end before start
        send(post("/api/v1/admin/combos"), admin, body(List.of(Map.of("productId", phoneProduct.getId(),
                "discountType", "AMOUNT", "discountValue", 1000)))).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.items").exists());
        send(post("/api/v1/admin/combos"), admin, body(List.of())).andExpect(status().isBadRequest());
        send(post("/api/v1/admin/combos"), admin, body(List.of(item(cover, "PERCENT", 150))))
                .andExpect(status().isBadRequest());
        send(post("/api/v1/admin/combos"), admin, body(List.of(item(cover, "PERCENT", 5), item(cover, "AMOUNT", 5000))))
                .andExpect(status().isBadRequest());
        Map<String, Object> backwards = body(List.of(item(cover, "PERCENT", 5)));
        backwards.put("endsAt", LocalDateTime.now().minusDays(2).truncatedTo(ChronoUnit.SECONDS).toString());
        send(post("/api/v1/admin/combos"), admin, backwards).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.endsAt").exists());
    }

    @Test
    void productPageCombos_cartAndCheckout_comboThenVoucher_andDeleteRules() throws Exception {
        Long id = data(send(post("/api/v1/admin/combos"), admin,
                body(List.of(item(cover, "PERCENT", 50), item(charger, "AMOUNT", 100000))))
                .andExpect(status().isCreated())).get("id").asLong();

        // public: the product page offers the add-ons with the price after the discount
        send(get("/api/v1/products/" + phoneProduct.getId() + "/combos"), null, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].items.length()").value(2))
                .andExpect(jsonPath("$.data[0].items[?(@.productId == " + coverProduct.getId() + ")].variants[0].comboPrice")
                        .value(100000))
                .andExpect(jsonPath("$.data[0].items[?(@.productId == " + chargerProduct.getId() + ")].variants[0].comboPrice")
                        .value(400000));
        send(get("/api/v1/products/" + coverProduct.getId() + "/combos"), null, null)
                .andExpect(jsonPath("$.data.length()").value(0));

        // the add-on alone: no discount; the main product joins: the add-on gets it; leaves: back to the normal price
        addToCart(cover, 2);
        send(get("/api/v1/cart"), customer, null).andExpect(jsonPath("$.data.comboDiscount").value(0));
        addToCart(phone, 1);
        send(get("/api/v1/cart"), customer, null)
                .andExpect(jsonPath("$.data.comboDiscount").value(100000))
                .andExpect(jsonPath("$.data.combos[0].units").value(1))
                .andExpect(jsonPath("$.data.items[?(@.variantId == " + cover.getId() + ")].comboDiscount").value(100000))
                .andExpect(jsonPath("$.data.items[?(@.variantId == " + cover.getId() + ")].comboNames[0]").value("Combo mua kèm"))
                .andExpect(jsonPath("$.data.total").value(15300000));
        addToCart(charger, 1);
        send(get("/api/v1/cart"), customer, null).andExpect(jsonPath("$.data.comboDiscount").value(200000));

        // combo first, then the 10 % voucher on what is left (15.900.000 − 200.000), then free shipping from the goods before both
        Map<String, Object> voucher = new HashMap<>();
        voucher.put("code", "COMBOTEST");
        voucher.put("name", "Voucher thử combo");
        voucher.put("discountPercent", 10);
        voucher.put("startsAt", LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).toString());
        voucher.put("endsAt", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString());
        send(post("/api/v1/admin/vouchers"), admin, voucher).andExpect(status().isCreated());
        send(post("/api/v1/vouchers/apply"), customer, Map.of("code", "COMBOTEST"))
                .andExpect(jsonPath("$.data.eligibleSubtotal").value(15700000))
                .andExpect(jsonPath("$.data.discount").value(1570000));

        JsonNode order = data(send(post("/api/v1/orders"), customer, orderBody("COMBOTEST"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.subtotal").value(15900000))
                .andExpect(jsonPath("$.data.comboDiscount").value(200000))
                .andExpect(jsonPath("$.data.combos[0].name").value("Combo mua kèm"))
                .andExpect(jsonPath("$.data.voucherDiscount").value(1570000))
                .andExpect(jsonPath("$.data.shippingFee").value(0))
                .andExpect(jsonPath("$.data.total").value(14130000)));
        Long orderId = order.get("id").asLong();
        assertThat(sum("order_items", "combo_discount", orderId)).isEqualByComparingTo("200000");
        assertThat(sum("order_combos", "discount_amount", orderId)).isEqualByComparingTo("200000");

        // delivered → the sales report counts the combo (orders, add-on units, discount, order totals)
        entityManager.createNativeQuery("update orders set status = 'DELIVERED', delivered_at = now() where order_id = :id")
                .setParameter("id", orderId).executeUpdate();
        entityManager.flush();
        entityManager.clear();
        send(get("/api/v1/admin/reports/sales"), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.byCombo[?(@.comboId == " + id + ")].orders").value(1))
                .andExpect(jsonPath("$.data.byCombo[?(@.comboId == " + id + ")].units").value(2))
                .andExpect(jsonPath("$.data.byCombo[?(@.comboId == " + id + ")].discount").value(200000.0))
                .andExpect(jsonPath("$.data.byCombo[?(@.comboId == " + id + ")].revenue").value(14130000.0));

        // a combo an order used cannot be deleted: switch it off, and a new cart gets no discount
        send(delete("/api/v1/admin/combos/" + id), admin, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("COMBO_IN_USE"));
        send(patch("/api/v1/admin/combos/" + id + "/status"), admin, Map.of("active", false))
                .andExpect(jsonPath("$.data.status").value("PAUSED"))
                .andExpect(jsonPath("$.data.orderCount").value(1));
        addToCart(phone, 1);
        addToCart(cover, 1);
        send(get("/api/v1/cart"), customer, null).andExpect(jsonPath("$.data.comboDiscount").value(0));
        send(get("/api/v1/products/" + phoneProduct.getId() + "/combos"), null, null)
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void anUnusedComboCanBeEditedAndDeleted_andSuggestionsComeFromTheRelations() throws Exception {
        Long id = data(send(post("/api/v1/admin/combos"), admin, body(List.of(item(cover, "PERCENT", 10))))
                .andExpect(status().isCreated())).get("id").asLong();
        send(put("/api/v1/admin/combos/" + id), admin, body(List.of(item(cover, "AMOUNT", 20000), item(charger, "PERCENT", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(2));
        send(put("/api/v1/admin/combos/" + id), admin, body(List.of(item(charger, "PERCENT", 7))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].discountValue").value(7));

        entityManager.createNativeQuery("insert into product_relations (product_id, related_product_id, relation_type, strength) "
                        + "values (:p, :r, 'ACCESSORY', 0.9)")
                .setParameter("p", phoneProduct.getId()).setParameter("r", coverProduct.getId()).executeUpdate();
        send(get("/api/v1/admin/combos/suggestions?productId=" + phoneProduct.getId()), admin, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].productId").value(coverProduct.getId()))
                .andExpect(jsonPath("$.data[0].relationType").value("ACCESSORY"));

        send(delete("/api/v1/admin/combos/" + id), admin, null).andExpect(status().isNoContent());
        send(get("/api/v1/admin/combos/" + id), admin, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMBO_NOT_FOUND"));
    }

    private BigDecimal sum(String table, String column, Long orderId) {
        entityManager.flush();
        return (BigDecimal) entityManager.createNativeQuery(
                        "select coalesce(sum(" + column + "), 0) from " + table + " where order_id = :id")
                .setParameter("id", orderId).getSingleResult();
    }

    private Map<String, Object> item(ProductVariant variant, String type, int value) {
        return Map.of("productId", variant.getProduct().getId(), "discountType", type, "discountValue", value);
    }

    private Map<String, Object> body(List<?> items) {
        Map<String, Object> body = new HashMap<>();
        body.put("name", "Combo mua kèm");
        body.put("mainProductId", phoneProduct.getId());
        body.put("startsAt", LocalDateTime.now().minusHours(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("endsAt", LocalDateTime.now().plusDays(1).truncatedTo(ChronoUnit.SECONDS).toString());
        body.put("items", items);
        return body;
    }

    private static Map<String, Object> orderBody(String voucherCode) {
        Map<String, Object> body = new HashMap<>();
        body.put("recipientName", "Khách Combo");
        body.put("recipientPhone", "0901234567");
        body.put("shippingAddress", "1 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh");
        body.put("paymentMethod", "COD");
        body.put("voucherCode", voucherCode);
        return body;
    }

    private void addToCart(ProductVariant variant, int quantity) throws Exception {
        send(post("/api/v1/cart/items"), customer, Map.of("variantId", variant.getId(), "quantity", quantity))
                .andExpect(status().isOk());
    }

    private Product product(String name, String slug, Category category, String price) {
        Product product = new Product();
        product.setName(name);
        product.setSlug(slug);
        product.setCategory(category);
        product.setBasePrice(new BigDecimal(price));
        return productRepository.save(product);
    }

    private ProductVariant variant(Product product, String name, String price) {
        ProductVariant variant = new ProductVariant();
        variant.setProduct(product);
        variant.setVariantName(name);
        variant.setPrice(new BigDecimal(price));
        return variantRepository.save(variant);
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Thử Combo"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private static String token(JsonNode auth) {
        return auth.get("accessToken").asString();
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id), roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private ResultActions send(MockHttpServletRequestBuilder builder, String accessToken, Object body) throws Exception {
        if (accessToken != null) {
            builder.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
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
