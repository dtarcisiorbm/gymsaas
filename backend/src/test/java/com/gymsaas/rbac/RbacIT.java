package com.gymsaas.rbac;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.support.AbstractIntegrationTest;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RbacIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void receptionistCannotReadRolesOrAuditButAdminCan() throws Exception {
        String receptionToken = body(login("reception@gymsaas.local", "Reception@123")).get("accessToken").asText();
        String adminToken = body(login("admin@gymsaas.local", "Admin@123")).get("accessToken").asText();

        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + receptionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + receptionToken))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/users").header("Authorization", "Bearer " + receptionToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/roles").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("TENANT_ADMIN")));
        mockMvc.perform(get("/api/permissions").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].code", hasItem("user:assign_role")));
        mockMvc.perform(get("/api/audit-logs").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void adminCreatesUserAssignsRolesAndAuditTrailIsRecorded() throws Exception {
        String adminToken = body(login("admin@gymsaas.local", "Admin@123")).get("accessToken").asText();
        JsonNode roles = json(mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn());
        String receptionistRoleId = roleId(roles, "RECEPTIONIST");
        String trainerRoleId = roleId(roles, "TRAINER");

        String email = "new.reception." + System.nanoTime() + "@gymsaas.local";
        MvcResult created = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "fullName", "Novo Recepcionista",
                                "password", "SenhaForte@123",
                                "roleIds", List.of(receptionistRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles", hasItem("RECEPTIONIST")))
                .andReturn();
        String userId = body(created).get("id").asText();

        mockMvc.perform(put("/api/users/{userId}/roles", userId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("roleIds", List.of(trainerRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("TRAINER")))
                .andExpect(jsonPath("$.roles", org.hamcrest.Matchers.not(hasItem("RECEPTIONIST"))));

        JsonNode newUserLogin = body(login(email, "SenhaForte@123"));
        String newUserToken = newUserLogin.get("accessToken").asText();
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + newUserToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("TRAINER")));

        mockMvc.perform(get("/api/audit-logs").param("size", "100")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].action", hasItem("USER_CREATED")))
                .andExpect(jsonPath("$.content[*].action", hasItem("USER_ROLES_UPDATED")));
    }

    @Test
    void refreshTokenCookieIsScopedAndHttpOnly() throws Exception {
        MvcResult login = login("admin@gymsaas.local", "Admin@123");
        Cookie refresh = login.getResponse().getCookie("gymsaas_refresh_token");
        assertNotNull(refresh);
        assertEquals("/api/auth", refresh.getPath());
        assertEquals(true, refresh.isHttpOnly());
    }

    private MvcResult login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return result;
    }

    private JsonNode body(MvcResult result) throws Exception {
        return json(result);
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String roleId(JsonNode roles, String code) {
        List<String> ids = new ArrayList<>();
        roles.forEach(role -> {
            if (code.equals(role.get("code").asText())) {
                ids.add(role.get("id").asText());
            }
        });
        assertEquals(1, ids.size(), "expected exactly one role " + code);
        return ids.get(0);
    }
}
