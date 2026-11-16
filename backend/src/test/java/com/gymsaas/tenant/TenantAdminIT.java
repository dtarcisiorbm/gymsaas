package com.gymsaas.tenant;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantAdminIT extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void platformAdminManagesTenantsAndTenantAdminIsRejected() throws Exception {
        String platformToken = platformToken();

        mockMvc.perform(get("/api/admin/tenants")
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", hasItem("Academia Demo")));

        String document = uniqueDocument();
        String email = "academia." + System.nanoTime() + "@example.com";
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Academia Fase Tres");
        create.put("legalName", "Academia Fase Tres Ltda");
        create.put("document", document);
        create.put("email", email);
        create.put("phone", "(11) 3333-4444");

        MvcResult created = mockMvc.perform(post("/api/admin/tenants")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.document").value(document))
                .andReturn();
        String tenantId = body(created).get("id").asText();

        mockMvc.perform(post("/api/admin/tenants")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TENANT_DOCUMENT_EXISTS"));

        mockMvc.perform(get("/api/admin/tenants/{id}", tenantId)
                        .header("Authorization", "Bearer " + platformToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.status").value("PENDING"));

        Map<String, Object> update = new HashMap<>(create);
        update.put("name", "Academia Renomeada");
        mockMvc.perform(put("/api/admin/tenants/{id}", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Academia Renomeada"));

        mockMvc.perform(patch("/api/admin/tenants/{id}/status", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(patch("/api/admin/tenants/{id}/status", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUSPENDED"));

        mockMvc.perform(patch("/api/admin/tenants/{id}/status", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "CANCELLED"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        String tenantAdminToken = loginToken("admin@gymsaas.local", "Admin@123");
        mockMvc.perform(get("/api/admin/tenants")
                        .header("Authorization", "Bearer " + tenantAdminToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void suspendedTenantBlocksUserLogin() throws Exception {
        String platformToken = platformToken();
        String document = uniqueDocument();
        Map<String, Object> create = new HashMap<>();
        create.put("name", "Academia Suspensa");
        create.put("legalName", "Academia Suspensa Ltda");
        create.put("document", document);
        create.put("email", "suspensa." + System.nanoTime() + "@example.com");

        MvcResult created = mockMvc.perform(post("/api/admin/tenants")
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(create)))
                .andExpect(status().isCreated())
                .andReturn();
        String tenantId = body(created).get("id").asText();

        mockMvc.perform(patch("/api/admin/tenants/{id}/status", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "ACTIVE"))))
                .andExpect(status().isOk());

        String email = "suspenso." + System.nanoTime() + "@example.com";
        String rawPassword = "SenhaForte@123";
        String hash = passwordEncoder.encode(rawPassword);
        UUID userId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        UUID tenantUuid = UUID.fromString(tenantId);

        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into users (id, email, password_hash, full_name, status)
                    values (?, ?, ?, 'Usuario Suspenso', 'ACTIVE')
                    """, userId, email, hash);
            jdbcTemplate.update("""
                    insert into user_tenants (id, user_id, tenant_id, is_default, status)
                    values (?, ?, ?, true, 'ACTIVE')
                    """, membershipId, userId, tenantUuid);
        });

        login(email, rawPassword);

        mockMvc.perform(patch("/api/admin/tenants/{id}/status", tenantId)
                        .header("Authorization", "Bearer " + platformToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("status", "SUSPENDED"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", rawPassword))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TENANT_INACTIVE"));
    }

    private String platformToken() throws Exception {
        MvcResult result = login("superadmin@gymsaas.local", "SuperAdmin@123");
        JsonNode session = body(result);
        org.junit.jupiter.api.Assertions.assertTrue(session.get("platformAdmin").asBoolean(),
                "superadmin must be a platform administrator");
        return session.get("accessToken").asText();
    }

    private String loginToken(String email, String password) throws Exception {
        return body(login(email, password)).get("accessToken").asText();
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

    private String uniqueDocument() {
        long value = Math.abs(System.nanoTime() % 100_000_000_000L);
        return String.format("%011d", value);
    }
}
