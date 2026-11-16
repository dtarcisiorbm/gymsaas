package com.gymsaas.student;

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

import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StudentIsolationIT extends AbstractIntegrationTest {

    private static final String TENANT_A_EMAIL = "admin@gymsaas.local";
    private static final String TENANT_A_PASSWORD = "Admin@123";
    private static final String TENANT_B_CPF = "39053344705";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void studentFromAnotherTenantIsInvisibleViaApiAndRls() throws Exception {
        String tokenA = loginToken(TENANT_A_EMAIL, TENANT_A_PASSWORD);

        UUID tenantB = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        UUID documentB = UUID.randomUUID();
        String reg = "2099" + String.format("%05d", Math.abs(System.nanoTime()) % 100000);
        String otherName = "Aluno Externo " + studentB;

        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, tenantB, "Academia B " + tenantB, "Academia B Ltda",
                    documentFor(tenantB), documentFor(tenantB), tenantB + "@example.com");
            jdbcTemplate.update("""
                    insert into students (id, tenant_id, registration_number, name, status)
                    values (?, ?, ?, ?, 'ACTIVE')
                    """, studentB, tenantB, reg, otherName);
            jdbcTemplate.update("""
                    insert into student_documents
                    (id, tenant_id, student_id, document_type, document_number, normalized_number, is_primary)
                    values (?, ?, ?, 'CPF', ?, ?, true)
                    """, documentB, tenantB, studentB, TENANT_B_CPF, TENANT_B_CPF);
        });

        // API must not reveal existence
        mockMvc.perform(get("/api/students/{id}", studentB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/students")
                        .param("search", "Aluno Externo")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].name", not(hasItem(otherName))));

        mockMvc.perform(get("/api/students/{id}/history", studentB)
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());

        // RLS (runtime role, NOBYPASSRLS): the row is invisible in tenant A's
        // context and visible in tenant B's, proving the check is not vacuous.
        // (Phase 5 fix: the previous version set non-existent GUCs inside an
        // unreferenced CTE, so the count was always 0.)
        UUID tenantA = demoTenantId();
        assertEquals(0, countStudentAs(tenantA, studentB));
        assertEquals(1, countStudentAs(tenantB, studentB));
    }

    private int countStudentAs(UUID tenantId, UUID studentId) {
        TenantContext.set(new TenantContext.Context(tenantId, null, false, false));
        try {
            Integer count = jdbcTemplate.queryForObject("select count(*)::int from students where id = ?",
                    Integer.class, studentId);
            return count == null ? 0 : count;
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void sameCpfAllowedInDifferentTenants() throws Exception {
        UUID tenantB = UUID.randomUUID();
        UUID studentB = UUID.randomUUID();
        String reg = "2098" + String.format("%05d", Math.abs(System.nanoTime()) % 100000);

        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, tenantB, "Academia C " + tenantB, "Academia C Ltda",
                    documentFor(tenantB), documentFor(tenantB), tenantB + "@example.com");
            jdbcTemplate.update("""
                    insert into students (id, tenant_id, registration_number, name, status)
                    values (?, ?, ?, ?, 'ACTIVE')
                    """, studentB, tenantB, reg, "Aluno Tenant B");
            jdbcTemplate.update("""
                    insert into student_documents
                    (id, tenant_id, student_id, document_type, document_number, normalized_number, is_primary)
                    values (?, ?, ?, 'CPF', ?, ?, true)
                    """, UUID.randomUUID(), tenantB, studentB, TENANT_B_CPF, TENANT_B_CPF);
        });

        // Creating a student with same CPF in tenant A must succeed
        String tokenA = loginToken(TENANT_A_EMAIL, TENANT_A_PASSWORD);
        mockMvc.perform(post("/api/students")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("name", "Aluno Tenant A", "cpf", TENANT_B_CPF))))
                .andExpect(status().isCreated());
    }

    private UUID demoTenantId() {
        return TenantContext.callWithBypass(() -> jdbcTemplate.queryForObject(
                "select id from tenants where name = 'Academia Demo' limit 1", UUID.class));
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
