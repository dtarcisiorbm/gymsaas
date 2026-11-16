package com.gymsaas.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserBranchAccessIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tenantAdminManagesUsersRolesAndBranchLinks() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");
        String branchId = demoBranchId(token);
        String receptionistRoleId = roleId(token, "RECEPTIONIST");
        String trainerRoleId = roleId(token, "TRAINER");

        String email = "novo." + System.nanoTime() + "@gymsaas.local";
        String password = "SenhaForte@123";
        Map<String, Object> create = new HashMap<>();
        create.put("email", email);
        create.put("fullName", "Novo Colaborador");
        create.put("password", password);
        create.put("phone", "(11) 97777-6666");
        create.put("roleIds", java.util.List.of(receptionistRoleId));
        create.put("branchIds", java.util.List.of(branchId));

        MvcResult created = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.roles", hasItem("RECEPTIONIST")))
                .andExpect(jsonPath("$.branchIds", hasItem(branchId)))
                .andReturn();
        JsonNode user = body(created);
        String userId = user.get("id").asText();
        String membershipId = user.get("membershipId").asText();
        assertNotNull(membershipId);

        mockMvc.perform(get("/api/users")
                        .param("search", email)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].email").value(email));

        mockMvc.perform(put("/api/users/{id}/roles", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("roleIds", java.util.List.of(trainerRoleId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles", hasItem("TRAINER")))
                .andExpect(jsonPath("$.roles", not(hasItem("RECEPTIONIST"))));

        mockMvc.perform(get("/api/users/{id}/branches", userId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(branchId));

        String inactiveBranchId = createInactiveBranch(token);
        mockMvc.perform(put("/api/users/{id}/branches", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchIds", java.util.List.of(inactiveBranchId)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRANCH_INACTIVE"));

        mockMvc.perform(put("/api/users/{id}/branches", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchIds", java.util.List.of(branchId)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(branchId));

        mockMvc.perform(delete("/api/users/{id}/branches/{branchId}", userId, branchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        mockMvc.perform(post("/api/users/{id}/branches/{branchId}", userId, branchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(branchId));

        mockMvc.perform(patch("/api/users/{id}/status", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    void crossTenantBranchCannotBeLinked() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");
        String tenantId = body(mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()).get("tenant").get("id").asText();

        String email = "cross." + System.nanoTime() + "@gymsaas.local";
        MvcResult created = mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "email", email,
                                "fullName", "Usuario Cross",
                                "password", "SenhaForte@123"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode user = body(created);
        String userId = user.get("id").asText();
        String membershipId = user.get("membershipId").asText();

        UUID tenantB = UUID.randomUUID();
        UUID branchB = UUID.randomUUID();
        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, tenantB, "Academia Cross " + tenantB, "Academia Cross Ltda",
                    documentFor(tenantB), documentFor(tenantB), tenantB + "@example.com");
            jdbcTemplate.update("""
                    insert into branches (id, tenant_id, name, code, status)
                    values (?, ?, ?, ?, 'ACTIVE')
                    """, branchB, tenantB, "Unidade Cross", "CX" + branchB.toString().substring(0, 6));
        });

        mockMvc.perform(put("/api/users/{id}/branches", userId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("branchIds", java.util.List.of(branchB.toString())))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BRANCH_NOT_ALLOWED"));

        assertThrows(DataIntegrityViolationException.class, () -> TenantContext.runWithBypass(() ->
                        jdbcTemplate.update("""
                                insert into user_branch_access
                                    (id, tenant_id, user_tenant_id, branch_id, created_at)
                                values (?, ?, ?, ?, now())
                                """, UUID.randomUUID(), UUID.fromString(tenantId),
                                UUID.fromString(membershipId), branchB)),
                "composite FK must reject a branch from another tenant");
    }

    private String createInactiveBranch(String token) throws Exception {
        String code = "IN" + (Math.abs(System.nanoTime()) % 1_000_000);
        MvcResult created = mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Unidade Inativa", "code", code))))
                .andExpect(status().isCreated())
                .andReturn();
        String branchId = body(created).get("id").asText();

        mockMvc.perform(patch("/api/branches/{id}/status", branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk());
        return branchId;
    }

    private String demoBranchId(String token) throws Exception {
        JsonNode page = body(mockMvc.perform(get("/api/branches")
                        .param("search", "BR-DEMO")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        return page.get("content").get(0).get("id").asText();
    }

    private String roleId(String token, String code) throws Exception {
        JsonNode roles = body(mockMvc.perform(get("/api/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn());
        for (JsonNode role : roles) {
            if (code.equals(role.get("code").asText())) {
                return role.get("id").asText();
            }
        }
        throw new IllegalStateException("role not found: " + code);
    }

    private String loginToken(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    private String documentFor(UUID id) {
        return id.toString().replace("-", "").substring(0, 14);
    }
}
