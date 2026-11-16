-- V2: tenants, users, RBAC and tenant membership.

CREATE TABLE tenants (
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    name        varchar(150) NOT NULL,
    legal_name  varchar(200) NOT NULL,
    cnpj        varchar(18)  NOT NULL,
    email       varchar(150) NOT NULL,
    phone       varchar(20),
    address     jsonb,
    status      varchar(20) NOT NULL DEFAULT 'ACTIVE'
                CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    settings    jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now(),
    deleted_at  timestamptz
);
CREATE UNIQUE INDEX ux_tenants_cnpj ON tenants (cnpj) WHERE deleted_at IS NULL;
CREATE INDEX ix_tenants_status ON tenants (status);

CREATE TABLE users (
    id            uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    email         varchar(150) NOT NULL,
    password_hash varchar(255) NOT NULL,
    full_name     varchar(200) NOT NULL,
    phone         varchar(20),
    avatar_url    varchar(500),
    locale        varchar(10) NOT NULL DEFAULT 'pt-BR',
    timezone      varchar(50) NOT NULL DEFAULT 'America/Sao_Paulo',
    mfa_enabled   boolean NOT NULL DEFAULT false,
    mfa_secret    varchar(255),
    last_login_at timestamptz,
    status        varchar(20) NOT NULL DEFAULT 'ACTIVE'
                  CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED')),
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz
);
CREATE UNIQUE INDEX ux_users_email ON users (lower(email)) WHERE deleted_at IS NULL;

CREATE TABLE roles (
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id   uuid REFERENCES tenants (id) ON DELETE CASCADE,
    code        varchar(50) NOT NULL,
    name        varchar(100) NOT NULL,
    description text,
    is_system   boolean NOT NULL DEFAULT false,
    created_at  timestamptz NOT NULL DEFAULT now(),
    updated_at  timestamptz NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX ux_roles_scope_code
    ON roles (COALESCE(tenant_id, '00000000-0000-0000-0000-000000000000'::uuid), code);
CREATE INDEX ix_roles_tenant ON roles (tenant_id);

CREATE TABLE permissions (
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    code        varchar(100) NOT NULL UNIQUE,
    resource    varchar(50) NOT NULL,
    action      varchar(20) NOT NULL,
    description text,
    created_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
    role_id       uuid NOT NULL REFERENCES roles (id) ON DELETE CASCADE,
    permission_id uuid NOT NULL REFERENCES permissions (id) ON DELETE CASCADE,
    PRIMARY KEY (role_id, permission_id)
);

CREATE TABLE user_tenants (
    id         uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id    uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tenant_id  uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    is_default boolean NOT NULL DEFAULT false,
    status     varchar(20) NOT NULL DEFAULT 'ACTIVE'
               CHECK (status IN ('ACTIVE', 'SUSPENDED', 'REVOKED')),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_user_tenants_user_tenant UNIQUE (user_id, tenant_id)
);
CREATE INDEX ix_user_tenants_user ON user_tenants (user_id);
CREATE INDEX ix_user_tenants_tenant ON user_tenants (tenant_id);

CREATE TABLE user_roles (
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_tenant_id uuid NOT NULL REFERENCES user_tenants (id) ON DELETE CASCADE,
    tenant_id      uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    role_id        uuid NOT NULL REFERENCES roles (id) ON DELETE RESTRICT,
    assigned_by    uuid REFERENCES users (id) ON DELETE SET NULL,
    assigned_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_user_roles_user_tenant_role UNIQUE (user_tenant_id, role_id)
);
CREATE INDEX ix_user_roles_tenant ON user_roles (tenant_id);
CREATE INDEX ix_user_roles_user_tenant ON user_roles (user_tenant_id);

-- A tenant-scoped role may only be assigned inside its own tenant.
CREATE OR REPLACE FUNCTION enforce_user_role_scope() RETURNS trigger
LANGUAGE plpgsql
AS $$
DECLARE
    role_tenant uuid;
BEGIN
    SELECT tenant_id INTO role_tenant FROM roles WHERE id = NEW.role_id;
    IF role_tenant IS NOT NULL AND role_tenant <> NEW.tenant_id THEN
        RAISE EXCEPTION 'role % does not belong to tenant %', NEW.role_id, NEW.tenant_id;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_user_roles_scope
    BEFORE INSERT OR UPDATE ON user_roles
    FOR EACH ROW EXECUTE FUNCTION enforce_user_role_scope();

CREATE TRIGGER trg_tenants_updated_at BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_roles_updated_at BEFORE UPDATE ON roles
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
CREATE TRIGGER trg_user_tenants_updated_at BEFORE UPDATE ON user_tenants
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();
