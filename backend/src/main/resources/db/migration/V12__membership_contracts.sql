-- V12: membership contracts and acceptance evidence (Phase 5).
--
-- The rendered contract text and its SHA-256 are stored in the database; the
-- PDF lives in external storage (storage_key) and only its hash/size/type are
-- stored here. An acceptance is a SIMPLE electronic acceptance record, not a
-- qualified digital signature (ICP-Brasil).

CREATE TABLE membership_contracts (
    id                    uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id             uuid NOT NULL,
    membership_id         uuid NOT NULL,
    contract_version      integer NOT NULL CHECK (contract_version >= 1),
    template_version      varchar(20) NOT NULL,
    status                varchar(20) NOT NULL CHECK (status IN ('GENERATED', 'ACCEPTED', 'SUPERSEDED')),
    content               text NOT NULL,
    content_sha256        char(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    storage_key           varchar(300) NOT NULL,
    document_sha256       char(64) NOT NULL CHECK (document_sha256 ~ '^[0-9a-f]{64}$'),
    document_size_bytes   bigint NOT NULL CHECK (document_size_bytes > 0),
    document_content_type varchar(100) NOT NULL,
    generated_by          uuid REFERENCES users (id),
    generated_at          timestamptz NOT NULL DEFAULT now(),
    status_changed_at     timestamptz,
    CONSTRAINT ux_membership_contracts_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ux_membership_contracts_version UNIQUE (tenant_id, membership_id, contract_version),
    CONSTRAINT ux_membership_contracts_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_membership_contracts_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES student_memberships (tenant_id, id)
);
CREATE INDEX ix_membership_contracts_tenant_membership
    ON membership_contracts (tenant_id, membership_id, contract_version DESC);
-- Only one current (generated or accepted) contract per membership.
CREATE UNIQUE INDEX ux_membership_contracts_current
    ON membership_contracts (tenant_id, membership_id) WHERE status IN ('GENERATED', 'ACCEPTED');

CREATE OR REPLACE FUNCTION prevent_contract_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'membership_contracts rows cannot be deleted';
    END IF;
    IF NEW.tenant_id IS DISTINCT FROM OLD.tenant_id
       OR NEW.membership_id IS DISTINCT FROM OLD.membership_id
       OR NEW.contract_version IS DISTINCT FROM OLD.contract_version
       OR NEW.template_version IS DISTINCT FROM OLD.template_version
       OR NEW.content IS DISTINCT FROM OLD.content
       OR NEW.content_sha256 IS DISTINCT FROM OLD.content_sha256
       OR NEW.storage_key IS DISTINCT FROM OLD.storage_key
       OR NEW.document_sha256 IS DISTINCT FROM OLD.document_sha256
       OR NEW.document_size_bytes IS DISTINCT FROM OLD.document_size_bytes
       OR NEW.document_content_type IS DISTINCT FROM OLD.document_content_type
       OR NEW.generated_by IS DISTINCT FROM OLD.generated_by
       OR NEW.generated_at IS DISTINCT FROM OLD.generated_at THEN
        RAISE EXCEPTION 'membership contract content is immutable';
    END IF;
    IF NEW.status IS DISTINCT FROM OLD.status
       AND NOT (OLD.status = 'GENERATED' AND NEW.status IN ('ACCEPTED', 'SUPERSEDED')) THEN
        RAISE EXCEPTION 'invalid contract status transition % -> %', OLD.status, NEW.status;
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_membership_contracts_immutable
    BEFORE UPDATE OR DELETE ON membership_contracts
    FOR EACH ROW EXECUTE FUNCTION prevent_contract_mutation();

-- ---------------------------------------------------------------------------
-- Acceptance evidence (append-only, one per contract).
-- ---------------------------------------------------------------------------
CREATE TABLE contract_acceptances (
    id                       uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id                uuid NOT NULL,
    contract_id              uuid NOT NULL,
    membership_id            uuid NOT NULL,
    student_id               uuid NOT NULL,
    accepted_at              timestamptz NOT NULL,
    channel                  varchar(20) NOT NULL CHECK (channel IN ('IN_PERSON', 'ELECTRONIC')),
    acceptor_name            varchar(200) NOT NULL,
    acceptor_document_masked varchar(20),
    content_sha256           char(64) NOT NULL CHECK (content_sha256 ~ '^[0-9a-f]{64}$'),
    declaration              text NOT NULL,
    ip_address               varchar(45),
    user_agent               varchar(500),
    registered_by            uuid REFERENCES users (id),
    created_at               timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_contract_acceptances_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ux_contract_acceptances_contract UNIQUE (tenant_id, contract_id),
    CONSTRAINT fk_contract_acceptances_contract
        FOREIGN KEY (tenant_id, contract_id) REFERENCES membership_contracts (tenant_id, id),
    CONSTRAINT fk_contract_acceptances_membership
        FOREIGN KEY (tenant_id, membership_id) REFERENCES student_memberships (tenant_id, id),
    CONSTRAINT fk_contract_acceptances_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id)
);
CREATE INDEX ix_contract_acceptances_tenant_membership ON contract_acceptances (tenant_id, membership_id);

CREATE TRIGGER trg_contract_acceptances_append_only
    BEFORE UPDATE OR DELETE ON contract_acceptances
    FOR EACH ROW EXECUTE FUNCTION prevent_append_only_mutation();

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE membership_contracts ENABLE ROW LEVEL SECURITY;
CREATE POLICY membership_contracts_isolation ON membership_contracts
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE contract_acceptances ENABLE ROW LEVEL SECURITY;
CREATE POLICY contract_acceptances_isolation ON contract_acceptances
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

-- ---------------------------------------------------------------------------
-- Phase 5 contract permissions.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description) VALUES
    ('contracts:read',                'contracts', 'read',                'Visualizar contratos de matrícula'),
    ('contracts:generate',            'contracts', 'generate',            'Gerar contratos de matrícula'),
    ('contracts:register_acceptance', 'contracts', 'register_acceptance', 'Registrar aceite de contrato')
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
JOIN permissions p ON p.code IN ('contracts:read', 'contracts:generate', 'contracts:register_acceptance')
WHERE r.code IN ('TENANT_ADMIN', 'BRANCH_MANAGER', 'RECEPTIONIST')
ON CONFLICT DO NOTHING;
