package com.gymsaas.membership;

import com.fasterxml.jackson.databind.JsonNode;
import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import com.gymsaas.support.Phase5IntegrationSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Plans, prices, memberships and contracts of another tenant are invisible (API + real RLS + composite FKs). */
class MembershipIsolationIT extends Phase5IntegrationSupport {

    @Autowired DataSource dataSource;

    record Other(UUID tenantId, UUID branchId, UUID studentId, UUID planId, UUID priceId, UUID membershipId) {
    }

    @Test
    void otherTenantDataIsInvisibleAndUnusable() throws Exception {
        String token = adminToken();
        Other other = otherTenant();
        UUID branchA = demoBranchId(token);
        UUID studentA = createStudent(token, "Aluno Isolamento " + unique(""));
        JsonNode planA = createPlan(token, "70.00");

        getJson(token, "/api/plans/" + other.planId()).andExpect(status().isNotFound());
        getJson(token, "/api/plans/" + other.planId() + "/prices").andExpect(status().isNotFound());
        getJson(token, "/api/memberships/" + other.membershipId()).andExpect(status().isNotFound());
        getJson(token, "/api/memberships/" + other.membershipId() + "/history").andExpect(status().isNotFound());
        getJson(token, "/api/memberships/" + other.membershipId() + "/contracts").andExpect(status().isNotFound());
        getJson(token, "/api/plans?size=100").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(other.planId().toString()))));
        getJson(token, "/api/memberships?size=100").andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", not(hasItem(other.membershipId().toString()))));

        // Contracting with foreign plan / student / branch is refused as "not found".
        postJson(token, "/api/memberships", membershipPayload(studentA, other.planId(), branchA))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("PLAN_NOT_FOUND"));
        postJson(token, "/api/memberships", membershipPayload(other.studentId(), id(planA), branchA))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("STUDENT_NOT_FOUND"));
        postJson(token, "/api/memberships", membershipPayload(studentA, id(planA), other.branchId()))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("BRANCH_NOT_FOUND"));
        postJson(token, "/api/memberships/" + other.membershipId() + "/cancel",
                java.util.Map.of("reason", "x")).andExpect(status().isNotFound());

        // Real RLS as the runtime role: invisible in tenant A, visible in tenant B (not vacuous).
        UUID tenantA = demoTenantId();
        assertEquals(0, countAs(tenantA, "plans", other.planId()));
        assertEquals(0, countAs(tenantA, "student_memberships", other.membershipId()));
        assertEquals(1, countAs(other.tenantId(), "plans", other.planId()));
        assertEquals(1, countAs(other.tenantId(), "student_memberships", other.membershipId()));
        assertEquals(1, countAs(tenantA, "plans", id(planA)));

        // RLS WITH CHECK: tenant A cannot write rows of tenant B.
        assertThrows(SQLException.class, () -> insertPlanAs(tenantA, other.tenantId()));
    }

    @Test
    void compositeForeignKeysRejectCrossTenantReferences() throws Exception {
        String token = adminToken();
        Other other = otherTenant();
        UUID tenantA = demoTenantId();
        UUID branchA = demoBranchId(token);
        UUID studentA = createStudent(token, "Aluno FK " + unique(""));
        // Tenant A membership pointing at tenant B's plan/price must fail at the database level.
        assertThrows(DataAccessException.class, () -> TenantContext.runWithBypass(() -> jdbcTemplate.update("""
                insert into student_memberships (tenant_id, student_id, plan_id, plan_price_id, branch_id,
                    registration_number, status, start_date, end_date, original_end_date, anchor_date,
                    duration_type, duration_value, amount, currency, billing_period, billing_interval,
                    final_amount, conditions_snapshot)
                values (?, ?, ?, ?, ?, ?, 'ACTIVE', current_date, current_date + 29, current_date + 29,
                    current_date, 'MONTHLY', 1, 10, 'BRL', 'MONTHLY', 1, 10, '{}'::jsonb)
                """, tenantA, studentA, other.planId(), other.priceId(), branchA, unique("X"))));
    }

    private Other otherTenant() {
        UUID tenantId = UUID.randomUUID();
        UUID branchId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID planId = UUID.randomUUID();
        UUID priceId = UUID.randomUUID();
        UUID membershipId = UUID.randomUUID();
        String document = tenantId.toString().replace("-", "").substring(0, 14);
        TenantContext.runWithBypass(() -> {
            jdbcTemplate.update("""
                    insert into tenants (id, name, legal_name, document, cnpj, email, status)
                    values (?, ?, 'Outra Ltda', ?, ?, ?, 'ACTIVE')
                    """, tenantId, "Outra Academia " + tenantId, document, document, tenantId + "@example.com");
            jdbcTemplate.update("insert into branches (id, tenant_id, name, code) values (?, ?, 'Outra', 'OUT')",
                    branchId, tenantId);
            jdbcTemplate.update("""
                    insert into students (id, tenant_id, registration_number, name, status)
                    values (?, ?, '202700001', 'Aluno Externo', 'ACTIVE')
                    """, studentId, tenantId);
            jdbcTemplate.update("""
                    insert into plans (id, tenant_id, code, name, duration_type, duration_value)
                    values (?, ?, 'EXT', 'Plano Externo', 'MONTHLY', 1)
                    """, planId, tenantId);
            jdbcTemplate.update("""
                    insert into plan_prices (id, tenant_id, plan_id, amount, billing_period, valid_from)
                    values (?, ?, ?, 50.00, 'MONTHLY', current_date - 10)
                    """, priceId, tenantId, planId);
            jdbcTemplate.update("""
                    insert into student_memberships (id, tenant_id, student_id, plan_id, plan_price_id, branch_id,
                        registration_number, status, start_date, end_date, original_end_date, anchor_date,
                        duration_type, duration_value, amount, currency, billing_period, billing_interval,
                        final_amount, conditions_snapshot)
                    values (?, ?, ?, ?, ?, ?, 'M2027000001', 'ACTIVE', current_date, current_date + 29,
                        current_date + 29, current_date, 'MONTHLY', 1, 50.00, 'BRL', 'MONTHLY', 1, 50.00,
                        '{}'::jsonb)
                    """, membershipId, tenantId, studentId, planId, priceId, branchId);
        });
        return new Other(tenantId, branchId, studentId, planId, priceId, membershipId);
    }

    private int countAs(UUID tenantId, String table, UUID id) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            setTenant(connection, tenantId);
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

    private void insertPlanAs(UUID contextTenant, UUID rowTenant) throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            setTenant(connection, contextTenant);
            try (PreparedStatement statement = connection.prepareStatement("""
                    insert into plans (tenant_id, code, name, duration_type, duration_value)
                    values (?, 'HACK', 'Hack', 'MONTHLY', 1)
                    """)) {
                statement.setObject(1, rowTenant);
                statement.executeUpdate();
            }
        }
    }

    private static void setTenant(Connection connection, UUID tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select set_config('app.current_tenant', ?, false),
                       set_config('app.current_user', '', false),
                       set_config('app.rls_bypass', 'false', false),
                       set_config('app.platform_admin', 'false', false)
                """)) {
            statement.setString(1, tenantId.toString());
            statement.execute();
        }
    }
}
