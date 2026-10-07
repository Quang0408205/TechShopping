package com.example.Tech.controller.employee;

import com.example.Tech.entity.employee.EmployeeAssignment;
import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.store.StoreRepository;
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

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * /api/v1/admin/employees and /api/v1/employees/me over HTTP against the test database (rolled back), with
 * real ADMIN / STAFF / customer accounts.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminEmployeeApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String URL = "/api/v1/admin/employees";
    private static final String ME_URL = "/api/v1/employees/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private Clock clock;

    private final List<Long> registeredUserIds = new ArrayList<>();

    private String customerToken;
    private Long customerId;
    private String staffToken;
    private Long staffId;
    private String adminToken;
    private Store storeA;
    private Store storeB;
    private Store closed;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("nv.customer");
        customerToken = customer.get("accessToken").asString();
        customerId = customer.get("user").get("id").asLong();
        staffToken = registerWithRole("nv.staff", "STAFF");
        staffId = userRepository.findByUsername("nv.staff").orElseThrow().getId();
        adminToken = registerWithRole("nv.admin", "ADMIN");

        storeA = storeRepository.save(store("ZZ NV Chi nhánh A", true));
        storeB = storeRepository.save(store("ZZ NV Chi nhánh B", true));
        closed = storeRepository.save(store("ZZ NV Đã đóng", false));
        entityManager.flush();
    }

    @AfterEach
    void revokeTokens() {
        registeredUserIds.forEach(refreshTokenService::revokeAll);
    }

    @Test
    void access_adminOnly_andMeNeedsAStaffAccount() throws Exception {
        send(get(URL), null, null).andExpect(status().isUnauthorized());
        send(get(URL), customerToken, null).andExpect(status().isForbidden());
        send(get(URL), staffToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        send(get(URL), adminToken, null).andExpect(status().isOk());

        send(get(ME_URL), null, null).andExpect(status().isUnauthorized());
        send(get(ME_URL), customerToken, null).andExpect(status().isForbidden());
        send(get(ME_URL), staffToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_NOT_FOUND"));
    }

    @Test
    void create_assign_move_unassign_andTheStaffSeesTheirStore() throws Exception {
        JsonNode created = data(send(post(URL), adminToken, Map.of(
                "userId", staffId, "employeeCode", " ZZNV01 ", "position", "Bán hàng",
                "storeId", storeA.getId(), "positionAtStore", "Quản lý chi nhánh"))
                .andExpect(status().isCreated()));
        long id = created.get("id").asLong();
        assertThat(created.get("username").asString()).isEqualTo("nv.staff");
        assertThat(created.get("employeeCode").asString()).isEqualTo("ZZNV01");
        assertThat(created.get("assignment").get("storeId").asInt()).isEqualTo(storeA.getId());
        assertThat(created.get("assignment").get("positionAtStore").asString()).isEqualTo("Quản lý chi nhánh");

        JsonNode mine = data(send(get(ME_URL), staffToken, null).andExpect(status().isOk()));
        assertThat(mine.get("id").asLong()).isEqualTo(id);
        assertThat(mine.get("assignment").get("storeName").asString()).isEqualTo("ZZ NV Chi nhánh A");
        assertThat(searchIds("storeId", storeA.getId().toString())).containsExactly(id);

        JsonNode moved = data(send(post(URL + "/" + id + "/assignment"), adminToken,
                Map.of("storeId", storeB.getId(), "positionAtStore", "Nhân viên"))
                .andExpect(status().isOk()));
        assertThat(moved.get("assignment").get("storeId").asInt()).isEqualTo(storeB.getId());
        assertThat(searchIds("storeId", storeA.getId().toString())).isEmpty();
        assertThat(searchIds("storeId", storeB.getId().toString())).containsExactly(id);
        List<EmployeeAssignment> history = entityManager.createQuery(
                        "select a from EmployeeAssignment a where a.employee.id = :id order by a.id", EmployeeAssignment.class)
                .setParameter("id", id).getResultList();
        assertThat(history).hasSize(2);
        assertThat(history.get(0).getActive()).isFalse();
        assertThat(history.get(0).getEndDate()).isEqualTo(LocalDate.now(clock));
        assertThat(history.get(1).getActive()).isTrue();

        JsonNode unassigned = data(send(delete(URL + "/" + id + "/assignment"), adminToken, null)
                .andExpect(status().isOk()));
        assertThat(unassigned.get("assignment").isNull()).isTrue();
        assertThat(data(send(get(ME_URL), staffToken, null).andExpect(status().isOk())).get("assignment").isNull())
                .isTrue();
    }

    @Test
    void update_toInactive_endsTheAssignment_andSearchFiltersByStatusAndKeyword() throws Exception {
        long id = createEmployee(staffId, storeA);

        Map<String, Object> request = new HashMap<>();
        request.put("employeeCode", "ZZNV02");
        request.put("active", false);
        JsonNode updated = data(send(put(URL + "/" + id), adminToken, request).andExpect(status().isOk()));

        assertThat(updated.get("active").asBoolean()).isFalse();
        assertThat(updated.get("assignment").isNull()).isTrue();
        assertThat(searchIds("keyword", "zznv02", "active", "false")).containsExactly(id);
        assertThat(searchIds("keyword", "zznv02", "active", "true")).isEmpty();
        assertThat(searchIds("keyword", "NV.STAFF")).containsExactly(id);

        send(post(URL + "/" + id + "/assignment"), adminToken, Map.of("storeId", storeB.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void invalidRequests() throws Exception {
        send(post(URL), adminToken, Map.of("userId", customerId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.userId").exists());
        send(post(URL), adminToken, Map.of("userId", -1))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.userId").exists());
        send(post(URL), adminToken, Map.of("userId", staffId, "storeId", closed.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());

        long id = createEmployee(staffId, null);
        send(post(URL), adminToken, Map.of("userId", staffId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_ALREADY_EXISTS"));

        String otherStaffToken = registerWithRole("nv.staff2", "STAFF");
        assertThat(otherStaffToken).isNotBlank();
        Long otherStaffId = userRepository.findByUsername("nv.staff2").orElseThrow().getId();
        send(put(URL + "/" + id), adminToken, Map.of("employeeCode", "ZZNV09")).andExpect(status().isOk());
        send(post(URL), adminToken, Map.of("userId", otherStaffId, "employeeCode", "ZZNV09"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMPLOYEE_CODE"));

        send(post(URL + "/" + id + "/assignment"), adminToken, Map.of("storeId", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
        send(post(URL + "/" + id + "/assignment"), adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());
        send(get(URL + "/-1"), adminToken, null)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_NOT_FOUND"));
    }

    private long createEmployee(Long userId, Store store) throws Exception {
        Map<String, Object> request = new HashMap<>();
        request.put("userId", userId);
        if (store != null) {
            request.put("storeId", store.getId());
        }
        return data(send(post(URL), adminToken, request).andExpect(status().isCreated())).get("id").asLong();
    }

    private List<Long> searchIds(String... params) throws Exception {
        MockHttpServletRequestBuilder builder = get(URL);
        for (int i = 0; i < params.length; i += 2) {
            builder.param(params[i], params[i + 1]);
        }
        List<Long> ids = new ArrayList<>();
        data(send(builder, adminToken, null).andExpect(status().isOk())).get("content")
                .forEach(row -> ids.add(row.get("id").asLong()));
        return ids;
    }

    private static Store store(String name, boolean active) {
        Store store = new Store();
        store.setName(name);
        store.setAddress("1 Đường Test");
        store.setDistrict("Quận 1");
        store.setCity("Hồ Chí Minh");
        store.setActive(active);
        return store;
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Nhân Viên"))
                .andExpect(status().isCreated()));
        registeredUserIds.add(data.get("user").get("id").asLong());
        return data;
    }

    /** Registers, adds the role in the DB, then logs in again so the access token carries it. */
    private String registerWithRole(String username, String role) throws Exception {
        Long id = register(username).get("user").get("id").asLong();
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
