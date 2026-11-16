package com.gymsaas.shared.infrastructure.persistence;

import com.gymsaas.shared.infrastructure.tenant.TenantContext;
import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Applies the tenant/user/platform-admin/RLS-bypass settings to every pooled
 * connection before it is handed to JPA. Values come exclusively from the
 * server-side context (JWT claims + auth flow), never from request parameters.
 */
public class RlsAwareDataSource extends DelegatingDataSource {

    private static final String SET_CONTEXT_SQL = """
            SELECT set_config('app.current_tenant', ?, false),
                   set_config('app.current_user', ?, false),
                   set_config('app.rls_bypass', ?, false),
                   set_config('app.platform_admin', ?, false)
            """;

    public RlsAwareDataSource(DataSource delegate) {
        super(delegate);
    }

    @Override
    public Connection getConnection() throws SQLException {
        Connection connection = super.getConnection();
        applyContext(connection);
        return connection;
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection connection = super.getConnection(username, password);
        applyContext(connection);
        return connection;
    }

    private void applyContext(Connection connection) throws SQLException {
        TenantContext.Context context = TenantContext.get();
        String tenantId = context != null && context.tenantId() != null ? context.tenantId().toString() : "";
        String userId = context != null && context.userId() != null ? context.userId().toString() : "";
        String bypass = context != null && context.bypass() ? "true" : "false";
        String platformAdmin = context != null && context.platformAdmin() ? "true" : "false";

        try (PreparedStatement statement = connection.prepareStatement(SET_CONTEXT_SQL)) {
            statement.setString(1, tenantId);
            statement.setString(2, userId);
            statement.setString(3, bypass);
            statement.setString(4, platformAdmin);
            statement.execute();
        }
    }
}
