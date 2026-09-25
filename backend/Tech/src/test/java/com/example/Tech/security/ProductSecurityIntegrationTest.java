package com.example.Tech.security;

import com.example.Tech.entity.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Authorization rules of decision D4 for all 8 catalogue resources, using real access tokens.
 * Writes by ADMIN send an empty body, so they stop at validation (400) and create nothing.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductSecurityIntegrationTest {

    private static final String UNKNOWN_ID = "999999";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    private String token(String... roles) {
        User user = new User();
        user.setId(123456L);
        user.setUsername("security-test");
        return "Bearer " + jwtTokenService.issueAccessToken(user, List.of(roles));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder builder) {
        return builder.contentType(MediaType.APPLICATION_JSON).content("{}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"categories", "brands", "products", "product-variants", "attributes",
            "attribute-values", "product-images", "product-specifications"})
    void catalogueResource_readsArePublic_writesRequireAdmin(String resource) throws Exception {
        String collection = "/api/v1/" + resource;
        String item = collection + "/" + UNKNOWN_ID;

        // GET is public: an unknown id gives 404 from the service, not 401/403
        mockMvc.perform(get(item)).andExpect(status().isNotFound());

        // anonymous write → 401
        mockMvc.perform(json(post(collection)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        // CUSTOMER / STAFF write → 403
        for (String role : List.of("CUSTOMER", "STAFF")) {
            mockMvc.perform(json(post(collection)).header(HttpHeaders.AUTHORIZATION, token(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        }
        mockMvc.perform(json(put(item)).header(HttpHeaders.AUTHORIZATION, token("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(item).header(HttpHeaders.AUTHORIZATION, token("CUSTOMER")))
                .andExpect(status().isForbidden());

        // ADMIN passes authorization and reaches validation
        mockMvc.perform(json(post(collection)).header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void publicLists_workWithoutToken() throws Exception {
        for (String path : List.of("/api/v1/categories", "/api/v1/brands", "/api/v1/products", "/api/v1/attributes")) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }
    }

    @Test
    void admin_canDeleteAndUpdate_reachingTheService() throws Exception {
        mockMvc.perform(delete("/api/v1/brands/" + UNKNOWN_ID).header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("BRAND_NOT_FOUND"));
        mockMvc.perform(post("/api/v1/product-variants/" + UNKNOWN_ID + "/attribute-values/" + UNKNOWN_ID)
                        .header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerAlsoHavingAdminRole_isAllowed() throws Exception {
        mockMvc.perform(json(post("/api/v1/brands")).header(HttpHeaders.AUTHORIZATION, token("CUSTOMER", "ADMIN")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminArea_requiresAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, token("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
        // an ADMIN token passes the URL rule, but the service re-checks the caller in the database;
        // this token belongs to a user id that does not exist
        mockMvc.perform(get("/api/v1/admin/users").header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
        mockMvc.perform(get("/api/v1/admin/not-mapped-yet").header(HttpHeaders.AUTHORIZATION, token("ADMIN")))
                .andExpect(status().isNotFound());
    }

    @Test
    void otherApiPaths_requireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/not-mapped-yet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
        mockMvc.perform(get("/api/v1/not-mapped-yet").header(HttpHeaders.AUTHORIZATION, token("CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void authEndpoints_stayPublic() throws Exception {
        mockMvc.perform(json(post("/api/v1/auth/login")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        mockMvc.perform(get("/api/v1/auth/login"))
                .andExpect(status().isUnauthorized());
    }
}
