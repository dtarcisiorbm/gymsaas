package com.gymsaas.branch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BranchManagementIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void tenantAdminManagesBranchesAndSettings() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");

        mockMvc.perform(get("/api/branches").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].code", hasItem("BR-DEMO")));

        String code = "BR" + (Math.abs(System.nanoTime()) % 1_000_000);
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Unidade Nova");
        create.put("code", code);
        create.put("city", "Campinas");
        create.put("state", "SP");
        create.put("phone", "(19) 98888-7777");
        create.put("zipCode", "13010-000");

        MvcResult created = mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andReturn();
        String branchId = body(created).get("id").asText();

        mockMvc.perform(post("/api/branches")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BRANCH_CODE_EXISTS"));

        mockMvc.perform(get("/api/branches/{id}", branchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(branchId))
                .andExpect(jsonPath("$.city").value("Campinas"));

        Map<String, Object> update = new HashMap<>(create);
        update.put("name", "Unidade Nova Renomeada");
        mockMvc.perform(put("/api/branches/{id}", branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Unidade Nova Renomeada"));

        mockMvc.perform(get("/api/branches/{id}/settings", branchId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timezone").value("America/Sao_Paulo"));

        mockMvc.perform(put("/api/branches/{id}/settings", branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "timezone", "America/Sao_Paulo",
                                "openingTime", "06:00",
                                "closingTime", "23:00",
                                "maxCapacity", 120,
                                "allow24hAccess", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxCapacity").value(120))
                .andExpect(jsonPath("$.allow24hAccess").value(true));

        mockMvc.perform(patch("/api/branches/{id}/status", branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));

        mockMvc.perform(patch("/api/branches/{id}/status", branchId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void branchFromAnotherTenantIsInvisible() throws Exception {
        String token = loginToken("admin@gymsaas.local", "Admin@123");

        UUID tenantB = UUID.randomUUID();
        UUID branchB = UUID.randomUUID();
        String otherBranchName = "Unidade Externa " + branchB;

        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, tenantB, "Academia Externa " + tenantB, "Academia Externa Ltda",
                    documentFor(tenantB), documentFor(tenantB), tenantB + "@example.com");
            jdbcTemplate.update("""
                    insert into branches (id, tenant_id, name, code, status)
                    values (?, ?, ?, ?, 'ACTIVE')
                    """, branchB, tenantB, otherBranchName, "EXT" + branchB.toString().substring(0, 6));
        });

        mockMvc.perform(get("/api/branches/{id}", branchB)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/branches")
                        .param("search", "Unidade Externa")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", not(hasItem(otherBranchName))))
                .andExpect(jsonPath("$.totalElements").value(0));
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
