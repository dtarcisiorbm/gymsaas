-- V4: Row Level Security. Enforcement relies on the runtime role being
-- NOSUPERUSER/NOBYPASSRLS (gymsaas_app); the owner role only runs migrations.

ALTER TABLE tenants       ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles         ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_tenants  ENABLE ROW LEVEL SECURITY;
ALTER TABLE user_roles    ENABLE ROW LEVEL SECURITY;
ALTER TABLE refresh_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs    ENABLE ROW LEVEL SECURITY;

-- tenants: a session only sees its own tenant unless RLS bypass is set for
-- authentication/provisioning operations.
CREATE POLICY tenants_isolation ON tenants
    USING (current_rls_bypass() OR id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR id = current_tenant_id());

-- roles: system roles (tenant_id IS NULL) are readable by everyone; tenant
-- roles are isolated.
CREATE POLICY roles_isolation ON roles
    USING (current_rls_bypass() OR tenant_id IS NULL OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR tenant_id = current_tenant_id());

CREATE POLICY user_tenants_isolation ON user_tenants
    USING (current_rls_bypass() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR tenant_id = current_tenant_id());

CREATE POLICY user_roles_isolation ON user_roles
    USING (current_rls_bypass() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR tenant_id = current_tenant_id());

CREATE POLICY audit_logs_isolation ON audit_logs
    USING (current_rls_bypass() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR tenant_id = current_tenant_id());

-- refresh_tokens: only the owning user may see tokens; auth flows run with
-- bypass because lookup happens before a tenant session exists.
CREATE POLICY refresh_tokens_isolation ON refresh_tokens
    USING (
        current_rls_bypass()
        OR (user_id = current_user_id() AND (tenant_id IS NULL OR tenant_id = current_tenant_id()))
    )
    WITH CHECK (current_rls_bypass() OR user_id = current_user_id());
