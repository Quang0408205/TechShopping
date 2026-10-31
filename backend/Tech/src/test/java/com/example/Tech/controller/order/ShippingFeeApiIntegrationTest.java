package com.example.Tech.controller.order;

import com.example.Tech.entity.user.UserRole;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The public fee lookup and the ADMIN-only province rules over HTTP against the test database (rolled back). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ShippingFeeApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String FEES = "/api/v1/admin/shipping-fees";

    @Autowired private MockMvc mockMvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private UserRepository userRepository;
    @Autowired private RoleRepository roleRepository;
    @Autowired private UserRoleRepository userRoleRepository;
    @Autowired private EntityManager entityManager;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private String adminToken;
    private String staffToken;

    @BeforeEach
    void setUp() throws Exception {
        customerToken = register("sf.customer").get("accessToken").asString();
        adminToken = registerWithRole("sf.admin", "ADMIN");
        staffToken = registerWithRole("sf.staff", "STAFF");
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void theFeeLookupIsPublic_defaultIs30k_noMatterTheValue_pickupAndEmptyAreFree() throws Exception {
        fee("?province=Thành phố Hà Nội&subtotal=500000").andExpect(jsonPath("$.data.fee").value(30000));
        fee("?province=Thành phố Hà Nội&subtotal=45000000").andExpect(jsonPath("$.data.fee").value(30000));
        fee("?subtotal=1000000").andExpect(jsonPath("$.data.fee").value(30000));
        fee("?province=Thành phố Hà Nội&subtotal=1000000&deliveryType=PICKUP").andExpect(jsonPath("$.data.fee").value(0));
        fee("?province=Thành phố Hà Nội&subtotal=0").andExpect(jsonPath("$.data.fee").value(0));
        fee("").andExpect(jsonPath("$.data.fee").value(0));
    }

    @Test
    void rules_adminManagesThem_andTheLookupFollows() throws Exception {
        int id = data(send(post(FEES), adminToken, Map.of("provinceName", "  Thành phố Hồ Chí Minh ", "fee", 20000))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.provinceName").value("Thành phố Hồ Chí Minh")))
                .get("id").asInt();
        fee("?province=thành phố hồ chí minh&subtotal=1000000").andExpect(jsonPath("$.data.fee").value(20000));
        fee("?province=Thành phố Hồ Chí Minh&subtotal=1000000&deliveryType=PICKUP").andExpect(jsonPath("$.data.fee").value(0));
        fee("?province=Thành phố Hà Nội&subtotal=1000000").andExpect(jsonPath("$.data.fee").value(30000));

        send(put(FEES + "/" + id), adminToken, Map.of("provinceName", "Thành phố Hồ Chí Minh", "fee", 0)).andExpect(status().isOk());
        fee("?province=Thành phố Hồ Chí Minh&subtotal=1000000").andExpect(jsonPath("$.data.fee").value(0));

        send(get(FEES), adminToken, null).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].provinceName").value("Thành phố Hồ Chí Minh"));
        send(delete(FEES + "/" + id), adminToken, null).andExpect(status().isOk());
        fee("?province=Thành phố Hồ Chí Minh&subtotal=1000000").andExpect(jsonPath("$.data.fee").value(30000));
        send(delete(FEES + "/" + id), adminToken, null).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("SHIPPING_FEE_RULE_NOT_FOUND"));
    }

    @Test
    void rules_areAdminOnly_validated_andUniquePerProvince() throws Exception {
        send(get(FEES), null, null).andExpect(status().isUnauthorized());
        send(get(FEES), customerToken, null).andExpect(status().isForbidden());
        send(get(FEES), staffToken, null).andExpect(status().isForbidden());
        send(post(FEES), staffToken, Map.of("provinceName", "Thành phố Huế", "fee", 1000)).andExpect(status().isForbidden());

        send(post(FEES), adminToken, Map.of("provinceName", "", "fee", 1000)).andExpect(status().isBadRequest());
        send(post(FEES), adminToken, Map.of("provinceName", "Thành phố Huế", "fee", -1)).andExpect(status().isBadRequest());
        send(post(FEES), adminToken, Map.of("provinceName", "Thành phố Huế", "fee", 99999999)).andExpect(status().isBadRequest());
        send(post(FEES), adminToken, Map.of("provinceName", "Thành phố Huế")).andExpect(status().isBadRequest());

        send(post(FEES), adminToken, Map.of("provinceName", "Thành phố Huế", "fee", 1000)).andExpect(status().isCreated());
        send(post(FEES), adminToken, Map.of("provinceName", "THÀNH PHỐ HUẾ", "fee", 5000))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("DUPLICATE_SHIPPING_FEE_RULE"));
    }

    private ResultActions fee(String query) throws Exception {
        return send(get("/api/v1/shipping-fee" + query), null, null).andExpect(status().isOk());
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Phí Giao")).andExpect(status().isCreated()));
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
