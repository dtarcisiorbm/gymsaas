-- V3: refresh token rotation/session revocation and audit trail.

CREATE TABLE refresh_tokens (
    id             uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    user_id        uuid NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    tenant_id      uuid REFERENCES tenants (id) ON DELETE CASCADE,
    family_id      uuid NOT NULL,
    token_hash     varchar(64) NOT NULL,
    issued_at      timestamptz NOT NULL DEFAULT now(),
    expires_at     timestamptz NOT NULL,
    revoked_at     timestamptz,
    revoke_reason  varchar(30)
                   CHECK (revoke_reason IN ('LOGOUT', 'ROTATED', 'REUSE_DETECTED', 'ADMIN', 'EXPIRED')),
    replaced_by_id uuid REFERENCES refresh_tokens (id) ON DELETE SET NULL,
    ip_address     inet,
    user_agent     varchar(500),
    created_at     timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_refresh_tokens_hash UNIQUE (token_hash)
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);
CREATE INDEX ix_refresh_tokens_expires ON refresh_tokens (expires_at);

CREATE TABLE audit_logs (
    id            uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id     uuid REFERENCES tenants (id) ON DELETE SET NULL,
    actor_type    varchar(20) NOT NULL
                  CHECK (actor_type IN ('USER', 'SYSTEM', 'GATEWAY', 'API_CLIENT')),
    actor_id      uuid,
    action        varchar(100) NOT NULL,
    resource_type varchar(50) NOT NULL,
    resource_id   uuid,
    old_values    jsonb,
    new_values    jsonb,
    metadata      jsonb NOT NULL DEFAULT '{}'::jsonb,
    risk_level    varchar(10) NOT NULL DEFAULT 'LOW'
                  CHECK (risk_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    occurred_at   timestamptz NOT NULL DEFAULT now(),
    created_at    timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_logs_tenant_occurred ON audit_logs (tenant_id, occurred_at DESC);
CREATE INDEX ix_audit_logs_actor ON audit_logs (actor_id, occurred_at DESC);
CREATE INDEX ix_audit_logs_resource ON audit_logs (resource_type, resource_id);
CREATE INDEX ix_audit_logs_action ON audit_logs (action, occurred_at DESC);

-- Audit trail is append-only (LGPD Art. 37).
CREATE OR REPLACE FUNCTION prevent_audit_log_mutation() RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_logs is append-only';
END;
$$;

CREATE TRIGGER trg_audit_logs_no_update
    BEFORE UPDATE OR DELETE ON audit_logs
    FOR EACH ROW EXECUTE FUNCTION prevent_audit_log_mutation();
