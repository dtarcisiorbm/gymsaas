-- V13: billing (Phase 6) — charges generated from memberships and manual
-- payments registered by operators. No payment gateway in this phase.
--
-- Amounts are immutable once a charge exists; only the status moves
-- (OPEN -> PAID | CANCELLED, PAID -> OPEN on refund). Payments are
-- append-only except for the CONFIRMED -> REFUNDED transition.
-- "Overdue" is derived (OPEN and due_date < tenant's today), never stored.

CREATE TABLE billing_number_counters (
    tenant_id  uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    kind       varchar(10) NOT NULL CHECK (kind IN ('CHARGE', 'PAYMENT')),
    year       integer NOT NULL,
    last_value integer NOT NULL DEFAULT 0 CHECK (last_value >= 0),
    PRIMARY KEY (tenant_id, kind, year)
);

-- ---------------------------------------------------------------------------
-- Charges.
-- ---------------------------------------------------------------------------
CREATE TABLE charges (
    id                  uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id           uuid NOT NULL,
    charge_number       varchar(20) NOT NULL,
    student_id          uuid NOT NULL,
    membership_id       uuid,
    branch_id           uuid NOT NULL,
    kind                varchar(20) NOT NULL CHECK (kind IN ('INSTALLMENT', 'SETUP_FEE', 'MANUAL')),
    installment_number  integer,
    installment_count   integer,
    description         varchar(300) NOT NULL,
    amount              numeric(12, 2) NOT NULL CHECK (amount > 0),
    currency            char(3) NOT NULL DEFAULT 'BRL',
    due_date            date NOT NULL,
    period_start        date,
    period_end          date,
    status              varchar(20) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'PAID', 'CANCELLED')),
    paid_at             timestamptz,
    cancelled_at        timestamptz,
    cancelled_by        uuid REFERENCES users (id),
    cancellation_reason varchar(1000),
    notes               varchar(1000),
    created_by          uuid REFERENCES users (id),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_charges_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ux_charges_number UNIQUE (tenant_id, charge_number),
    CONSTRAINT fk_charges_student FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id),
    CONSTRAINT fk_charges_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES student_memberships (tenant_id, id),
    CONSTRAINT fk_charges_branch FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT ck_charges_installment CHECK (
        (kind = 'INSTALLMENT' AND membership_id IS NOT NULL AND installment_number >= 1
            AND installment_count >= installment_number)
        OR (kind = 'SETUP_FEE' AND membership_id IS NOT NULL AND installment_number IS NULL
            AND installment_count IS NULL)
        OR (kind = 'MANUAL' AND installment_number IS NULL AND installment_count IS NULL)),
    CONSTRAINT ck_charges_period CHECK (period_start IS NULL OR period_end IS NULL OR period_end >= period_start),
    CONSTRAINT ck_charges_paid CHECK ((status = 'PAID') = (paid_at IS NOT NULL)),
    CONSTRAINT ck_charges_cancelled CHECK (
        (status = 'CANCELLED') = (cancelled_at IS NOT NULL AND cancellation_reason IS NOT NULL))
);
CREATE INDEX ix_charges_tenant_due ON charges (tenant_id, status, due_date);
CREATE INDEX ix_charges_tenant_student ON charges (tenant_id, student_id, due_date);
CREATE INDEX ix_charges_tenant_membership ON charges (tenant_id, membership_id, due_date);
-- One installment n / one setup fee per membership, even under concurrent generation.
CREATE UNIQUE INDEX ux_charges_membership_installment
    ON charges (tenant_id, membership_id, kind, COALESCE(installment_number, 0))
    WHERE kind IN ('INSTALLMENT', 'SETUP_FEE');

CREATE OR REPLACE FUNCTION prevent_charge_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'charges rows cannot be deleted';
    END IF;
    IF NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
       OR NEW.charge_number IS DISTINCT FROM OLD.charge_number
       OR NEW.student_id IS DISTINCT FROM OLD.student_id
       OR NEW.membership_id IS DISTINCT FROM OLD.membership_id
       OR NEW.branch_id IS DISTINCT FROM OLD.branch_id
       OR NEW.kind IS DISTINCT FROM OLD.kind
       OR NEW.installment_number IS DISTINCT FROM OLD.installment_number
       OR NEW.installment_count IS DISTINCT FROM OLD.installment_count
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.due_date IS DISTINCT FROM OLD.due_date
       OR NEW.period_start IS DISTINCT FROM OLD.period_start
       OR NEW.period_end IS DISTINCT FROM OLD.period_end
       OR NEW.created_by IS DISTINCT FROM OLD.created_by
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'charge values are immutable';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status
       AND NOT ((OLD.status = 'OPEN' AND NEW.status IN ('PAID', 'CANCELLED'))
                OR (OLD.status = 'PAID' AND NEW.status = 'OPEN')) THEN
        RAISE EXCEPTION 'invalid charge status transition % -> %', OLD.status, NEW.status;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_charges_immutable
    BEFORE UPDATE OR DELETE ON charges
    FOR EACH ROW EXECUTE FUNCTION prevent_charge_mutation();

-- ---------------------------------------------------------------------------
-- Payments (manual receipts).
-- ---------------------------------------------------------------------------
CREATE TABLE payments (
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id      uuid NOT NULL,
    payment_number varchar(20) NOT NULL,
    charge_id      uuid NOT NULL,
    student_id     uuid NOT NULL,
    branch_id      uuid NOT NULL,
    amount         numeric(12, 2) NOT NULL CHECK (amount > 0),
    currency       char(3) NOT NULL DEFAULT 'BRL',
    method         varchar(20) NOT NULL
        CHECK (method IN ('CASH', 'PIX', 'CREDIT_CARD', 'DEBIT_CARD', 'BANK_TRANSFER', 'OTHER')),
    paid_at        timestamptz NOT NULL,
    reference      varchar(200),
    notes          varchar(1000),
    status         varchar(20) NOT NULL DEFAULT 'CONFIRMED' CHECK (status IN ('CONFIRMED', 'REFUNDED')),
    received_by    uuid REFERENCES users (id),
    refunded_at    timestamptz,
    refunded_by    uuid REFERENCES users (id),
    refund_reason  varchar(1000),
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_payments_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ux_payments_number UNIQUE (tenant_id, payment_number),
    CONSTRAINT fk_payments_charge FOREIGN KEY (tenant_id, charge_id) REFERENCES charges (tenant_id, id),
    CONSTRAINT fk_payments_student FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id),
    CONSTRAINT fk_payments_branch FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT ck_payments_refund CHECK (
        (status = 'REFUNDED') = (refunded_at IS NOT NULL AND refund_reason IS NOT NULL))
);
CREATE INDEX ix_payments_tenant_charge ON payments (tenant_id, charge_id);
CREATE INDEX ix_payments_tenant_paid_at ON payments (tenant_id, status, paid_at);
-- A charge is settled by at most one confirmed payment.
CREATE UNIQUE INDEX ux_payments_confirmed_per_charge ON payments (tenant_id, charge_id) WHERE status = 'CONFIRMED';

CREATE OR REPLACE FUNCTION prevent_payment_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'payments rows cannot be deleted';
    END IF;
    IF OLD.status <> 'CONFIRMED' OR NEW.status <> 'REFUNDED'
       OR NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
       OR NEW.payment_number IS DISTINCT FROM OLD.payment_number
       OR NEW.charge_id IS DISTINCT FROM OLD.charge_id
       OR NEW.student_id IS DISTINCT FROM OLD.student_id
       OR NEW.branch_id IS DISTINCT FROM OLD.branch_id
       OR NEW.amount IS DISTINCT FROM OLD.amount
       OR NEW.currency IS DISTINCT FROM OLD.currency
       OR NEW.method IS DISTINCT FROM OLD.method
       OR NEW.paid_at IS DISTINCT FROM OLD.paid_at
       OR NEW.reference IS DISTINCT FROM OLD.reference
       OR NEW.notes IS DISTINCT FROM OLD.notes
       OR NEW.received_by IS DISTINCT FROM OLD.received_by
       OR NEW.created_at IS DISTINCT FROM OLD.created_at THEN
        RAISE EXCEPTION 'payments only allow CONFIRMED -> REFUNDED';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_payments_immutable
    BEFORE UPDATE OR DELETE ON payments
    FOR EACH ROW EXECUTE FUNCTION prevent_payment_mutation();

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE billing_number_counters ENABLE ROW LEVEL SECURITY;
CREATE POLICY billing_number_counters_isolation ON billing_number_counters
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE charges ENABLE ROW LEVEL SECURITY;
CREATE POLICY charges_isolation ON charges
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
CREATE POLICY payments_isolation ON payments
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

-- ---------------------------------------------------------------------------
-- Phase 6 permissions.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description) VALUES
    ('charges:read',      'charges',  'read',     'Visualizar cobranças'),
    ('charges:create',    'charges',  'create',   'Lançar cobranças avulsas'),
    ('charges:cancel',    'charges',  'cancel',   'Cancelar cobranças'),
    ('payments:register', 'payments', 'register', 'Registrar recebimentos'),
    ('payments:refund',   'payments', 'refund',   'Estornar recebimentos'),
    ('billing:reports',   'billing',  'reports',  'Ver resumo financeiro')
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
JOIN permissions p ON p.code IN ('charges:read', 'charges:create', 'charges:cancel', 'payments:register',
                                 'payments:refund', 'billing:reports')
WHERE r.code IN ('TENANT_ADMIN', 'BRANCH_MANAGER')
ON CONFLICT DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('charges:read', 'charges:create', 'payments:register')
WHERE r.code = 'RECEPTIONIST'
ON CONFLICT DO NOTHING;
