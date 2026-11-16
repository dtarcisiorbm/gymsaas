package com.gymsaas.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Shared helpers for Phase 5 (plans, memberships, contracts) integration tests. */
public abstract class Phase5IntegrationSupport extends AbstractIntegrationTest {

    protected static final String ADMIN = "admin@gymsaas.local";
    protected static final String ADMIN_PASSWORD = "Admin@123";
    protected static final String RECEPTION = "reception@gymsaas.local";
    protected static final String RECEPTION_PASSWORD = "Reception@123";
    /** Demo tenant uses the default time zone from V2. */
    protected static final ZoneId TENANT_ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected JdbcTemplate jdbcTemplate;

    protected LocalDate today() {
        return LocalDate.now(TENANT_ZONE);
    }

    protected String adminToken() throws Exception {
        return login(ADMIN, ADMIN_PASSWORD);
    }

    protected String receptionToken() throws Exception {
        return login(RECEPTION, RECEPTION_PASSWORD);
    }

    protected String login(String email, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return body(result).get("accessToken").asText();
    }

    protected ResultActions postJson(String token, String url, Object payload) throws Exception {
        return mockMvc.perform(post(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(payload)));
    }

    protected ResultActions putJson(String token, String url, Object payload) throws Exception {
        return mockMvc.perform(put(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(payload)));
    }

    protected ResultActions patchJson(String token, String url, Object payload) throws Exception {
        return mockMvc.perform(patch(url).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json(payload)));
    }

    protected ResultActions getJson(String token, String url) throws Exception {
        return mockMvc.perform(get(url).header("Authorization", "Bearer " + token));
    }

    protected JsonNode body(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected static String unique(String prefix) {
        return prefix + Long.toString(Math.abs(System.nanoTime()) % 2_000_000_000L, 36).toUpperCase();
    }

    protected UUID demoBranchId(String token) throws Exception {
        JsonNode page = body(getJson(token, "/api/branches?size=100").andExpect(status().isOk()).andReturn());
        for (JsonNode branch : page.get("content")) {
            if ("BR-DEMO".equals(branch.get("code").asText())) {
                return UUID.fromString(branch.get("id").asText());
            }
        }
        throw new IllegalStateException("Demo branch not found");
    }

    protected UUID createStudent(String token, String name) throws Exception {
        MvcResult result = postJson(token, "/api/students", Map.of("name", name))
                .andExpect(status().isCreated()).andReturn();
        return UUID.fromString(body(result).get("id").asText());
    }

    protected Map<String, Object> planPayload(String code, String amount) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("name", "Plano " + code);
        payload.put("durationType", "MONTHLY");
        payload.put("durationValue", 1);
        payload.put("freezePolicy", Map.of("maxFreezes", 2, "maxFreezeDays", 30));
        payload.put("contractTerms", "Cláusulas de teste definidas pela academia.");
        payload.put("initialPrice", Map.of("amount", amount, "billingPeriod", "MONTHLY", "setupFee", "20.00"));
        return payload;
    }

    protected JsonNode createPlan(String token, String amount) throws Exception {
        return body(postJson(token, "/api/plans", planPayload(unique("P"), amount))
                .andExpect(status().isCreated()).andReturn());
    }

    protected Map<String, Object> membershipPayload(UUID studentId, UUID planId, UUID branchId) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("studentId", studentId.toString());
        payload.put("planId", planId.toString());
        payload.put("branchId", branchId.toString());
        return payload;
    }

    protected JsonNode createMembership(String token, UUID studentId, UUID planId, UUID branchId) throws Exception {
        return body(postJson(token, "/api/memberships", membershipPayload(studentId, planId, branchId))
                .andExpect(status().isCreated()).andReturn());
    }

    protected UUID demoTenantId() {
        return TenantContext.callWithBypass(() -> jdbcTemplate.queryForObject(
                "select id from tenants where name = 'Academia Demo' limit 1", UUID.class));
    }

    /** Runs application code as a system job of the given tenant (RLS scoped, no bypass). */
    protected <T> T asTenant(UUID tenantId, Supplier<T> action) {
        TenantContext.set(new TenantContext.Context(tenantId, null, false, false));
        try {
            return action.get();
        } finally {
            TenantContext.clear();
        }
    }

    protected static UUID id(JsonNode node) {
        return UUID.fromString(node.get("id").asText());
    }
}
