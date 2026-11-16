-- V10: plans, immutable price history and typed plan features (Phase 5).
--
-- Money is NUMERIC(12,2) with an ISO-4217 currency code. Contracted values are
-- snapshotted on student_memberships (V11); changing a plan price never touches
-- existing memberships.

-- Needed for the "no overlapping validity" exclusion constraints (uuid/date
-- equality inside a GiST index).
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- Business dates ("today", vigência, expiração) are computed in the tenant's
-- time zone, never in the JVM zone. Additive column; same default already
-- used by users.timezone (V2) and branch_settings.timezone (V6).
ALTER TABLE tenants ADD COLUMN timezone varchar(50) NOT NULL DEFAULT 'America/Sao_Paulo';

-- ---------------------------------------------------------------------------
-- Plans. duration_* is the CONTRACT length; billing frequency lives on
-- plan_prices and is intentionally independent.
-- ---------------------------------------------------------------------------
CREATE TABLE plans (
    id               uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id        uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    code             varchar(30)  NOT NULL,
    name             varchar(150) NOT NULL,
    description      text,
    duration_type    varchar(20)  NOT NULL
                     CHECK (duration_type IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL',
                                              'CUSTOM_MONTHS', 'CUSTOM_DAYS')),
    duration_value   integer      NOT NULL CHECK (duration_value BETWEEN 1 AND 3650),
    status           varchar(20)  NOT NULL DEFAULT 'ACTIVE'
                     CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    -- Freeze policy (NULL = no limit). max_freezes = 0 disables freezing.
    max_freeze_days  integer CHECK (max_freeze_days IS NULL OR max_freeze_days > 0),
    max_freezes      integer CHECK (max_freezes IS NULL OR max_freezes >= 0),
    min_freeze_days  integer CHECK (min_freeze_days IS NULL OR min_freeze_days > 0),
    -- Clauses authored by the academy, copied into generated contracts.
    contract_terms   text,
    version          integer NOT NULL DEFAULT 0,
    created_by       uuid REFERENCES users (id),
    created_at       timestamptz NOT NULL DEFAULT now(),
    updated_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_plans_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ux_plans_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_plans_fixed_duration
        CHECK (duration_type IN ('CUSTOM_MONTHS', 'CUSTOM_DAYS') OR duration_value = 1),
    CONSTRAINT ck_plans_custom_months CHECK (duration_type <> 'CUSTOM_MONTHS' OR duration_value <= 120),
    CONSTRAINT ck_plans_freeze_bounds
        CHECK (min_freeze_days IS NULL OR max_freeze_days IS NULL OR min_freeze_days <= max_freeze_days)
);
CREATE INDEX ix_plans_tenant_status ON plans (tenant_id, status);
CREATE INDEX ix_plans_tenant_name ON plans (tenant_id, lower(name));

CREATE TRIGGER trg_plans_updated_at BEFORE UPDATE ON plans
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Price history. Rows are immutable except for closing (valid_until) and
-- cancelling a price that never took effect. Active prices of a plan can never
-- overlap in time.
-- ---------------------------------------------------------------------------
CREATE TABLE plan_prices (
    id               uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id        uuid NOT NULL,
    plan_id          uuid NOT NULL,
    amount           numeric(12, 2) NOT NULL CHECK (amount >= 0),
    currency         char(3)     NOT NULL DEFAULT 'BRL' CHECK (currency ~ '^[A-Z]{3}$'),
    billing_period   varchar(20) NOT NULL
                     CHECK (billing_period IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL', 'ONE_TIME')),
    billing_interval integer     NOT NULL DEFAULT 1 CHECK (billing_interval BETWEEN 1 AND 36),
    setup_fee        numeric(12, 2) NOT NULL DEFAULT 0 CHECK (setup_fee >= 0),
    valid_from       date        NOT NULL,
    valid_until      date,
    status           varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'CANCELLED')),
    notes            text,
    created_by       uuid REFERENCES users (id),
    created_at       timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_plan_prices_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ux_plan_prices_tenant_plan_id UNIQUE (tenant_id, plan_id, id),
    CONSTRAINT fk_plan_prices_plan
        FOREIGN KEY (tenant_id, plan_id) REFERENCES plans (tenant_id, id),
    CONSTRAINT ck_plan_prices_validity CHECK (valid_until IS NULL OR valid_until >= valid_from),
    CONSTRAINT ck_plan_prices_one_time CHECK (billing_period <> 'ONE_TIME' OR billing_interval = 1),
    CONSTRAINT ex_plan_prices_no_overlap EXCLUDE USING gist (
        tenant_id WITH =,
        plan_id WITH =,
        daterange(valid_from, valid_until, '[]') WITH &&
    ) WHERE (status = 'ACTIVE')
);
CREATE INDEX ix_plan_prices_tenant_plan ON plan_prices (tenant_id, plan_id, valid_from DESC);

CREATE OR REPLACE FUNCTION prevent_plan_price_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'plan_prices rows cannot be deleted';
    END IF;
    IF NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
       OR NEW.plan_id IS DISTINCT FROM OLD.plan_id
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.billing_period IS DISTINCT FROM OLD.billing_period
       OR NEW.billing_interval IS DISTINCT FROM OLD.billing_interval
       OR NEW.setup_fee IS DISTINCT FROM OLD.setup_fee
       OR NEW.valid_from IS DISTINCT FROM OLD.valid_from
       OR NEW.created_at IS DISTINCT FROM OLD.created_at
       OR NEW.created_by IS DISTINCT FROM OLD.created_by THEN
        RAISE EXCEPTION 'plan_prices commercial fields are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_plan_prices_immutable
    BEFORE UPDATE OR DELETE ON plan_prices
    FOR EACH ROW EXECUTE FUNCTION prevent_plan_price_mutation();

-- ---------------------------------------------------------------------------
-- Typed plan features (benefits). Shape is enforced per type; the access
-- engine (later phase) consumes them, this phase only stores/snapshots them.
-- days_of_week is a bitmask: Monday = 1, Tuesday = 2, ..., Sunday = 64.
-- ---------------------------------------------------------------------------
CREATE TABLE plan_features (
    id            uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id     uuid NOT NULL,
    plan_id       uuid NOT NULL,
    feature_type  varchar(30) NOT NULL
                  CHECK (feature_type IN ('ALL_BRANCHES_ACCESS', 'BRANCH_ACCESS', 'TIME_WINDOW',
                                          'DAILY_CHECKIN_LIMIT', 'WEEKLY_CHECKIN_LIMIT',
                                          'GUEST_PASSES_PER_MONTH', 'CUSTOM')),
    branch_id     uuid,
    int_value     integer CHECK (int_value IS NULL OR int_value > 0),
    start_time    time,
    end_time      time,
    days_of_week  smallint CHECK (days_of_week IS NULL OR days_of_week BETWEEN 1 AND 127),
    label         varchar(150),
    description   text,
    sort_order    integer NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_plan_features_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_plan_features_plan
        FOREIGN KEY (tenant_id, plan_id) REFERENCES plans (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_features_branch
        FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT ck_plan_features_shape CHECK (
        CASE feature_type
            WHEN 'ALL_BRANCHES_ACCESS' THEN branch_id IS NULL AND int_value IS NULL
                                            AND start_time IS NULL AND end_time IS NULL
            WHEN 'BRANCH_ACCESS' THEN branch_id IS NOT NULL AND int_value IS NULL
            WHEN 'TIME_WINDOW' THEN start_time IS NOT NULL AND end_time IS NOT NULL
                                    AND start_time < end_time AND days_of_week IS NOT NULL
            WHEN 'DAILY_CHECKIN_LIMIT' THEN int_value IS NOT NULL
            WHEN 'WEEKLY_CHECKIN_LIMIT' THEN int_value IS NOT NULL
            WHEN 'GUEST_PASSES_PER_MONTH' THEN int_value IS NOT NULL
            WHEN 'CUSTOM' THEN label IS NOT NULL
        END)
);
CREATE INDEX ix_plan_features_tenant_plan ON plan_features (tenant_id, plan_id, sort_order);
CREATE UNIQUE INDEX ux_plan_features_branch
    ON plan_features (tenant_id, plan_id, branch_id) WHERE feature_type = 'BRANCH_ACCESS';
CREATE UNIQUE INDEX ux_plan_features_singleton
    ON plan_features (tenant_id, plan_id, feature_type)
    WHERE feature_type IN ('ALL_BRANCHES_ACCESS', 'DAILY_CHECKIN_LIMIT', 'WEEKLY_CHECKIN_LIMIT',
                           'GUEST_PASSES_PER_MONTH');

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE plans ENABLE ROW LEVEL SECURITY;
CREATE POLICY plans_isolation ON plans
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE plan_prices ENABLE ROW LEVEL SECURITY;
CREATE POLICY plan_prices_isolation ON plan_prices
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE plan_features ENABLE ROW LEVEL SECURITY;
CREATE POLICY plan_features_isolation ON plan_features
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

-- ---------------------------------------------------------------------------
-- Phase 5 plan permissions. Legacy plan:read / plan:manage are preserved and
-- still accepted by the API.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description) VALUES
    ('plans:read',          'plans', 'read',          'Visualizar planos e preços'),
    ('plans:create',        'plans', 'create',        'Cadastrar planos'),
    ('plans:update',        'plans', 'update',        'Editar planos, benefícios e status'),
    ('plans:manage_prices', 'plans', 'manage_prices', 'Configurar preços de planos')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SAAS_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('plans:read', 'plans:create', 'plans:update', 'plans:manage_prices')
WHERE r.code = 'TENANT_ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'plans:read'
WHERE r.code IN ('BRANCH_MANAGER', 'RECEPTIONIST', 'TRAINER')
ON CONFLICT DO NOTHING;
