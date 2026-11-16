-- V8: students core, documents, contacts, addresses and registration counter.

-- ---------------------------------------------------------------------------
-- Students. branch_id intentionally lives in student_branch_assignments so a
-- student can be linked to several units while keeping one primary unit.
-- ---------------------------------------------------------------------------
CREATE TABLE students (
    id                  uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id           uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    registration_number varchar(20) NOT NULL,
    name                varchar(200) NOT NULL,
    social_name         varchar(200),
    birth_date          date,
    email               varchar(150),
    phone               varchar(20),
    gender              varchar(20)
                        CHECK (gender IS NULL OR gender IN ('MALE', 'FEMALE', 'OTHER', 'NOT_INFORMED')),
    photo_url           varchar(500),
    status              varchar(20) NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'ACTIVE', 'INACTIVE', 'BLOCKED', 'ARCHIVED')),
    notes               text,
    version             integer NOT NULL DEFAULT 0,
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now(),
    deleted_at          timestamptz,
    CONSTRAINT ux_students_tenant_registration UNIQUE (tenant_id, registration_number),
    CONSTRAINT ux_students_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT ck_students_birth_date CHECK (birth_date IS NULL OR birth_date <= CURRENT_DATE)
);
CREATE INDEX ix_students_tenant_status ON students (tenant_id, status) WHERE deleted_at IS NULL;
CREATE INDEX ix_students_tenant_name ON students (tenant_id, lower(name)) WHERE deleted_at IS NULL;
CREATE INDEX ix_students_tenant_created ON students (tenant_id, created_at DESC) WHERE deleted_at IS NULL;

CREATE TRIGGER trg_students_updated_at BEFORE UPDATE ON students
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- Atomic per-tenant/per-year registration counter. UPDATE ... RETURNING avoids
-- gaps and collisions without table-level locks.
CREATE TABLE student_registration_counters (
    tenant_id  uuid NOT NULL REFERENCES tenants (id) ON DELETE CASCADE,
    year       integer NOT NULL,
    last_value integer NOT NULL DEFAULT 0 CHECK (last_value >= 0),
    PRIMARY KEY (tenant_id, year)
);

-- ---------------------------------------------------------------------------
-- Documents. CPF is stored normalized (digits only); unique per tenant among
-- non-deleted rows. Other document types are not globally unique.
-- ---------------------------------------------------------------------------
CREATE TABLE student_documents (
    id                uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id         uuid NOT NULL,
    student_id        uuid NOT NULL,
    document_type     varchar(20) NOT NULL
                      CHECK (document_type IN ('CPF', 'RG', 'CNH', 'PASSPORT', 'OTHER')),
    document_number   varchar(100) NOT NULL,
    normalized_number varchar(60) NOT NULL,
    issuer            varchar(100),
    state             varchar(2),
    issued_at         date,
    expires_at        date,
    notes             text,
    is_primary        boolean NOT NULL DEFAULT false,
    created_at        timestamptz NOT NULL DEFAULT now(),
    updated_at        timestamptz NOT NULL DEFAULT now(),
    deleted_at        timestamptz,
    CONSTRAINT ux_student_documents_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_documents_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_student_documents_cpf
        CHECK (document_type <> 'CPF' OR normalized_number ~ '^[0-9]{11}$')
);
CREATE INDEX ix_student_documents_tenant_student ON student_documents (tenant_id, student_id)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_student_documents_tenant_cpf
    ON student_documents (tenant_id, normalized_number)
    WHERE document_type = 'CPF' AND deleted_at IS NULL;
CREATE UNIQUE INDEX ux_student_documents_primary
    ON student_documents (tenant_id, student_id, document_type)
    WHERE is_primary AND deleted_at IS NULL;

CREATE TRIGGER trg_student_documents_updated_at BEFORE UPDATE ON student_documents
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Contacts.
-- ---------------------------------------------------------------------------
CREATE TABLE student_contacts (
    id            uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id     uuid NOT NULL,
    student_id    uuid NOT NULL,
    name          varchar(150) NOT NULL,
    relationship  varchar(50),
    phone         varchar(20) NOT NULL,
    email         varchar(150),
    is_primary    boolean NOT NULL DEFAULT false,
    notes         text,
    created_at    timestamptz NOT NULL DEFAULT now(),
    updated_at    timestamptz NOT NULL DEFAULT now(),
    deleted_at    timestamptz,
    CONSTRAINT ux_student_contacts_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_contacts_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE
);
CREATE INDEX ix_student_contacts_tenant_student ON student_contacts (tenant_id, student_id)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_student_contacts_primary
    ON student_contacts (tenant_id, student_id)
    WHERE is_primary AND deleted_at IS NULL;

CREATE TRIGGER trg_student_contacts_updated_at BEFORE UPDATE ON student_contacts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- Address (1:1 with student).
-- ---------------------------------------------------------------------------
CREATE TABLE student_addresses (
    id           uuid PRIMARY KEY DEFAULT uuid_generate_v7(),
    tenant_id    uuid NOT NULL,
    student_id   uuid NOT NULL,
    zip_code     varchar(8),
    street       varchar(200),
    number       varchar(20),
    complement   varchar(100),
    neighborhood varchar(100),
    city         varchar(100),
    state        varchar(2),
    created_at   timestamptz NOT NULL DEFAULT now(),
    updated_at   timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT ux_student_addresses_student UNIQUE (tenant_id, student_id),
    CONSTRAINT ux_student_addresses_tenant_id UNIQUE (tenant_id, id),
    CONSTRAINT fk_student_addresses_student
        FOREIGN KEY (tenant_id, student_id) REFERENCES students (tenant_id, id) ON DELETE CASCADE
);

CREATE TRIGGER trg_student_addresses_updated_at BEFORE UPDATE ON student_addresses
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- ---------------------------------------------------------------------------
-- RLS.
-- ---------------------------------------------------------------------------
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
CREATE POLICY students_isolation ON students
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_registration_counters ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_registration_counters_isolation ON student_registration_counters
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_documents ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_documents_isolation ON student_documents
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_contacts ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_contacts_isolation ON student_contacts
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());

ALTER TABLE student_addresses ENABLE ROW LEVEL SECURITY;
CREATE POLICY student_addresses_isolation ON student_addresses
    USING (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id())
    WITH CHECK (current_rls_bypass() OR current_is_platform_admin() OR tenant_id = current_tenant_id());
