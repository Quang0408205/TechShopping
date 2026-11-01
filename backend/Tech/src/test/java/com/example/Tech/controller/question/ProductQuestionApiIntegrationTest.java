package com.example.Tech.controller.question;

import com.example.Tech.entity.product.Category;
import com.example.Tech.entity.product.Product;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.product.CategoryRepository;
import com.example.Tech.repository.product.ProductRepository;
import com.example.Tech.repository.user.RoleRepository;
import com.example.Tech.repository.user.UserRepository;
import com.example.Tech.repository.user.UserRoleRepository;
import com.example.Tech.security.RefreshTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Product questions over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductQuestionApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String QUESTION = "Máy này có hỗ trợ sạc nhanh không?";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String otherToken;
    private String staffToken;
    private String adminToken;
    private Product product;

    @BeforeEach
    void setUp() throws Exception {
        jdbc.update("delete from product_questions");
        jdbc.update("delete from staff_notifications");
        customerToken = register("qa.customer").get("accessToken").asString();
        otherToken = register("qa.other").get("accessToken").asString();
        staffToken = registerWithRole("qa.staff", "STAFF");
        adminToken = registerWithRole("qa.admin", "ADMIN");
        Category category = new Category();
        category.setName("Hỏi đáp test");
        category.setSlug("test-qa-category");
        category = categoryRepository.save(category);
        product = new Product();
        product.setName("Điện thoại Hỏi Đáp");
        product.setSlug("test-qa-phone");
        product.setCategory(category);
        product.setBasePrice(new BigDecimal("1000000"));
        product = productRepository.saveAndFlush(product);
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void aQuestionIsPublicOnlyOnceAnsweredAndNotHidden_andShowsTheGivenNameOnly() throws Exception {
        ask(customerToken, QUESTION).andExpect(status().isCreated());
        assertThat(publicQuestions().get("totalElements").asInt()).isZero();

        long id = pending().get(0).get("id").asLong();
        send(put("/api/v1/admin/questions/" + id + "/answer"), staffToken, Map.of("answer", "Có, hỗ trợ sạc nhanh 45W.")).andExpect(status().isOk());

        JsonNode shown = publicQuestions();
        assertThat(shown.get("totalElements").asInt()).isEqualTo(1);
        JsonNode first = shown.get("content").get(0);
        assertThat(first.get("question").asString()).isEqualTo(QUESTION);
        assertThat(first.get("answer").asString()).isEqualTo("Có, hỗ trợ sạc nhanh 45W.");
        assertThat(first.get("askerName").asString()).isEqualTo("Công");
        assertThat(first.toString()).doesNotContain("qa.customer").doesNotContain("@example.com");

        send(patch("/api/v1/admin/questions/" + id + "/hidden"), adminToken, Map.of("hidden", true)).andExpect(status().isOk());
        assertThat(publicQuestions().get("totalElements").asInt()).isZero();
        send(patch("/api/v1/admin/questions/" + id + "/hidden"), adminToken, Map.of("hidden", false)).andExpect(status().isOk());
        assertThat(publicQuestions().get("totalElements").asInt()).isEqualTo(1);
    }

    @Test
    void askingNeedsALogin_aValidProduct_andALengthBetween10And500() throws Exception {
        send(post("/api/v1/product-questions"), null, Map.of("productId", product.getId(), "question", QUESTION)).andExpect(status().isUnauthorized());
        ask(customerToken, "ngắn quá").andExpect(status().isBadRequest());
        ask(customerToken, "x".repeat(501)).andExpect(status().isBadRequest());
        ask(customerToken, "          ").andExpect(status().isBadRequest());
        send(post("/api/v1/product-questions"), customerToken, Map.of("productId", 999999999L, "question", QUESTION)).andExpect(status().isNotFound());
        send(get("/api/v1/products/999999999/questions"), null, null).andExpect(status().isNotFound());

        product.setActive(false);
        productRepository.saveAndFlush(product);
        ask(customerToken, QUESTION).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from product_questions", Integer.class)).isZero();
    }

    @Test
    void aCustomerMayAskFiveQuestionsADay_notSix() throws Exception {
        for (int i = 1; i <= 5; i++) {
            ask(customerToken, QUESTION + " " + i).andExpect(status().isCreated());
        }
        ask(customerToken, QUESTION + " 6").andExpect(status().isTooManyRequests());
        ask(otherToken, QUESTION).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select count(*) from product_questions", Integer.class)).isEqualTo(6);
    }

    @Test
    void staffAndAdminAnswer_customersAndVisitorsCannot() throws Exception {
        ask(customerToken, QUESTION).andExpect(status().isCreated());
        long id = pending().get(0).get("id").asLong();

        for (String path : new String[]{"/api/v1/admin/questions"}) {
            send(get(path), null, null).andExpect(status().isUnauthorized());
            send(get(path), customerToken, null).andExpect(status().isForbidden());
        }
        send(put("/api/v1/admin/questions/" + id + "/answer"), customerToken, Map.of("answer", "tự trả lời")).andExpect(status().isForbidden());
        send(patch("/api/v1/admin/questions/" + id + "/hidden"), customerToken, Map.of("hidden", true)).andExpect(status().isForbidden());
        send(put("/api/v1/admin/questions/" + id + "/answer"), staffToken, Map.of("answer", "  ")).andExpect(status().isBadRequest());
        send(put("/api/v1/admin/questions/" + id + "/answer"), staffToken, Map.of("answer", "y".repeat(1001))).andExpect(status().isBadRequest());
        send(put("/api/v1/admin/questions/999999999/answer"), staffToken, Map.of("answer", "ok")).andExpect(status().isNotFound());
        send(patch("/api/v1/admin/questions/999999999/hidden"), staffToken, Map.of("hidden", true)).andExpect(status().isNotFound());

        send(put("/api/v1/admin/questions/" + id + "/answer"), adminToken, Map.of("answer", "Quản trị viên trả lời")).andExpect(status().isOk());
        JsonNode answered = data(send(get("/api/v1/admin/questions?status=ANSWERED"), staffToken, null).andExpect(status().isOk())).get("content").get(0);
        assertThat(answered.get("answeredByName").asString()).isEqualTo("Người Dùng Hỏi Đáp");
        assertThat(answered.get("productName").asString()).isEqualTo("Điện thoại Hỏi Đáp");
        assertThat(answered.get("askerEmail").asString()).isEqualTo("qa.customer@example.com");
    }

    @Test
    void theStaffListFiltersByStatusAndKeyword_andANewQuestionNotifiesStaff() throws Exception {
        ask(customerToken, QUESTION).andExpect(status().isCreated());
        ask(otherToken, "Bảo hành máy này bao lâu vậy shop?").andExpect(status().isCreated());
        long first = pending().get(1).get("id").asLong();
        send(put("/api/v1/admin/questions/" + first + "/answer"), staffToken, Map.of("answer", "12 tháng")).andExpect(status().isOk());

        assertThat(list("?status=PENDING").get("totalElements").asInt()).isEqualTo(1);
        assertThat(list("?status=ANSWERED").get("totalElements").asInt()).isEqualTo(1);
        assertThat(list("").get("totalElements").asInt()).isEqualTo(2);
        assertThat(list("?keyword=bảo hành").get("totalElements").asInt()).isEqualTo(1);
        assertThat(list("?keyword=qa.other@example.com").get("totalElements").asInt()).isEqualTo(1);
        assertThat(list("?keyword=%25").get("totalElements").asInt()).isZero();
        send(patch("/api/v1/admin/questions/" + first + "/hidden"), staffToken, Map.of("hidden", true)).andExpect(status().isOk());
        assertThat(list("?status=HIDDEN").get("totalElements").asInt()).isEqualTo(1);
        assertThat(list("?status=ANSWERED").get("totalElements").asInt()).isZero();

        List<Map<String, Object>> notices = jdbc.queryForList("select kind, title, link, store_id from staff_notifications");
        assertThat(notices).hasSize(2);
        assertThat(notices.get(0).get("kind")).isEqualTo("PRODUCT_QUESTION");
        assertThat(notices.get(0).get("title").toString()).isEqualTo("Câu hỏi mới về Điện thoại Hỏi Đáp");
        assertThat(notices.get(0).get("store_id")).isNull();
        assertThat(notices.get(0).get("link")).isEqualTo("questions.html");
    }

    private JsonNode list(String query) throws Exception {
        return data(send(get("/api/v1/admin/questions" + query), staffToken, null).andExpect(status().isOk()));
    }

    private JsonNode pending() throws Exception {
        return list("?status=PENDING").get("content");
    }

    private JsonNode publicQuestions() throws Exception {
        return data(send(get("/api/v1/products/" + product.getId() + "/questions"), null, null).andExpect(status().isOk()));
    }

    private ResultActions ask(String token, String question) throws Exception {
        return send(post("/api/v1/product-questions"), token, Map.of("productId", product.getId(), "question", question));
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", username.equals("qa.customer") ? "Nguyễn Văn Công" : "Người Dùng Hỏi Đáp")).andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
        userRoleRepository.deleteAll(userRoleRepository.findAllByIdUserId(id));
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(id),
                roleRepository.findByName(role).orElseThrow()));
        entityManager.flush();
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", PASSWORD))
                .andExpect(status().isOk())).get("accessToken").asString();
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
