-- V9: student branch assignments, status history, notes and phase 4 permissions.

-- ---------------------------------------------------------------------------
-- Branch assignments. Rows are closed with ended_at, never deleted, so the
-- transfer history survives. Exactly one active primary assignment per student.
-- ---------------------------------------------------------------------------
CREATE TABLE student_branch_assignments (
    id              uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id       uuid NOT NULL,
    student_id      uuid NOT NULL,
    branch_id       uuid NOT NULL,
    is_primary      boolean NOT NULL DEFAULT false,
    started_at      timestamptz NOT NULL DEFAULT now(),
    ended_at        timestamptz,
    transfer_reason text,
    assigned_by     uuid REFERENCES users (id),
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_student_branch_assignments_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_branch_assignments_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_student_branch_assignments_branch
        FOREIGN KEY (tenant_id, branch_id) REFERENCES branches (tenant_id, id),
    CONSTRAINT ck_student_branch_assignments_period
        CHECK (ended_at IS NULL OR ended_at >= started_at)
);
CREATE UNIQUE INDEX ux_student_primary_branch
    ON student_branch_assignments (tenant_id, student_id)
    WHERE ended_at IS NULL AND is_primary;
CREATE UNIQUE INDEX ux_student_branch_active
    ON student_branch_assignments (tenant_id, student_id, branch_id)
    WHERE ended_at IS NULL;
CREATE INDEX ix_student_branch_assignments_tenant_student
    ON student_branch_assignments (tenant_id, student_id);

CREATE TRIGGER trg_student_branch_assignments_updated_at BEFORE UPDATE ON student_branch_assignments
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Status history (append-only by application convention).
-- ---------------------------------------------------------------------------
CREATE TABLE student_status_history (
    id          uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id   uuid NOT NULL,
    student_id  uuid NOT NULL,
    from_status varchar(20)
                CHECK (from_status IS NULL OR from_status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'BLOCKED', 'ARCHIVED')),
    to_status   varchar(20) NOT NULL
                CHECK (to_status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'BLOCKED', 'ARCHIVED')),
    reason      text,
    changed_by  uuid REFERENCES users (id),
    changed_at  timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_student_status_history_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_status_history_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX ix_student_status_history_tenant_student
    ON student_status_history (tenant_id, student_id, changed_at DESC);

-- ---------------------------------------------------------------------------
-- Administrative notes.
-- ---------------------------------------------------------------------------
CREATE TABLE student_notes (
    id         uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id  uuid NOT NULL,
    student_id uuid NOT NULL,
    content    text NOT NULL,
    created_by uuid REFERENCES users (id),
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    deleted_at timestamptz,
    CONSTRAINT ux_student_notes_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_notes_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX ix_student_notes_tenant_student ON student_notes (tenant_id, student_id)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_student_notes_updated_at BEFORE UPDATE ON student_notes
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE student_branch_assignments ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_branch_assignments_isolation ON student_branch_assignments
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_status_history ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_status_history_isolation ON student_status_history
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_notes ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_notes_isolation ON student_notes
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

-- ---------------------------------------------------------------------------
-- Phase 4 permissions. Legacy singular student:* permissions are preserved.
-- ---------------------------------------------------------------------------
INSERT INTO permissions (code, resource, action, description) VALUES
    ('students:create',           'students', 'create',           'Cadastrar alunos'),
    ('students:read',             'students', 'read',             'Visualizar alunos'),
    ('students:update',           'students', 'update',           'Editar alunos'),
    ('students:delete',           'students', 'delete',           'Excluir alunos (soft delete)'),
    ('students:change_status',    'students', 'change_status',    'Alterar status cadastral de alunos'),
    ('students:manage_documents', 'students', 'manage_documents', 'Gerenciar documentos de alunos'),
    ('students:manage_contacts',  'students', 'manage_contacts',  'Gerenciar contatos de alunos'),
    ('students:manage_branches',  'students', 'manage_branches',  'Gerenciar vínculos e transferências de alunos'),
    ('students:manage_notes',     'students', 'manage_notes',     'Gerenciar observações administrativas'),
    ('students:read_sensitive',   'students', 'read_sensitive',   'Visualizar documentos completos de alunos')
ON CONFLICT (code) DO NOTHING;

-- SAAS_ADMIN keeps global administration of every tenant.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.code = 'SAAS_ADMIN'
ON CONFLICT DO NOTHING;

-- TENANT_ADMIN: full student management, including sensitive documents.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'students:create', 'students:read', 'students:update', 'students:delete',
    'students:change_status', 'students:manage_documents', 'students:manage_contacts',
    'students:manage_branches', 'students:manage_notes', 'students:read_sensitive'
)
WHERE r.code = 'TENANT_ADMIN'
ON CONFLICT DO NOTHING;

-- BRANCH_MANAGER: everything except deleting students.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'students:create', 'students:read', 'students:update',
    'students:change_status', 'students:manage_documents', 'students:manage_contacts',
    'students:manage_branches', 'students:manage_notes', 'students:read_sensitive'
)
WHERE r.code = 'BRANCH_MANAGER'
ON CONFLICT DO NOTHING;

-- RECEPTIONIST: front-desk operation, no full document reveal and no delete.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN (
    'students:create', 'students:read', 'students:update', 'students:change_status',
    'students:manage_documents', 'students:manage_contacts', 'students:manage_branches',
    'students:manage_notes'
)
WHERE r.code = 'RECEPTIONIST'
ON CONFLICT DO NOTHING;

-- TRAINER: read-only.
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code = 'students:read'
WHERE r.code = 'TRAINER'
ON CONFLICT DO NOTHING;
