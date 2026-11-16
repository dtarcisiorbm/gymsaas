package com.gymsaas.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthFlowIT extends AbstractIntegrationTest {

    private static final String COOKIE = "gymsaas_refresh_token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginMeRefreshRotationReuseDetectionAndLogout() throws Exception {
        MvcResult login = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@gymsaas.local", "password", "Admin@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.tenants[0].name").value("Academia Demo"))
                .andExpect(jsonPath("$.activeTenantId").isNotEmpty())
                .andExpect(cookie().httpOnly(COOKIE, true))
                .andReturn();

        String accessToken = body(login).get("accessToken").asText();
        String tenantId = body(login).get("activeTenantId").asText();
        Cookie firstRefresh = login.getResponse().getCookie(COOKIE);
        assertNotNull(firstRefresh);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.id").value(tenantId))
                .andExpect(jsonPath("$.roles", hasItem("TENANT_ADMIN")))
                .andExpect(jsonPath("$.permissions", hasItem("user:read")));

        MvcResult refreshed = mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();
        Cookie secondRefresh = refreshed.getResponse().getCookie(COOKIE);
        assertNotNull(secondRefresh);
        assertNotEquals(firstRefresh.getValue(), secondRefresh.getValue(),
                "refresh token must rotate on every use");

        // Replaying a rotated token must invalidate the whole family.
        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefresh))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH"));

        mockMvc.perform(post("/api/auth/refresh").cookie(secondRefresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRevokesFamilyImmediately() throws Exception {
        MvcResult login = login("admin@gymsaas.local", "Admin@123");
        String accessToken = body(login).get("accessToken").asText();
        Cookie refresh = login.getResponse().getCookie(COOKIE);

        mockMvc.perform(post("/api/auth/logout").cookie(refresh))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh").cookie(refresh))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidCredentialsWithoutIssuingCookie() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "admin@gymsaas.local", "password", "wrong-password"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"))
                .andExpect(cookie().doesNotExist(COOKIE));
    }

    @Test
    void selectTenantIssuesTokenScopedToAuthorizedAcademy() throws Exception {
        MvcResult login = login("admin@gymsaas.local", "Admin@123");
        String accessToken = body(login).get("accessToken").asText();
        String tenantId = body(login).get("activeTenantId").asText();

        MvcResult selection = mockMvc.perform(post("/api/auth/select-tenant")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("tenantId", tenantId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenant.id").value(tenantId))
                .andReturn();

        String scopedToken = body(selection).get("accessToken").asText();
        assertEquals(tenantId, body(login).get("activeTenantId").asText());
        mockMvc.perform(get("/api/tenants/current").header("Authorization", "Bearer " + scopedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId));
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
