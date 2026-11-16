-- V6: platform administration, branches, branch settings and user-branch links.

-- ---------------------------------------------------------------------------
-- Tenants: document column and Phase 3 statuses.
-- ---------------------------------------------------------------------------
ALTER TABLE tenants ADD COLUMN document varchar(20);
UPDATE tenants SET document = cnpj WHERE document IS NULL;
UPDATE tenants SET status = 'INACTIVE' WHERE status = 'CANCELLED';
ALTER TABLE tenants ALTER COLUMN document SET NOT NULL;

ALTER TABLE tenants DROP CONSTRAINT tenants_status_check;
ALTER TABLE tenants ADD CONSTRAINT tenants_status_check
    CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'INACTIVE'));

CREATE UNIQUE INDEX ux_tenants_document ON tenants (document) WHERE deleted_at IS NULL;

-- ---------------------------------------------------------------------------
-- Users: platform administrator flag (phone already exists since V2).
-- ---------------------------------------------------------------------------
ALTER TABLE users ADD COLUMN is_platform_admin boolean NOT NULL DEFAULT false;
CREATE INDEX ix_users_platform_admin ON users (is_platform_admin) WHERE is_platform_admin;

-- Composite key needed so user_branch_access can reference membership + tenant.
ALTER TABLE user_tenants ADD CONSTRAINT ux_user_tenants_tenant_id UNIQUE (tenant_id, id);

-- ---------------------------------------------------------------------------
-- Branches (units) belong to exactly one tenant.
-- ---------------------------------------------------------------------------
CREATE TABLE branches (
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id    uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    name         varchar(150) NOT NULL,
    code         varchar(30)  NOT NULL,
    document     varchar(20),
    email        varchar(150),
    phone        varchar(20),
    zip_code     varchar(10),
    street       varchar(200),
    number       varchar(20),
    complement   varchar(100),
    neighborhood varchar(100),
    city         varchar(100),
    state        varchar(2),
    status       varchar(20) NOT NULL DEFAULT 'ACTIVE'
                 CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_branches_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ux_branches_tenant_id UNIQUE (tenant_id, id)
);
CREATE INDEX ix_branches_tenant ON branches (tenant_id);
CREATE INDEX ix_branches_tenant_status ON branches (tenant_id, status);

CREATE TRIGGER trg_branches_updated_at BEFORE UPDATE ON branches
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Branch settings (1:1 with branch, tenant-safe composite FK).
-- ---------------------------------------------------------------------------
CREATE TABLE branch_settings (
    id              uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id       uuid NOT NULL,
    branch_id       uuid NOT NULL,
    timezone        varchar(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    opening_time    time,
    closing_time    time,
    max_capacity    integer CHECK (max_capacity IS NULL OR max_capacity > 0),
    allow_24h_access boolean NOT NULL DEFAULT false,
    settings        jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_branch_settings_branch UNIQUE (branch_id),
    CONSTRAINT fk_branch_settings_branch
        FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX ix_branch_settings_tenant ON branch_settings (tenant_id);

CREATE TRIGGER trg_branch_settings_updated_at BEFORE UPDATE ON branch_settings
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- User access to branches. Both foreign keys are composite: a membership and
-- a branch from different tenants can never be linked.
-- ---------------------------------------------------------------------------
CREATE TABLE user_branch_access (
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id      uuid NOT NULL,
    user_tenant_id uuid NOT NULL,
    branch_id      uuid NOT NULL,
    granted_by     uuid REFERENCES users (id) ON DELETE SET NULL,
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_user_branch_access_user_branch UNIQUE (user_tenant_id, branch_id),
    CONSTRAINT fk_user_branch_access_branch
        FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_user_branch_access_membership
        FOREIGN KEY (tenant_id, user_tenant_id) REFERENCES user_tenants (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX ix_user_branch_access_tenant ON user_branch_access (tenant_id);
CREATE INDEX ix_user_branch_access_membership ON user_branch_access (user_tenant_id);
CREATE INDEX ix_user_branch_access_branch ON user_branch_access (branch_id);

-- ---------------------------------------------------------------------------
-- RLS: platform administrators may administer tenants globally, but the
-- flag comes from server-side context (signed JWT), never from the client.
-- ---------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION current_is_platform_admin() RETURNS boolean
LANGUAGE sql
STABLE
AS $$
    SELECT COALESCE(NULLIF(current_setting('app.platform_admin', true), ''), 'false')::boolean
$$;

DROP POLICY tenants_isolation ON tenants;
CREATE POLICY tenants_isolation ON tenants
    USING (current_rls_bypass() OR current_is_platform_admin() OR id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR id = current_tenant_id());

DROP POLICY roles_isolation ON roles;
CREATE POLICY roles_isolation ON roles
    USING (current_rls_bypass() OR current_is_platform_admin()
           OR tenant_id IS NULL OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

DROP POLICY user_tenants_isolation ON user_tenants;
CREATE POLICY user_tenants_isolation ON user_tenants
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

DROP POLICY user_roles_isolation ON user_roles;
CREATE POLICY user_roles_isolation ON user_roles
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

DROP POLICY audit_logs_isolation ON audit_logs;
CREATE POLICY audit_logs_isolation ON audit_logs
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
CREATE POLICY branches_isolation ON branches
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE branch_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY branch_settings_isolation ON branch_settings
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE user_branch_access ENABLE ROW LEVEL SECURITY;
CREATE POLICY user_branch_access_isolation ON user_branch_access
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());
