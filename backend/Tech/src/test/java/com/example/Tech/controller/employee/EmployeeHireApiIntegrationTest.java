package com.example.Tech.controller.employee;

import com.example.Tech.entity.store.Store;
import com.example.Tech.entity.user.UserRole;
import com.example.Tech.repository.employee.EmployeeAssignmentRepository;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hiring (ADMIN: /api/v1/admin/employees/hire) and a branch manager's own branch (/api/v1/branch/**) over HTTP
 * against the test database (rolled back). Managers and staff are created through the hire API itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EmployeeHireApiIntegrationTest {

    private static final String PASSWORD = "Matkhau@123";
    private static final String HIRE = "/api/v1/admin/employees/hire";
    private static final String BRANCH = "/api/v1/branch";

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
    private EmployeeAssignmentRepository assignmentRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private EntityManager entityManager;

    private final List<Long> userIds = new ArrayList<>();

    private String adminToken;
    private String customerToken;
    private Long customerId;
    private Store storeA;
    private Store storeB;
    private Store closed;
    private String managerAToken;
    private Long managerAUserId;
    private String staffAToken;
    private Long staffAUserId;
    private Long staffAEmployeeId;
    private Long staffBEmployeeId;

    @BeforeEach
    void setUp() throws Exception {
        JsonNode customer = register("hr.khach");
        customerToken = customer.get("accessToken").asString();
        customerId = customer.get("user").get("id").asLong();
        Long adminId = register("hr.admin").get("user").get("id").asLong();
        userRoleRepository.deleteAll(userRoleRepository.findAllByIdUserId(adminId));
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(adminId), roleRepository.findByName("ADMIN").orElseThrow()));
        entityManager.flush();
        adminToken = login("hr.admin");

        storeA = storeRepository.save(store("ZZ HR Chi nhánh A", true));
        storeB = storeRepository.save(store("ZZ HR Chi nhánh B", true));
        closed = storeRepository.save(store("ZZ HR Đã đóng", false));
        entityManager.flush();

        JsonNode manager = hire(adminToken, hireBody("hr.quanly", "BRANCH_MANAGER", storeA.getId()), 201);
        managerAUserId = manager.get("employee").get("userId").asLong();
        managerAToken = login("hr.quanly");
        JsonNode staffA = hire(adminToken, hireBody("hr.nva", "STAFF", storeA.getId()), 201);
        staffAUserId = staffA.get("employee").get("userId").asLong();
        staffAEmployeeId = staffA.get("employee").get("id").asLong();
        staffAToken = login("hr.nva");
        staffBEmployeeId = hire(adminToken, hireBody("hr.nvb", "STAFF", storeB.getId()), 201)
                .get("employee").get("id").asLong();
    }

    @AfterEach
    void revokeTokens() {
        userIds.forEach(refreshTokenService::revokeAll);
    }

    // ---------- admin hire ----------

    @Test
    void adminHire_createsAccountProfileAndAssignment_withOneRoleOnly() throws Exception {
        Map<String, Object> body = hireBody("hr.moi", "staff", storeA.getId());
        body.put("temporaryPassword", "Tam@Pass123");
        body.put("employeeCode", "ZZHR01");
        body.put("salary", 9000000);
        body.put("positionAtStore", "Thu ngân");
        JsonNode hired = hire(adminToken, body, 201);

        assertThat(hired.get("role").asString()).isEqualTo("STAFF");
        assertThat(hired.get("temporaryPassword").asString()).isEqualTo("Tam@Pass123");
        JsonNode employee = hired.get("employee");
        assertThat(employee.get("assignment").get("storeId").asInt()).isEqualTo(storeA.getId());
        assertThat(employee.get("assignment").get("positionAtStore").asString()).isEqualTo("Thu ngân");
        assertThat(employee.get("employeeCode").asString()).isEqualTo("ZZHR01");
        assertThat(employee.get("active").asBoolean()).isTrue();

        // the new account signs in through the normal login and has exactly one internal role
        JsonNode auth = data(send(post("/api/v1/auth/login"), null,
                Map.of("identifier", "hr.moi", "password", "Tam@Pass123")).andExpect(status().isOk()));
        assertThat(auth.get("user").get("roles")).hasSize(1);
        assertThat(auth.get("user").get("roles").get(0).asString()).isEqualTo("STAFF");
        assertThat(userRoleRepository.findRoleNamesByUserId(employee.get("userId").asLong())).containsExactly("STAFF");
        userIds.add(employee.get("userId").asLong());
    }

    @Test
    void adminHire_withoutPassword_generatesATwelveCharacterOneAndNeverCachesIt() throws Exception {
        Map<String, Object> body = hireBody("hr.tudong", "STAFF", storeA.getId());
        body.remove("temporaryPassword");
        ResultActions result = send(post(HIRE), adminToken, body)
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"));
        String password = data(result).get("temporaryPassword").asString();
        assertThat(password).hasSize(12).matches(".*[a-z].*").matches(".*[A-Z].*").matches(".*\\d.*");
        assertThat(login("hr.tudong", password)).isNotBlank();
    }

    @Test
    void adminHire_managerFlow_oneManagerPerStore_andTheManagerLabelIsReservedForTheRole() throws Exception {
        send(post(HIRE), adminToken, hireBody("hr.quanly2", "BRANCH_MANAGER", storeA.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("STORE_ALREADY_HAS_MANAGER"));
        assertThat(userRepository.findByUsername("hr.quanly2")).isEmpty();

        // a manager of store B is fine, and carries the manager label whatever was typed
        Map<String, Object> body = hireBody("hr.quanlyb", "BRANCH_MANAGER", storeB.getId());
        body.put("positionAtStore", "Thu ngân");
        JsonNode hired = hire(adminToken, body, 201);
        assertThat(hired.get("employee").get("assignment").get("positionAtStore").asString()).isEqualTo("Quản lý chi nhánh");
        userIds.add(hired.get("employee").get("userId").asLong());

        Map<String, Object> label = hireBody("hr.nhan", "STAFF", storeA.getId());
        label.put("positionAtStore", "Quản lý chi nhánh");
        send(post(HIRE), adminToken, label)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.positionAtStore").exists());
    }

    @Test
    void adminHire_validationAndConflicts() throws Exception {
        for (String role : new String[]{"ADMIN", "CUSTOMER", "SUPERUSER"}) {
            send(post(HIRE), adminToken, hireBody("hr.sai", role, storeA.getId()))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.code").value("ROLE_NOT_ASSIGNABLE"));
        }
        send(post(HIRE), adminToken, hireBody("hr.sai", null, storeA.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_ASSIGNABLE"));
        send(post(HIRE), adminToken, hireBody("hr.sai", "STAFF", null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());
        send(post(HIRE), adminToken, hireBody("hr.sai", "STAFF", closed.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.storeId").exists());
        send(post(HIRE), adminToken, hireBody("hr.sai", "STAFF", -1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"));
        Map<String, Object> invalid = hireBody("a", "STAFF", storeA.getId());
        invalid.put("email", "khong-hop-le");
        send(post(HIRE), adminToken, invalid)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.username").exists())
                .andExpect(jsonPath("$.error.details.email").exists());

        send(post(HIRE), adminToken, hireBody("hr.nva", "STAFF", storeA.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMAIL"));
        Map<String, Object> sameUsername = hireBody("hr.nva", "STAFF", storeA.getId());
        sameUsername.put("email", "khac@example.com");
        send(post(HIRE), adminToken, sameUsername)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_USERNAME"));
        Map<String, Object> code = hireBody("hr.trung", "STAFF", storeA.getId());
        code.put("employeeCode", "ZZHRDUP");
        hire(adminToken, code, 201);
        Map<String, Object> again = hireBody("hr.trung2", "STAFF", storeA.getId());
        again.put("employeeCode", "ZZHRDUP");
        send(post(HIRE), adminToken, again)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_EMPLOYEE_CODE"));
    }

    @Test
    void adminHire_isForAdminsOnly() throws Exception {
        Map<String, Object> body = hireBody("hr.khac", "STAFF", storeA.getId());
        send(post(HIRE), null, body).andExpect(status().isUnauthorized());
        send(post(HIRE), customerToken, body).andExpect(status().isForbidden());
        send(post(HIRE), staffAToken, body).andExpect(status().isForbidden());
        send(post(HIRE), managerAToken, body).andExpect(status().isForbidden());
    }

    @Test
    void legacyCreate_refusesAnAccountThatAlsoHasCustomer() throws Exception {
        userRoleRepository.save(new UserRole(userRepository.getReferenceById(customerId),
                roleRepository.findByName("STAFF").orElseThrow()));
        entityManager.flush();

        send(post("/api/v1/admin/employees"), adminToken, Map.of("userId", customerId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("CUSTOMER_ACCOUNT_NOT_ELIGIBLE"));
    }

    // ---------- branch manager ----------

    @Test
    void branch_onlyManagersEnter() throws Exception {
        send(get(BRANCH + "/me"), null, null).andExpect(status().isUnauthorized());
        send(get(BRANCH + "/me"), customerToken, null).andExpect(status().isForbidden());
        send(get(BRANCH + "/me"), staffAToken, null).andExpect(status().isForbidden());
        send(get(BRANCH + "/me"), adminToken, null).andExpect(status().isForbidden());
        send(get(BRANCH + "/employees"), staffAToken, null).andExpect(status().isForbidden());
        send(post(BRANCH + "/employees/hire"), staffAToken, hireBody("hr.x", "STAFF", null)).andExpect(status().isForbidden());
        send(put(BRANCH + "/employees/" + staffAEmployeeId), staffAToken, Map.of()).andExpect(status().isForbidden());
        send(post(BRANCH + "/employees/" + staffAEmployeeId + "/deactivate"), staffAToken, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void branch_me_andTheEmployeeListShowOnlyTheirOwnBranch() throws Exception {
        send(get(BRANCH + "/me"), managerAToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(storeA.getId()))
                .andExpect(jsonPath("$.data.name").value("ZZ HR Chi nhánh A"))
                .andExpect(jsonPath("$.data.activeEmployees").value(2));

        // a storeId filter cannot widen the view
        ResultActions list = send(get(BRANCH + "/employees").param("storeId", storeB.getId().toString()),
                managerAToken, null).andExpect(status().isOk());
        List<Long> ids = new ArrayList<>();
        data(list).get("content").forEach(row -> ids.add(row.get("id").asLong()));
        assertThat(ids).contains(staffAEmployeeId).doesNotContain(staffBEmployeeId);
        assertThat(ids).hasSize(2);
    }

    @Test
    void branch_hire_alwaysJoinsTheManagersOwnStore_andOnlyAsStaff() throws Exception {
        Map<String, Object> body = hireBody("hr.nvmoi", "STAFF", storeB.getId());
        JsonNode hired = hire(managerAToken, body, 201);
        assertThat(hired.get("employee").get("assignment").get("storeId").asInt()).isEqualTo(storeA.getId());
        assertThat(hired.get("role").asString()).isEqualTo("STAFF");
        assertThat(hired.get("employee").get("assignment").get("positionAtStore").asString()).isEqualTo("Nhân viên bán hàng");
        assertThat(login("hr.nvmoi", hired.get("temporaryPassword").asString())).isNotBlank();
        userIds.add(hired.get("employee").get("userId").asLong());

        send(post(BRANCH + "/employees/hire"), managerAToken, hireBody("hr.qlmoi", "BRANCH_MANAGER", storeA.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("ROLE_NOT_ASSIGNABLE"));
        send(post(BRANCH + "/employees/hire"), managerAToken, hireBody("hr.admin2", "ADMIN", storeA.getId()))
                .andExpect(status().isBadRequest());
        Map<String, Object> label = hireBody("hr.nhan2", null, null);
        label.put("positionAtStore", "Quản lý chi nhánh");
        send(post(BRANCH + "/employees/hire"), managerAToken, label)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.positionAtStore").exists());
        assertThat(userRepository.findByUsername("hr.qlmoi")).isEmpty();
    }

    @Test
    void branch_update_ownStaffOnly() throws Exception {
        send(put(BRANCH + "/employees/" + staffAEmployeeId), managerAToken, Map.of(
                "employeeCode", "ZZHR77", "department", "Kỹ thuật", "salary", 12000000, "positionAtStore", "Nhân viên kỹ thuật"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.employeeCode").value("ZZHR77"))
                .andExpect(jsonPath("$.data.department").value("Kỹ thuật"))
                .andExpect(jsonPath("$.data.assignment.positionAtStore").value("Nhân viên kỹ thuật"));

        send(put(BRANCH + "/employees/" + staffAEmployeeId), managerAToken, Map.of("positionAtStore", "Quản lý chi nhánh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.details.positionAtStore").exists());
        // another branch, and the manager's own record
        send(put(BRANCH + "/employees/" + staffBEmployeeId), managerAToken, Map.of("department", "X"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        Long managerEmployee = assignmentRepository.findActiveWithStoreByUserId(managerAUserId).orElseThrow().getEmployee().getId();
        send(put(BRANCH + "/employees/" + managerEmployee), managerAToken, Map.of("salary", 99999999))
                .andExpect(status().isForbidden());
        send(put(BRANCH + "/employees/-1"), managerAToken, Map.of())
                .andExpect(status().isNotFound());
    }

    @Test
    void branch_deactivate_endsTheAssignment_locksTheAccount_andCutsTheSession() throws Exception {
        JsonNode auth = data(send(post("/api/v1/auth/login"), null, Map.of("identifier", "hr.nva", "password", PASSWORD))
                .andExpect(status().isOk()));
        String refresh = auth.get("refreshToken").asString();

        send(post(BRANCH + "/employees/" + staffAEmployeeId + "/deactivate"), managerAToken, null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false))
                .andExpect(jsonPath("$.data.assignment").doesNotExist());

        assertThat(assignmentRepository.findActiveWithStoreByUserId(staffAUserId)).isEmpty();
        assertThat(userRepository.findById(staffAUserId).orElseThrow().isEnabled()).isFalse();
        send(post("/api/v1/auth/login"), null, Map.of("identifier", "hr.nva", "password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DISABLED"));
        send(post("/api/v1/auth/refresh"), null, Map.of("refreshToken", refresh)).andExpect(status().isUnauthorized());

        // still in the branch list as "gone", and cannot be changed again
        ResultActions gone = send(get(BRANCH + "/employees").param("active", "false"), managerAToken, null)
                .andExpect(status().isOk());
        List<Long> ids = new ArrayList<>();
        data(gone).get("content").forEach(row -> ids.add(row.get("id").asLong()));
        assertThat(ids).containsExactly(staffAEmployeeId);
        send(put(BRANCH + "/employees/" + staffAEmployeeId), managerAToken, Map.of("department", "X"))
                .andExpect(status().isForbidden());
        send(post(BRANCH + "/employees/" + staffBEmployeeId + "/deactivate"), managerAToken, null)
                .andExpect(status().isForbidden());
    }

    @Test
    void branch_aDemotedManagerLosesTheBranchApi_immediately() throws Exception {
        send(put("/api/v1/admin/users/" + managerAUserId + "/roles"), adminToken, Map.of("roles", List.of("STAFF")))
                .andExpect(status().isOk());

        // the old token still names BRANCH_MANAGER, but the service re-reads the role in the database
        send(get(BRANCH + "/me"), managerAToken, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    // ---------- helpers ----------

    private Map<String, Object> hireBody(String username, String role, Integer storeId) {
        Map<String, Object> body = new HashMap<>();
        body.put("email", username + "@example.com");
        body.put("username", username);
        body.put("fullname", "Nhân Viên " + username);
        body.put("temporaryPassword", PASSWORD);
        if (role != null) {
            body.put("role", role);
        }
        if (storeId != null) {
            body.put("storeId", storeId);
        }
        return body;
    }

    private JsonNode hire(String token, Map<String, Object> body, int expectedStatus) throws Exception {
        JsonNode data = data(send(post(token.equals(managerAToken) ? BRANCH + "/employees/hire" : HIRE), token, body)
                .andExpect(status().is(expectedStatus)));
        userIds.add(data.get("employee").get("userId").asLong());
        return data;
    }

    private String login(String username) throws Exception {
        return login(username, PASSWORD);
    }

    private String login(String username, String password) throws Exception {
        return data(send(post("/api/v1/auth/login"), null, Map.of("identifier", username, "password", password))
                .andExpect(status().isOk())).get("accessToken").asString();
    }

    private JsonNode register(String username) throws Exception {
        JsonNode data = data(send(post("/api/v1/auth/register"), null, Map.of(
                "email", username + "@example.com", "username", username, "password", PASSWORD,
                "fullname", "Người Dùng Thử"))
                .andExpect(status().isCreated()));
        userIds.add(data.get("user").get("id").asLong());
        return data;
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
