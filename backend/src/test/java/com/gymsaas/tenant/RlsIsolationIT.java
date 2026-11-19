package com.gymsaas.tenant;

import com.gymsaas.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies RLS as the runtime role (NOSUPERUSER / NOBYPASSRLS): rows of other
 * tenants are invisible and cross-tenant writes are rejected by the database,
 * not only by application code.
 */
class RlsIsolationIT extends AbstractIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void runtimeRoleIsNotSuperuser() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(
                     "select rolsuper, rolbypassrls from pg_roles where rolname = current_user")) {
            assertTrue(result.next());
            assertFalse(result.getBoolean("rolsuper"), "runtime role must not be superuser");
            assertFalse(result.getBoolean("rolbypassrls"), "runtime role must not bypass RLS");
        }
    }

    @Test
    void migrationsAppliedAndRlsEnabledOnTenantTables() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            setContext(connection, null, null, true);
            try (Statement statement = connection.createStatement()) {
                try (ResultSet result = statement.executeQuery(
                        "select count(*) from flyway_schema_history where success")) {
                    assertTrue(result.next());
                    assertEquals(13, result.getInt(1), "all migrations must be applied");
                }
                try (ResultSet result = statement.executeQuery("""
                        select count(*) from pg_class c
                        join pg_namespace n on n.oid = c.relnamespace
                        where n.nspname = 'public' and c.relrowsecurity
                          and c.relname in ('tenants', 'roles', 'user_tenants',
                                            'user_roles', 'refresh_tokens', 'audit_logs',
                                            'branches', 'branch_settings', 'user_branch_access',
                                            'students', 'student_documents', 'student_contacts',
                                            'student_addresses', 'student_branch_assignments',
                                            'student_status_history', 'student_notes',
                                            'student_registration_counters',
                                            'plans', 'plan_prices', 'plan_features',
                                            'membership_number_counters', 'student_memberships',
                                            'membership_freezes', 'membership_status_history',
                                            'membership_contracts', 'contract_acceptances',
                                            'billing_number_counters', 'charges', 'payments')
                        """)) {
                    assertTrue(result.next());
                    assertEquals(29, result.getInt(1), "RLS must be enabled on tenant-scoped tables");
                }
            } finally {
                resetContext(connection);
            }
        }
    }

    @Test
    void tenantContextHidesOtherTenantRowsAndBlocksCrossTenantWrites() throws SQLException {
        UUID tenantA = UUID.randomUUID();
        UUID tenantB = UUID.randomUUID();
        UUID userA = UUID.randomUUID();
        UUID membershipA = UUID.randomUUID();

        try (Connection connection = dataSource.getConnection()) {
            setContext(connection, null, null, true);
            try {
                insertTenant(connection, tenantA, "Tenant A " + tenantA);
                insertTenant(connection, tenantB, "Tenant B " + tenantB);
                insertUser(connection, userA);
                insertMembership(connection, membershipA, userA, tenantA);
            } finally {
                resetContext(connection);
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            setContext(connection, tenantA, userA, false);
            try {
                assertEquals(1, count(connection, "select count(*) from tenants"),
                        "only the active tenant must be visible");
                assertEquals(1, count(connection,
                                "select count(*) from user_tenants where tenant_id = '" + tenantA + "'"),
                        "membership of the active tenant must be visible");
                assertEquals(0, count(connection,
                                "select count(*) from user_tenants where tenant_id = '" + tenantB + "'"),
                        "membership of another tenant must be invisible");
                assertEquals(0, count(connection,
                                "select count(*) from tenants where id = '" + tenantB + "'"),
                        "another tenant must be invisible");

                assertThrows(SQLException.class, () ->
                                insertMembership(connection, UUID.randomUUID(), userA, tenantB),
                        "RLS WITH CHECK must reject cross-tenant inserts");
            } finally {
                resetContext(connection);
            }
        }
    }

    private void insertTenant(Connection connection, UUID id, String name) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into tenants (id, name, legal_name, document, cnpj, email, status)
                values (?, ?, ?, ?, ?, ?, 'ACTIVE')
                """)) {
            statement.setObject(1, id);
            statement.setString(2, name);
            statement.setString(3, name + " LTDA");
            statement.setString(4, id.toString().replace("-", "").substring(0, 14));
            statement.setString(5, id.toString().replace("-", "").substring(0, 14));
            statement.setString(6, id + "@example.com");
            statement.executeUpdate();
        }
    }

    private void insertUser(Connection connection, UUID id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into users (id, email, password_hash, full_name, status)
                values (?, ?, 'argon2-placeholder', 'Tenant User', 'ACTIVE')
                """)) {
            statement.setObject(1, id);
            statement.setString(2, id + "@example.com");
            statement.executeUpdate();
        }
    }

    private void insertMembership(Connection connection, UUID id, UUID userId, UUID tenantId)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                insert into user_tenants (id, user_id, tenant_id, is_default, status)
                values (?, ?, ?, false, 'ACTIVE')
                """)) {
            statement.setObject(1, id);
            statement.setObject(2, userId);
            statement.setObject(3, tenantId);
            statement.executeUpdate();
        }
    }

    private int count(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            assertTrue(result.next());
            return result.getInt(1);
        }
    }

    private void setContext(Connection connection, UUID tenantId, UUID userId, boolean bypass)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                select set_config('app.current_tenant', ?, false),
                       set_config('app.current_user', ?, false),
                       set_config('app.rls_bypass', ?, false)
                """)) {
            statement.setString(1, tenantId == null ? "" : tenantId.toString());
            statement.setString(2, userId == null ? "" : userId.toString());
            statement.setString(3, Boolean.toString(bypass));
            statement.execute();
        }
    }

    private void resetContext(Connection connection) throws SQLException {
        setContext(connection, null, null, false);
    }
}
