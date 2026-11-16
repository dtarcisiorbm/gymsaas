-- V11: student memberships, freezes and lifecycle history (Phase 5).
--
-- A membership snapshots every commercial condition at contracting time
-- (price, billing, discount, duration, freeze policy). Those columns are
-- immutable at the database level: a later plan/price change can never alter
-- what was contracted.

-- Atomic per-tenant/per-year membership number counter (same pattern as V8).
CREATE TABLE membership_number_counters (
    tenant_id  uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    year       integer NOT NULL,
    last_value integer NOT NULL DEFAULT 0 CHECK (last_value >= 0),
    PRIMARY KEY (tenant_id, year)
);

-- ---------------------------------------------------------------------------
-- Memberships.
--   end_date          current end, extended by freezes
--   original_end_date calendar end computed at contracting time
--   anchor_date/term_sequence keep renewals on the original calendar so dates
--   never drift: end = anchor + months * term_sequence - 1 day.
-- ---------------------------------------------------------------------------
CREATE TABLE student_memberships (
    id                  uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id           uuid NOT NULL,
    student_id          uuid NOT NULL,
    plan_id             uuid NOT NULL,
    plan_price_id       uuid NOT NULL,
    branch_id           uuid NOT NULL,
    registration_number varchar(20) NOT NULL,
    status              varchar(20) NOT NULL
                        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'FROZEN', 'EXPIRED', 'CANCELLED')),
    start_date          date NOT NULL,
    end_date            date NOT NULL,
    original_end_date   date NOT NULL,
    anchor_date         date NOT NULL,
    term_sequence       integer NOT NULL DEFAULT 1 CHECK (term_sequence >= 1),
    duration_type       varchar(20) NOT NULL
                        CHECK (duration_type IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL',
                                                 'CUSTOM_MONTHS', 'CUSTOM_DAYS')),
    duration_value      integer NOT NULL CHECK (duration_value BETWEEN 1 AND 3650),
    amount              numeric(12, 2) NOT NULL CHECK (amount >= 0),
    setup_fee           numeric(12, 2) NOT NULL DEFAULT 0 CHECK (setup_fee >= 0),
    currency            char(3) NOT NULL CHECK (currency ~ '^[A-Z]{3}$'),
    billing_period      varchar(20) NOT NULL
                        CHECK (billing_period IN ('MONTHLY', 'QUARTERLY', 'SEMIANNUAL', 'ANNUAL', 'ONE_TIME')),
    billing_interval    integer NOT NULL CHECK (billing_interval BETWEEN 1 AND 36),
    discount_amount     numeric(12, 2) NOT NULL DEFAULT 0,
    discount_reason     text,
    final_amount        numeric(12, 2) NOT NULL,
    max_freeze_days     integer CHECK (max_freeze_days IS NULL OR max_freeze_days > 0),
    max_freezes         integer CHECK (max_freezes IS NULL OR max_freezes >= 0),
    min_freeze_days     integer CHECK (min_freeze_days IS NULL OR min_freeze_days > 0),
    conditions_snapshot jsonb NOT NULL,
    renewed_from_id     uuid,
    notes               text,
    cancelled_at        timestamptz,
    cancelled_by        uuid REFERENCES users (id),
    cancellation_reason text,
    created_by          uuid REFERENCES users (id),
    version             integer NOT NULL DEFAULT 0,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_student_memberships_tenant_number UNIQUE (tenant_id, registration_number),
    CONSTRAINT ux_student_memberships_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_memberships_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id),
    CONSTRAINT fk_student_memberships_plan
        FOREIGN KEY (tenant_id, plan_id) REFERENCES plans (tenant_id, id),
    -- The price must belong to the same plan AND the same tenant.
    CONSTRAINT fk_student_memberships_price
        FOREIGN KEY (tenant_id, plan_id, plan_price_id) REFERENCES plan_prices (tenant_id, plan_id, id),
    CONSTRAINT fk_student_memberships_branch
        FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT fk_student_memberships_renewed_from
        FOREIGN KEY (tenant_id, renewed_from_id) REFERENCES student_memberships (tenant_id, id),
    CONSTRAINT ck_student_memberships_dates
        CHECK (original_end_date >= start_date AND end_date >= original_end_date AND anchor_date <= start_date),
    CONSTRAINT ck_student_memberships_discount
        CHECK (discount_amount >= 0 AND discount_amount <= amount AND final_amount = amount - discount_amount),
    CONSTRAINT ck_student_memberships_discount_reason
        CHECK (discount_amount = 0 OR discount_reason IS NOT NULL),
    CONSTRAINT ck_student_memberships_cancellation
        CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)
               AND (cancelled_at IS NULL OR cancellation_reason IS NOT NULL)),
    -- A student cannot hold two open memberships of the same plan for
    -- overlapping periods. Different plans may coexist.
    CONSTRAINT ex_student_memberships_no_overlap EXCLUDE USING gist (
        tenant_id WITH =,
        student_id WITH =,
        plan_id WITH =,
        daterange(start_date, end_date, '[]') WITH &&
    ) WHERE (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'FROZEN'))
);
CREATE INDEX ix_student_memberships_tenant_student ON student_memberships (tenant_id, student_id);
CREATE INDEX ix_student_memberships_tenant_status_end ON student_memberships (tenant_id, status, end_date);
CREATE INDEX ix_student_memberships_tenant_plan ON student_memberships (tenant_id, plan_id);
CREATE INDEX ix_student_memberships_tenant_branch ON student_memberships (tenant_id, branch_id);
CREATE INDEX ix_student_memberships_tenant_created ON student_memberships (tenant_id, created_at DESC);
-- At most one live renewal per membership.
CREATE UNIQUE INDEX ux_student_memberships_renewal
    ON student_memberships (tenant_id, renewed_from_id)
    WHERE renewed_from_id IS NOT NULL AND status <> 'CANCELLED';

CREATE TRIGGER trg_student_memberships_updated_at BEFORE UPDATE ON student_memberships
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE OR REPLACE FUNCTION prevent_membership_contract_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'student_memberships rows cannot be deleted';
    END IF;
    IF NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
       OR NEW.student_id IS DISTINCT FROM OLD.student_id
       OR NEW.plan_id IS DISTINCT FROM OLD.plan_id
       OR NEW.plan_price_id IS DISTINCT FROM OLD.plan_price_id
       OR NEW.registration_number IS DISTINCT FROM OLD.registration_number
       OR NEW.start_date IS DISTINCT FROM OLD.start_date
       OR NEW.original_end_date IS DISTINCT FROM OLD.original_end_date
       OR NEW.anchor_date IS DISTINCT FROM OLD.anchor_date
       OR NEW.term_sequence IS DISTINCT FROM OLD.term_sequence
       OR NEW.duration_type IS DISTINCT FROM OLD.duration_type
       OR NEW.duration_value IS DISTINCT FROM OLD.duration_value
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.setup_fee IS DISTINCT FROM OLD.setup_fee
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.billing_period IS DISTINCT FROM OLD.billing_period
       OR NEW.billing_interval IS DISTINCT FROM OLD.billing_interval
       OR NEW.discount_amount IS DISTINCT FROM OLD.discount_amount
       OR NEW.discount_reason IS DISTINCT FROM OLD.discount_reason
       OR NEW.final_amount IS DISTINCT FROM OLD.final_amount
       OR NEW.max_freeze_days IS DISTINCT FROM OLD.max_freeze_days
       OR NEW.max_freezes IS DISTINCT FROM OLD.max_freezes
       OR NEW.min_freeze_days IS DISTINCT FROM OLD.min_freeze_days
       OR NEW.conditions_snapshot IS DISTINCT FROM OLD.conditions_snapshot
       OR NEW.renewed_from_id IS DISTINCT FROM OLD.renewed_from_id
       OR NEW.created_by IS DISTINCT FROM OLD.created_by
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'contracted membership conditions are immutable';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_student_memberships_immutable
    BEFORE UPDATE OR DELETE ON student_memberships
    FOR EACH ROW EXECUTE FUNCTION prevent_membership_contract_mutation();

-- ---------------------------------------------------------------------------
-- Freezes. Period is inclusive [start_date, planned_end_date]. An early
-- unfreeze records actual_end_date/effective_days; the membership end_date is
-- extended by planned_days and reduced by the unused days.
-- ---------------------------------------------------------------------------
CREATE TABLE membership_freezes (
    id               uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id        uuid NOT NULL,
    membership_id    uuid NOT NULL,
    start_date       date NOT NULL,
    planned_end_date date NOT NULL,
    actual_end_date  date,
    planned_days     integer NOT NULL CHECK (planned_days > 0),
    effective_days   integer,
    status           varchar(20) NOT NULL CHECK (status IN ('ACTIVE', 'COMPLETED', 'CANCELLED')),
    reason           text NOT NULL,
    created_by       uuid REFERENCES users (id),
    ended_by         uuid REFERENCES users (id),
    created_at       timestamptz NOT NULL DEFAULT now(),
    ended_at         timestamptz,
    CONSTRAINT ux_membership_freezes_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_membership_freezes_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES student_memberships (tenant_id, id),
    CONSTRAINT ck_membership_freezes_period
        CHECK (planned_end_date >= start_date AND planned_days = planned_end_date - start_date + 1),
    CONSTRAINT ck_membership_freezes_effective
        CHECK (effective_days IS NULL OR effective_days BETWEEN 0 AND planned_days),
    CONSTRAINT ck_membership_freezes_actual_end
        CHECK (actual_end_date IS NULL OR actual_end_date BETWEEN start_date AND planned_end_date),
    CONSTRAINT ck_membership_freezes_closed
        CHECK ((status = 'ACTIVE') = (ended_at IS NULL)
               AND (status = 'ACTIVE' OR effective_days IS NOT NULL)),
    CONSTRAINT ex_membership_freezes_no_overlap EXCLUDE USING gist (
        tenant_id WITH =,
        membership_id WITH =,
        daterange(start_date, COALESCE(actual_end_date, planned_end_date), '[]') WITH &&
    ) WHERE (status <> 'CANCELLED')
);
CREATE INDEX ix_membership_freezes_tenant_membership ON membership_freezes (tenant_id, membership_id);
CREATE UNIQUE INDEX ux_membership_freezes_active
    ON membership_freezes (tenant_id, membership_id) WHERE status = 'ACTIVE';
CREATE INDEX ix_membership_freezes_active_end
    ON membership_freezes (tenant_id, planned_end_date) WHERE status = 'ACTIVE';

-- ---------------------------------------------------------------------------
-- Lifecycle history (append-only): status changes, renewals, freezes,
-- contract events. Unified source for "histórico de matrículas".
-- ---------------------------------------------------------------------------
CREATE TABLE membership_status_history (
    id            uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id     uuid NOT NULL,
    membership_id uuid NOT NULL,
    event_type    varchar(30) NOT NULL
                  CHECK (event_type IN ('CREATED', 'ACTIVATED', 'UPDATED', 'RENEWED', 'CANCELLED',
                                        'FROZEN', 'UNFROZEN', 'SUSPENDED', 'REACTIVATED', 'EXPIRED',
                                        'CONTRACT_GENERATED', 'CONTRACT_ACCEPTED')),
    from_status   varchar(20),
    to_status     varchar(20),
    reason        text,
    metadata      jsonb NOT NULL DEFAULT '{}'::jsonb,
    changed_by    uuid REFERENCES users (id),
    changed_at    timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_membership_status_history_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_membership_status_history_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES student_memberships (tenant_id, id)
);
CREATE INDEX ix_membership_status_history_tenant_membership
    ON membership_status_history (tenant_id, membership_id, changed_at DESC);

CREATE OR REPLACE FUNCTION prevent_append_only_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION '% is append-only', TG_TABLE_NAME;
END;
$$;

CREATE TRIGGER trg_membership_status_history_append_only
    BEFORE UPDATE OR DELETE ON membership_status_history
    FOR EACH ROW EXECUTE FUNCTION prevent_append_only_mutation();

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE membership_number_counters ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_number_counters_isolation ON membership_number_counters
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_memberships ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_memberships_isolation ON student_memberships
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE membership_freezes ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_freezes_isolation ON membership_freezes
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE membership_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_status_history_isolation ON membership_status_history
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

-- ---------------------------------------------------------------------------
-- Phase 5 membership permissions.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description) VALUES
    ('memberships:read',           'memberships', 'read',           'Visualizar matrículas'),
    ('memberships:create',         'memberships', 'create',         'Cadastrar matrículas'),
    ('memberships:update',         'memberships', 'update',         'Editar dados operacionais de matrículas'),
    ('memberships:renew',          'memberships', 'renew',          'Renovar matrículas'),
    ('memberships:cancel',         'memberships', 'cancel',         'Cancelar matrículas'),
    ('memberships:freeze',         'memberships', 'freeze',         'Congelar e descongelar matrículas'),
    ('memberships:suspend',        'memberships', 'suspend',        'Suspender e reativar matrículas'),
    ('memberships:apply_discount', 'memberships', 'apply_discount', 'Conceder desconto na contratação')
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
JOIN permissions p ON p.code IN (
    'memberships:read', 'memberships:create', 'memberships:update', 'memberships:renew',
    'memberships:cancel', 'memberships:freeze', 'memberships:suspend', 'memberships:apply_discount'
)
WHERE r.code IN ('TENANT_ADMIN', 'BRANCH_MANAGER')
ON CONFLICT DO NOTHING;

-- RECEPTIONIST: front-desk operation; no cancellation, suspension or discount.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'memberships:read', 'memberships:create', 'memberships:update', 'memberships:renew',
    'memberships:freeze'
)
WHERE r.code = 'RECEPTIONIST'
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'memberships:read'
WHERE r.code = 'TRAINER'
ON CONFLICT DO NOTHING;
