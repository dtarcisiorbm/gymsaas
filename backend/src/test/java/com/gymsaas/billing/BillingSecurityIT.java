package com.gymsaas.billing;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.MediaType;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Multi-tenant isolation (API + real RLS), database immutability, concurrency and permissions. */
class BillingSecurityIT extends BillingIntegrationSupport {

    @Autowired DataSource dataSource;

    record Other(UUID tenantId, UUID branchId, UUID studentId, UUID chargeId, UUID paymentId) {
    }

    @Test
    void otherTenantChargesAndPaymentsAreInvisibleAndUnusable() throws Exception {
        String token = adminToken();
        Other other = otherTenant();
        UUID branchA = demoBranchId(token);
        UUID studentA = createStudent(token, "Aluno Isolamento Fin " + unique(""));
        JsonNode chargeA = manualCharge(token, studentA, branchA, "12.00", today().toString());

        getJson(token, "/api/charges/" + other.chargeId()).andExpect(status().isNotFound());
        getJson(token, "/api/charges?size=100").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(other.chargeId().toString()))));
        getJson(token, "/api/charges?studentId=" + other.studentId()).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
        postJson(token, "/api/charges/" + other.chargeId() + "/payments", payment("50.00", "PIX"))
                .andExpect(status().isNotFound());
        postJson(token, "/api/charges/" + other.chargeId() + "/cancel", Map.of("reason", "x"))
                .andExpect(status().isNotFound());
        postJson(token, "/api/payments/" + other.paymentId() + "/refund", Map.of("reason", "x"))
                .andExpect(status().isNotFound());

        Map<String, Object> foreign = new java.util.HashMap<>();
        foreign.put("studentId", other.studentId().toString());
        foreign.put("branchId", branchA.toString());
        foreign.put("description", "x");
        foreign.put("amount", "1.00");
        foreign.put("dueDate", today().toString());
        postJson(token, "/api/charges", foreign).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        foreign.put("studentId", studentA.toString());
        foreign.put("branchId", other.branchId().toString());
        postJson(token, "/api/charges", foreign).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));

        // Real RLS as the runtime role (not vacuous: tenant B sees its own rows).
        UUID tenantA = demoTenantId();
        assertEquals(0, countAs(tenantA, "charges", other.chargeId()));
        assertEquals(0, countAs(tenantA, "payments", other.paymentId()));
        assertEquals(1, countAs(other.tenantId(), "charges", other.chargeId()));
        assertEquals(1, countAs(other.tenantId(), "payments", other.paymentId()));
        assertEquals(1, countAs(tenantA, "charges", id(chargeA)));

        // Composite FK: a tenant A charge cannot point at tenant B's student.
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update("""
                insert into charges (tenant_id, charge_number, student_id, branch_id, kind, description, amount,
                    due_date)
                values (?, ?, ?, ?, 'MANUAL', 'x', 1, current_date)
                """, tenantA, unique("CX"), other.studentId(), branchA)));
    }

    @Test
    void triggersKeepValuesImmutableAndRowsUndeletable() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Trigger " + unique(""));
        JsonNode charge = manualCharge(token, studentId, branchId, "20.00", today().toString());
        JsonNode paid = body(postJson(token, "/api/charges/" + id(charge) + "/payments", payment("20.00", "CASH"))
                .andExpect(status().isCreated()).andReturn());
        UUID paymentId = UUID.fromString(paid.get("payments").get(0).get("id").asText());

        assertThrows(DataAccessException.class, () -> bypass("update charges set amount = 1 where id = ?",
                id(charge)));
        assertThrows(DataAccessException.class, () -> bypass("update charges set due_date = due_date + 1 where id = ?",
                id(charge)));
        assertThrows(DataAccessException.class, () -> bypass("delete from charges where id = ?", id(charge)));
        assertThrows(DataAccessException.class, () -> bypass(
                "update charges set status = 'CANCELLED', cancelled_at = now(), cancellation_reason = 'x', "
                        + "paid_at = null where id = ?", id(charge)));
        assertThrows(DataAccessException.class, () -> bypass("update payments set amount = 1 where id = ?",
                paymentId));
        assertThrows(DataAccessException.class, () -> bypass("delete from payments where id = ?", paymentId));
        // A second confirmed payment for the same charge violates the partial unique index.
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update("""
                insert into payments (tenant_id, payment_number, charge_id, student_id, branch_id, amount, method,
                    paid_at)
                select tenant_id, ?, id, student_id, branch_id, amount, 'PIX', now() from charges where id = ?
                """, unique("RX"), id(charge))));
    }

    @Test
    void concurrentPaymentsSettleTheChargeOnlyOnce() throws Exception {
        String token = adminToken();
        UUID branchId = demoBranchId(token);
        UUID studentId = createStudent(token, "Aluno Concorrência Fin " + unique(""));
        JsonNode charge = manualCharge(token, studentId, branchId, "33.00", today().toString());
        String url = "/api/charges/" + id(charge) + "/payments";
        String payload = json(payment("33.00", "PIX"));

        int threads = 5;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Integer> statuses = new ArrayList<>();
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (int index = 0; index < threads; index++) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return mockMvc.perform(post(url).header("Authorization", "Bearer " + token)
                                    .contentType(MediaType.APPLICATION_JSON).content(payload))
                            .andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            for (Future<Integer> future : futures) {
                statuses.add(future.get(30, TimeUnit.SECONDS));
            }
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, statuses.stream().filter(code -> code == 201).count(), statuses.toString());
        assertTrue(statuses.stream().filter(code -> code != 201).allMatch(code -> code == 409), statuses.toString());
        JsonNode detail = body(getJson(token, "/api/charges/" + id(charge)).andReturn());
        assertEquals(1, detail.get("payments").size());
        assertEquals("PAID", detail.get("charge").get("status").asText());
    }

    @Test
    void receptionCanReadCreateAndReceiveButNotCancelRefundOrReport() throws Exception {
        String admin = adminToken();
        String reception = receptionToken();
        UUID branchId = demoBranchId(admin);
        UUID studentId = createStudent(admin, "Aluno Permissão Fin " + unique(""));
        JsonNode charge = manualCharge(reception, studentId, branchId, "9.00", today().toString());
        getJson(reception, "/api/charges/" + id(charge)).andExpect(status().isOk());
        getJson(reception, "/api/charges?studentId=" + studentId).andExpect(status().isOk());
        postJson(reception, "/api/charges/" + id(charge) + "/cancel", Map.of("reason", "x"))
                .andExpect(status().isForbidden());
        JsonNode paid = body(postJson(reception, "/api/charges/" + id(charge) + "/payments", payment("9.00", "CASH"))
                .andExpect(status().isCreated()).andReturn());
        postJson(reception, "/api/payments/" + paid.get("payments").get(0).get("id").asText() + "/refund",
                Map.of("reason", "x")).andExpect(status().isForbidden());
        getJson(reception, "/api/billing/summary").andExpect(status().isForbidden());
        getJson(admin, "/api/billing/summary").andExpect(status().isOk());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/charges"))
                .andExpect(status().isUnauthorized());
    }

    private void bypass(String sql, UUID id) {
        TenantContext.runWithBypass(() -> jdbcTemplate.update(sql, id));
    }

    private Other otherTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID chargeId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        String document = tenantId.toString().replace("-", "").substring(0, 14);
        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, 'Outra Fin Ltda', ?, ?, ?, 'ACTIVE')
                    """, tenantId, "Outra Academia Fin " + tenantId, document, document, tenantId + "@example.com");
            jdbcTemplate.update("insert into branches (id, tenant_id, name, code) values (?, ?, 'Outra', 'OUT')",
                    branchId, tenantId);
            jdbcTemplate.update("""
                    insert into students (id, tenant_id, registration_number, name, status)
                    values (?, ?, '202700001', 'Aluno Externo', 'ACTIVE')
                    """, studentId, tenantId);
            jdbcTemplate.update("""
                    insert into charges (id, tenant_id, charge_number, student_id, branch_id, kind, description,
                        amount, due_date, status, paid_at)
                    values (?, ?, 'C2027000001', ?, ?, 'MANUAL', 'Externa', 50.00, current_date, 'PAID', now())
                    """, chargeId, tenantId, studentId, branchId);
            jdbcTemplate.update("""
                    insert into payments (id, tenant_id, payment_number, charge_id, student_id, branch_id, amount,
                        method, paid_at)
                    values (?, ?, 'R2027000001', ?, ?, ?, 50.00, 'PIX', now())
                    """, paymentId, tenantId, chargeId, studentId, branchId);
        });
        return new Other(tenantId, branchId, studentId, chargeId, paymentId);
    }

    private int countAs(UUID tenantId, String table, UUID id) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    select set_config('app.current_tenant', ?, false),
                           set_config('app.current_user', '', false),
                           set_config('app.rls_bypass', 'false', false),
                           set_config('app.platform_admin', 'false', false)
                    """)) {
                statement.setString(1, tenantId.toString());
                statement.execute();
            }
            try (PreparedStatement statement = connection.prepareStatement(
                    "select count(*) from " + table + " where id = ?")) {
                statement.setObject(1, id);
                try (ResultSet result = statement.executeQuery()) {
                    assertTrue(result.next());
                    return result.getInt(1);
                }
            }
        }
    }
}
