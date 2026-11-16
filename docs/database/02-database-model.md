# Modelo de Banco de Dados - Fase 1

## Estratégia Geral

- **PostgreSQL 16** com extensões: `uuid-ossp`, `pgcrypto`, `btree_gin`
- **UUID v7** (timestamp-ordered) para chaves primárias — melhor para índices e sharding futuro
- **Shared Schema + RLS** para multi-tenancy
- **Flyway** para migrations (versionamento `V{major}_{minor}_{patch}__{description}.sql`)
- **Naming Convention**: `snake_case` tabelas/colunas; PK = `id`; FK = `{table}_id`

---

## Diagrama Entidade-Relacionamento (Mermaid)

```mermaid
erDiagram
    TENANTS ||--o{ TENANT_SUBSCRIPTIONS : has
    SAAS_PLANS ||--o{ TENANT_SUBSCRIPTIONS : defines
    TENANTS ||--o{ BRANCHES : owns
    BRANCHES ||--o{ GATEWAYS : hosts
    BRANCHES ||--o{ DEVICES : contains
    BRANCHES ||--o{ TURNSTILES : contains
    BRANCHES ||--o{ USERS : employs
    BRANCHES ||--o{ STUDENTS : serves
    BRANCHES ||--o{ PLANS : offers
    BRANCHES ||--o{ ACCESS_RULES : defines
    BRANCHES ||--o{ ACCESS_SCHEDULES : schedules

    USERS ||--o{ USER_TENANTS : belongs_to
    TENANTS ||--o{ USER_TENANTS : grants_access
    ROLES ||--o{ USER_TENANTS : assigns
    ROLES ||--o{ PERMISSIONS : grants
    PERMISSIONS }|--o{ ROLES : ""

    STUDENTS ||--o{ STUDENT_PLANS : subscribes
    PLANS ||--o{ STUDENT_PLANS : defines
    STUDENT_PLANS ||--o{ PAYMENTS : generates

    STUDENTS ||--o{ BIOMETRIC_PROFILES : has
    BIOMETRIC_PROFILES ||--o{ BIOMETRIC_ENROLLMENTS : enrolls
    BIOMETRIC_PROFILES ||--o{ BIOMETRIC_CONSENTS : consents

    GATEWAYS ||--o{ DEVICE_CREDENTIALS : authenticates
    DEVICES ||--o{ DEVICE_CREDENTIALS : uses
    GATEWAYS ||--o{ DEVICE_COMMANDS : receives
    DEVICES ||--o{ DEVICE_EVENTS : emits
    DEVICES ||--o{ ACCESS_LOGS : logs

    ACCESS_RULES ||--o{ ACCESS_SCHEDULES : time_bounds
    TIME_ZONES ||--o{ TIME_SPANS : contains
    ACCESS_RULES }|--o{ TIME_ZONES : ""

    AUDIT_LOGS }|..{ TENANTS : tracks
```

---

## Dicionário de Dados por Tabela

### 1. `tenants` — Academias (Tenants Raiz)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK, `gen_random_uuid()` | Identificador único |
| `name` | `varchar(150)` | NOT NULL | Nome fantasia |
| `legal_name` | `varchar(200)` | NOT NULL | Razão social |
| `cnpj` | `varchar(18)` | UNIQUE, NOT NULL | CNPJ formatado |
| `email` | `varchar(150)` | NOT NULL | Contato principal |
| `phone` | `varchar(20)` | | Telefone |
| `address` | `jsonb` | | Endereço estruturado |
| `status` | `varchar(20)` | NOT NULL, DEFAULT 'ACTIVE' | ACTIVE, SUSPENDED, CANCELLED |
| `settings` | `jsonb` | DEFAULT '{}' | Configurações customizadas (white-label, features) |
| `created_at` | `timestamptz` | NOT NULL, DEFAULT now() | |
| `updated_at` | `timestamptz` | NOT NULL, DEFAULT now() | |
| `deleted_at` | `timestamptz` | | Soft delete |

**Índices**: `idx_tenants_cnpj`, `idx_tenants_status`

---

### 2. `saas_plans` — Planos da Plataforma SaaS

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `code` | `varchar(50)` | UNIQUE, NOT NULL | Ex: STARTER, PRO, ENTERPRISE |
| `name` | `varchar(100)` | NOT NULL | |
| `description` | `text` | | |
| `monthly_price_cents` | `integer` | NOT NULL | Preço em centavos |
| `limits` | `jsonb` | NOT NULL | `{branches, students_per_branch, devices, gateways, log_retention_days, api_rate_limit}` |
| `features` | `jsonb` | DEFAULT '[]' | Lista de features habilitadas |
| `is_active` | `boolean` | DEFAULT true | |
| `created_at` | `timestamptz` | DEFAULT now() | |

---

### 3. `tenant_subscriptions` — Assinaturas Tenant ↔ Plano

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | |
| `saas_plan_id` | `uuid` | FK→saas_plans, NOT NULL | |
| `status` | `varchar(20)` | NOT NULL | TRIAL, ACTIVE, PAST_DUE, CANCELLED |
| `billing_cycle` | `varchar(10)` | NOT NULL | MONTHLY, YEARLY |
| `current_period_start` | `timestamptz` | NOT NULL | |
| `current_period_end` | `timestamptz` | NOT NULL | |
| `cancelled_at` | `timestamptz` | | |
| `trial_ends_at` | `timestamptz` | | |
| `metadata` | `jsonb` | DEFAULT '{}' | Dados gateway pagamento (Stripe, etc) |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_subscriptions_tenant`, `idx_subscriptions_status`

---

### 4. `branches` — Unidades da Academia

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | RLS |
| `code` | `varchar(20)` | NOT NULL | Código curto (ex: SP-01) |
| `name` | `varchar(150)` | NOT NULL | |
| `address` | `jsonb` | NOT NULL | |
| `phone` | `varchar(20)` | | |
| `email` | `varchar(150)` | | |
| `timezone` | `varchar(50)` | DEFAULT 'America/Sao_Paulo' | IANA TZ |
| `opening_hours` | `jsonb` | | Horários de funcionamento por dia |
| `settings` | `jsonb` | DEFAULT '{}' | Configurações locais |
| `status` | `varchar(20)` | DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, MAINTENANCE |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |
| `deleted_at` | `timestamptz` | | |

**Constraints**: `UK(tenant_id, code)`
**Índices**: `idx_branches_tenant`, `idx_branches_status`

---

### 5. `users` — Usuários do Sistema (Staff, Recepcionistas, Admins)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `email` | `varchar(150)` | UNIQUE, NOT NULL | Login |
| `password_hash` | `varchar(255)` | NOT NULL | Argon2id |
| `full_name` | `varchar(200)` | NOT NULL | |
| `phone` | `varchar(20)` | | |
| `avatar_url` | `varchar(500)` | | |
| `locale` | `varchar(10)` | DEFAULT 'pt-BR' | |
| `timezone` | `varchar(50)` | DEFAULT 'America/Sao_Paulo' | |
| `mfa_enabled` | `boolean` | DEFAULT false | TOTP |
| `mfa_secret` | `varchar(100)` | | Encryptado |
| `last_login_at` | `timestamptz` | | |
| `status` | `varchar(20)` | DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, LOCKED |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |
| `deleted_at` | `timestamptz` | | |

---

### 6. `roles` — Papéis (RBAC)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `code` | `varchar(50)` | UNIQUE, NOT NULL | SAAS_ADMIN, TENANT_ADMIN, BRANCH_MANAGER, RECEPTIONIST, TRAINER |
| `name` | `varchar(100)` | NOT NULL | |
| `description` | `text` | | |
| `is_system` | `boolean` | DEFAULT false | Roles protegidas |
| `created_at` | `timestamptz` | DEFAULT now() | |

---

### 7. `permissions` — Permissões Granulares

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `code` | `varchar(100)` | UNIQUE, NOT NULL | Ex: `student:create`, `access_rule:delete`, `device:reboot` |
| `resource` | `varchar(50)` | NOT NULL | student, plan, device, access_log, etc |
| `action` | `varchar(20)` | NOT NULL | create, read, update, delete, manage |
| `description` | `text` | | |
| `created_at` | `timestamptz` | DEFAULT now() | |

---

### 8. `role_permissions` — Role ↔ Permission (M:N)

| Coluna | Tipo | Constraints |
|--------|------|-------------|
| `role_id` | `uuid` | FK→roles, PK |
| `permission_id` | `uuid` | FK→permissions, PK |

---

### 9. `user_tenants` — Vínculo User ↔ Tenant + Role

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `user_id` | `uuid` | FK→users, NOT NULL | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | |
| `role_id` | `uuid` | FK→roles, NOT NULL | |
| `branch_ids` | `uuid[]` | DEFAULT '{}' | Restrição opcional a unidades |
| `is_default` | `boolean` | DEFAULT false | Contexto padrão |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `expires_at` | `timestamptz` | | Expiração acesso |

**Constraints**: `UK(user_id, tenant_id, role_id)`
**Índices**: `idx_user_tenants_user`, `idx_user_tenants_tenant`

---

### 10. `students` — Alunos

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | RLS |
| `branch_id` | `uuid` | FK→branches, NOT NULL | Unidade principal |
| `registration` | `varchar(50)` | NOT NULL | Matrícula única por tenant |
| `full_name` | `varchar(200)` | NOT NULL | |
| `cpf` | `varchar(14)` | | CPF formatado |
| `email` | `varchar(150)` | | |
| `phone` | `varchar(20)` | | |
| `birth_date` | `date` | | |
| `gender` | `varchar(1)` | | M, F, O |
| `address` | `jsonb` | | |
| `emergency_contact` | `jsonb` | | Nome, telefone, parentesco |
| `photo_url` | `varchar(500)` | | Foto cadastral (não biométrica) |
| `status` | `varchar(20)` | DEFAULT 'ACTIVE' | ACTIVE, INACTIVE, BLOCKED, PENDING |
| `block_reason` | `text` | | Motivo bloqueio |
| `notes` | `text` | | Observações internas |
| `metadata` | `jsonb` | DEFAULT '{}' | Campos customizados |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |
| `deleted_at` | `timestamptz` | | |

**Constraints**: `UK(tenant_id, registration)`
**Índices**: `idx_students_tenant_branch`, `idx_students_status`, `idx_students_cpf`

---

### 11. `plans` — Planos de Mensalidade (por Academia)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | RLS |
| `branch_id` | `uuid` | FK→branches | NULL = global do tenant |
| `name` | `varchar(100)` | NOT NULL | Ex: "Mensalidade Standard", "Anual Black" |
| `description` | `text` | | |
| `price_cents` | `integer` | NOT NULL | Valor em centavos |
| `billing_cycle` | `varchar(10)` | NOT NULL | MONTHLY, QUARTERLY, SEMI_ANNUAL, YEARLY |
| `duration_days` | `integer` | NOT NULL | Duração do ciclo |
| `access_rules` | `jsonb` | DEFAULT '[]' | Regras de acesso incluídas (horários, áreas) |
| `max_checkins_per_day` | `integer` | DEFAULT 1 | |
| `allows_guest` | `boolean` | DEFAULT false | |
| `guest_passes_per_month` | `integer` | DEFAULT 0 | |
| `is_active` | `boolean` | DEFAULT true | |
| `sort_order` | `integer` | DEFAULT 0 | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

---

### 12. `student_plans` — Vínculo Aluno ↔ Plano (Assinatura)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `student_id` | `uuid` | FK→students, NOT NULL | |
| `plan_id` | `uuid` | FK→plans, NOT NULL | |
| `status` | `varchar(20)` | NOT NULL | ACTIVE, EXPIRED, CANCELLED, PENDING_PAYMENT |
| `start_date` | `date` | NOT NULL | |
| `end_date` | `date` | | NULL = indeterminado |
| `auto_renew` | `boolean` | DEFAULT true | |
| `price_cents` | `integer` | NOT NULL | Preço no momento da contratação |
| `discount_cents` | `integer` | DEFAULT 0 | Desconto aplicado |
| `payment_method` | `varchar(30)` | | PIX, BOLETO, CREDIT_CARD, CASH |
| `payment_gateway_id` | `varchar(100)` | | ID no gateway (Stripe, Asaas, etc) |
| `metadata` | `jsonb` | DEFAULT '{}' | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |
| `cancelled_at` | `timestamptz` | | |

**Índices**: `idx_student_plans_student`, `idx_student_plans_status`, `idx_student_plans_end_date`

---

### 13. `payments` — Pagamentos

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `student_plan_id` | `uuid` | FK→student_plans, NOT NULL | |
| `amount_cents` | `integer` | NOT NULL | |
| `currency` | `varchar(3)` | DEFAULT 'BRL' | |
| `status` | `varchar(20)` | NOT NULL | PENDING, PAID, FAILED, REFUNDED, PARTIAL |
| `payment_method` | `varchar(30)` | NOT NULL | |
| `payment_date` | `timestamptz` | | Data efetiva |
| `due_date` | `date` | NOT NULL | Vencimento |
| `gateway_transaction_id` | `varchar(100)` | | ID no gateway |
| `gateway_response` | `jsonb` | | Response bruto |
| `receipt_url` | `varchar(500)` | | Comprovante |
| `notes` | `text` | | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_payments_student_plan`, `idx_payments_status`, `idx_payments_due_date`

---

### 14. `biometric_profiles` — Perfil Biométrico do Aluno

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `student_id` | `uuid` | FK→students, UNIQUE, NOT NULL | 1:1 |
| `face_template_hash` | `varchar(64)` | | SHA-256 do template (para dedup) |
| `face_quality_score` | `integer` | | Score qualidade (0-1000) |
| `enrollment_device_id` | `uuid` | FK→devices | Dispositivo que capturou |
| `enrollment_method` | `varchar(20)` | NOT NULL | PHOTO_UPLOAD, DEVICE_CAPTURE, TEMPLATE_IMPORT |
| `liveness_verified` | `boolean` | DEFAULT false | Verificação de vivacidade |
| `status` | `varchar(20)` | DEFAULT 'PENDING' | PENDING, ACTIVE, REVOKED, EXPIRED |
| `expires_at` | `timestamptz` | | Expiração biométrica (LGPD) |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |
| `revoked_at` | `timestamptz` | | |
| `revoked_reason` | `text` | | |

---

### 15. `biometric_enrollments` — Enrolamentos por Dispositivo

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `biometric_profile_id` | `uuid` | FK→biometric_profiles, NOT NULL | |
| `device_id` | `uuid` | FK→devices, NOT NULL | |
| `device_user_id` | `bigint` | NOT NULL | ID do usuário no equipamento (int64) |
| `template_version` | `integer` | DEFAULT 1 | Versão do template no device |
| `sync_status` | `varchar(20)` | DEFAULT 'PENDING' | PENDING, SYNCED, FAILED, CONFLICT |
| `last_synced_at` | `timestamptz` | | |
| `error_message` | `text` | | Último erro |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Constraints**: `UK(biometric_profile_id, device_id)`
**Índices**: `idx_enrollments_device`, `idx_enrollments_sync_status`

---

### 16. `biometric_consents` — Consentimento LGPD (Art. 7º, 11)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `student_id` | `uuid` | FK→students, NOT NULL | |
| `consent_version` | `varchar(20)` | NOT NULL | Versão do termo |
| `purpose` | `varchar(100)` | NOT NULL | ACCESS_CONTROL, ATTENDANCE, SECURITY |
| `legal_basis` | `varchar(50)` | NOT NULL | CONSENT, LEGITIMATE_INTEREST, CONTRACT |
| `granted` | `boolean` | NOT NULL | |
| `granted_at` | `timestamptz` | | |
| `revoked_at` | `timestamptz` | | |
| `revoked_reason` | `text` | | |
| `ip_address` | `inet` | | IP no momento |
| `user_agent` | `text` | | |
| `signature_hash` | `varchar(64)` | | Hash da assinatura digital |
| `created_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_consents_student`, `idx_consents_granted`

---

### 17. `gateways` — Gateways Locais (Hardware por Unidade)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `branch_id` | `uuid` | FK→branches, NOT NULL | |
| `serial_number` | `varchar(50)` | UNIQUE, NOT NULL | SN do hardware (RPi MAC, etc) |
| `name` | `varchar(100)` | NOT NULL | Ex: "Gateway Principal - Recepção" |
| `local_ip` | `inet` | | IP LAN atual |
| `public_ip` | `inet` | | IP público (se exposto) |
| `mqtt_topic_prefix` | `varchar(100)` | | Prefixo tópicos MQTT |
| `version` | `varchar(20)` | | Versão firmware gateway |
| `status` | `varchar(20)` | DEFAULT 'OFFLINE' | ONLINE, OFFLINE, SYNCING, ERROR |
| `last_seen_at` | `timestamptz` | | Último heartbeat |
| `last_sync_at` | `timestamptz` | | Último sync completo |
| `config` | `jsonb` | DEFAULT '{}' | Configurações gateway |
| `certificate_pem` | `text` | | Cert mTLS cliente (público) |
| `certificate_expires_at` | `timestamptz` | | Validade cert |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_gateways_branch`, `idx_gateways_status`, `idx_gateways_last_seen`

---

### 18. `devices` — Equipamentos Control iD (iDFace, iDBlock, etc)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `gateway_id` | `uuid` | FK→gateways, NOT NULL | |
| `branch_id` | `uuid` | FK→branches, NOT NULL | Denormalizado para queries |
| `serial_number` | `varchar(50)` | UNIQUE, NOT NULL | SN do equipamento |
| `model` | `varchar(50)` | NOT NULL | IDFACE, IDFACE_MAX, IDBLOCK, IDBLOCK_NEXT, IDBOX, IDFLEX |
| `firmware_version` | `varchar(30)` | | |
| `name` | `varchar(100)` | NOT NULL | Ex: "Catraca Entrada Principal" |
| `ip_address` | `inet` | NOT NULL | IP LAN fixo |
| `mac_address` | `macaddr` | | |
| `device_type` | `varchar(20)` | NOT NULL | TERMINAL, TURNSTILE, CONTROLLER |
| `location` | `varchar(100)` | | Descrição física |
| `area_from_id` | `uuid` | FK→areas | Área origem (portal) |
| `area_to_id` | `uuid` | FK→areas | Área destino |
| `portal_id` | `bigint` | | ID do portal no equipamento |
| `capabilities` | `jsonb` | DEFAULT '{}' | `{face: true, fingerprint: false, card: true, qr: true, uhf: false, liveness: true}` |
| `config` | `jsonb` | DEFAULT '{}' | Configs específicas (thresholds, timeouts) |
| `mode` | `varchar(20)` | DEFAULT 'STANDALONE' | STANDALONE, PRO, ENTERPRISE |
| `status` | `varchar(20)` | DEFAULT 'OFFLINE' | ONLINE, OFFLINE, SYNCING, ERROR, MAINTENANCE |
| `last_seen_at` | `timestamptz` | | |
| `last_sync_at` | `timestamptz` | | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_devices_gateway`, `idx_devices_branch`, `idx_devices_status`, `idx_devices_serial`

---

### 19. `turnstiles` — Catracas (Especialização de Device)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK, FK→devices | Herança 1:1 |
| `direction` | `varchar(20)` | NOT NULL | ENTRY, EXIT, BIDIRECTIONAL |
| `gate_mode` | `varchar(20)` | DEFAULT 'NORMAL' | NORMAL, FREE_ENTRY, FREE_EXIT, LOCKED |
| `entry_sensor_enabled` | `boolean` | DEFAULT true | |
| `exit_sensor_enabled` | `boolean` | DEFAULT true | |
| `turnstile_config` | `jsonb` | DEFAULT '{}' | Timeouts, sentidos, intertravamento |

---

### 20. `device_credentials` — Credenciais de Acesso aos Equipamentos

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `gateway_id` | `uuid` | FK→gateways, NOT NULL | |
| `device_id` | `uuid` | FK→devices, NOT NULL | |
| `username` | `varchar(50)` | NOT NULL | Default: admin |
| `password_hash` | `varchar(255)` | NOT NULL | Argon2id (senha do equipamento) |
| `api_key` | `varchar(100)` | | Se equipamento suportar |
| `is_active` | `boolean` | DEFAULT true | |
| `rotated_at` | `timestamptz` | | Última rotação |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Constraints**: `UK(gateway_id, device_id)`
**Segurança**: Criptografia em repouso (pgcrypto) + rotação periódica

---

### 21. `access_rules` — Regras de Acesso

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `branch_id` | `uuid` | FK→branches, NOT NULL | |
| `name` | `varchar(100)` | NOT NULL | |
| `description` | `text` | | |
| `type` | `varchar(20)` | NOT NULL | ALLOW, DENY |
| `priority` | `integer` | DEFAULT 0 | Maior = mais prioritário |
| `conditions` | `jsonb` | NOT NULL | `{student_status: [], plan_types: [], areas: [], custom: {}}` |
| `actions` | `jsonb` | NOT NULL | `{grant: true, message: "", door_open_time: 5, log_level: "FULL"}` |
| `is_active` | `boolean` | DEFAULT true | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

---

### 22. `time_zones` — Horários (Conjunto de Intervalos)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `branch_id` | `uuid` | FK→branches, NOT NULL | |
| `name` | `varchar(100)` | NOT NULL | Ex: "Horário Comercial", "Fins de Semana" |
| `description` | `text` | | |
| `is_active` | `boolean` | DEFAULT true | |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

---

### 23. `time_spans` — Intervalos de Horário

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `time_zone_id` | `uuid` | FK→time_zones, NOT NULL | |
| `start_seconds` | `integer` | NOT NULL | Segundos desde 00:00 (0-86399) |
| `end_seconds` | `integer` | NOT NULL | |
| `sun` | `boolean` | DEFAULT false | |
| `mon` | `boolean` | DEFAULT false | |
| `tue` | `boolean` | DEFAULT false | |
| `wed` | `boolean` | DEFAULT false | |
| `thu` | `boolean` | DEFAULT false | |
| `fri` | `boolean` | DEFAULT false | |
| `sat` | `boolean` | DEFAULT false | |
| `holiday_type_1` | `boolean` | DEFAULT false | Feriado tipo 1 |
| `holiday_type_2` | `boolean` | DEFAULT false | Feriado tipo 2 |
| `holiday_type_3` | `boolean` | DEFAULT false | Feriado tipo 3 |

**Constraints**: `CHECK(start_seconds < end_seconds)`

---

### 24. `access_rule_time_zones` — Regra ↔ Horário (M:N)

| Coluna | Tipo | Constraints |
|--------|------|-------------|
| `access_rule_id` | `uuid` | FK→access_rules, PK |
| `time_zone_id` | `uuid` | FK→time_zones, PK |

---

### 25. `access_logs` — Logs de Acesso (Alta Volume)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | RLS, denormalizado |
| `branch_id` | `uuid` | FK→branches, NOT NULL | |
| `device_id` | `uuid` | FK→devices, NOT NULL | |
| `student_id` | `uuid` | FK→students | NULL se não identificado |
| `user_id` | `uuid` | FK→users | Staff access |
| `event_type` | `varchar(30)` | NOT NULL | GRANTED, DENIED, TIMEOUT, PENDING, BUTTON, WEB, INTERCOM, GIVE_UP |
| `event_code` | `integer` | NOT NULL | Código Control iD (1-15) |
| `portal_id` | `bigint` | | Portal no equipamento |
| `identifier_id` | `bigint` | | ID leitor (wiegand, bio, etc) |
| `credential_type` | `varchar(20)` | | FACE, CARD, QR, PIN, PASSWORD, FINGERPRINT, UHF |
| `credential_value` | `varchar(100)` | | Valor lido (card number, etc) |
| `confidence` | `integer` | | Score facial (0-1800) |
| `face_mask` | `boolean` | | |
| `temperature` | `numeric(4,1)` | | Se equipamento tiver sensor |
| `direction` | `varchar(10)` | | ENTRY, EXIT, UNKNOWN |
| `message` | `varchar(200)` | | Msg exibida no display |
| `raw_payload` | `jsonb` | | Payload bruto do evento |
| `occurred_at` | `timestamptz` | NOT NULL | Timestamp do evento |
| `received_at` | `timestamptz` | DEFAULT now() | Quando chegou no SaaS |
| `synced` | `boolean` | DEFAULT true | Se veio via gateway sync |
| `created_at` | `timestamptz` | DEFAULT now() | |

**Particionamento**: `PARTITION BY RANGE (occurred_at)` — mensal
**Índices**: 
- `idx_access_logs_tenant_occurred` (tenant_id, occurred_at DESC)
- `idx_access_logs_branch_occurred` (branch_id, occurred_at DESC)
- `idx_access_logs_student_occurred` (student_id, occurred_at DESC)
- `idx_access_logs_device_occurred` (device_id, occurred_at DESC)
- `idx_access_logs_event_type` (event_type)

---

### 26. `device_commands` — Comandos Enviados aos Equipamentos

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `gateway_id` | `uuid` | FK→gateways, NOT NULL | |
| `device_id` | `uuid` | FK→devices, NOT NULL | |
| `command_type` | `varchar(50)` | NOT NULL | OPEN_DOOR, OPEN_TURNSTILE, SHOW_MESSAGE, REBOOT, SYNC_USERS, SYNC_CONFIG, UPDATE_FIRMWARE, PLAY_BUZZER |
| `parameters` | `jsonb` | NOT NULL | Parâmetros específicos |
| `priority` | `integer` | DEFAULT 0 | HIGH, NORMAL, LOW |
| `status` | `varchar(20)` | DEFAULT 'PENDING' | PENDING, SENT, ACKNOWLEDGED, EXECUTED, FAILED, EXPIRED |
| `sent_at` | `timestamptz` | | |
| `acknowledged_at` | `timestamptz` | | Gateway confirmou recebimento |
| `executed_at` | `timestamptz` | | Equipamento executou |
| `result` | `jsonb` | | Resultado/resposta |
| `error_message` | `text` | | |
| `expires_at` | `timestamptz` | | TTL do comando |
| `idempotency_key` | `varchar(100)` | UNIQUE | Prevenção duplicidade |
| `created_at` | `timestamptz` | DEFAULT now() | |
| `updated_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_commands_gateway_status`, `idx_commands_device_status`, `idx_commands_idempotency`

---

### 27. `device_events` — Eventos Recebidos dos Equipamentos (via Monitor/Push)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `gateway_id` | `uuid` | FK→gateways, NOT NULL | |
| `device_id` | `uuid` | FK→devices, NOT NULL | |
| `event_type` | `varchar(50)` | NOT NULL | ACCESS_LOG, ALARM_LOG, DOOR_STATE, TURNSTILE_EVENT, OPERATION_MODE, DEVICE_ALIVE, REMOTE_ENROLL |
| `event_subtype` | `varchar(50)` | | INSERT, UPDATE, DELETE, OPEN, CLOSE, TURN_LEFT, TURN_RIGHT, GIVE_UP |
| `payload` | `jsonb` | NOT NULL | Dados brutos do evento |
| `processed` | `boolean` | DEFAULT false | Se já processado pelo domain |
| `processing_error` | `text` | | |
| `received_at` | `timestamptz` | DEFAULT now() | |
| `occurred_at` | `timestamptz` | NOT NULL | Timestamp do equipamento |
| `created_at` | `timestamptz` | DEFAULT now() | |

**Índices**: `idx_device_events_gateway_received`, `idx_device_events_device_type`, `idx_device_events_processed`

---

### 28. `audit_logs` — Auditoria Completa (LGPD, Compliance)

| Coluna | Tipo | Constraints | Descrição |
|--------|------|-------------|-----------|
| `id` | `uuid` | PK | |
| `tenant_id` | `uuid` | FK→tenants, NOT NULL | RLS |
| `actor_type` | `varchar(20)` | NOT NULL | USER, SYSTEM, GATEWAY, API_CLIENT |
| `actor_id` | `uuid` | | ID do ator (user_id, gateway_id, etc) |
| `action` | `varchar(100)` | NOT NULL | CREATE, UPDATE, DELETE, LOGIN, EXPORT, ACCESS_GRANT, CONSENT_GRANT, CONSENT_REVOKE |
| `resource_type` | `varchar(50)` | NOT NULL | student, access_rule, biometric_profile, etc |
| `resource_id` | `uuid` | | |
| `old_values` | `jsonb` | | Estado anterior |
| `new_values` | `jsonb` | | Estado posterior |
| `metadata` | `jsonb` | DEFAULT '{}' | IP, user-agent, correlation_id |
| `risk_level` | `varchar(10)` | DEFAULT 'LOW' | LOW, MEDIUM, HIGH, CRITICAL |
| `occurred_at` | `timestamptz` | DEFAULT now() | |

**Particionamento**: Mensal por `occurred_at`
**Índices**: `idx_audit_tenant_occurred`, `idx_audit_actor`, `idx_audit_resource`, `idx_audit_action`

---

## Row Level Security (RLS) — Políticas

```sql
-- Habilita RLS em todas as tabelas multi-tenant
ALTER TABLE tenants ENABLE ROW LEVEL SECURITY;
ALTER TABLE branches ENABLE ROW LEVEL SECURITY;
ALTER TABLE students ENABLE ROW LEVEL SECURITY;
-- ... (todas as tabelas com tenant_id)

-- Função para obter tenant_id do contexto
CREATE OR REPLACE FUNCTION current_tenant_id() RETURNS uuid AS $$
  SELECT current_setting('app.current_tenant', true)::uuid;
$$ LANGUAGE sql STABLE;

-- Política padrão: isolamento total por tenant
CREATE POLICY tenant_isolation ON branches
  USING (tenant_id = current_tenant_id())
  WITH CHECK (tenant_id = current_tenant_id());

-- Replicar para: students, plans, student_plans, payments, 
-- biometric_profiles, biometric_enrollments, biometric_consents,
-- access_rules, time_zones, access_logs, device_commands, 
-- device_events, audit_logs, gateways, devices, turnstiles,
-- device_credentials

-- Exceção: saas_plans (global, read-only para tenants)
CREATE POLICY saas_plans_read ON saas_plans FOR SELECT USING (is_active = true);

-- Admins SaaS veem tudo (role SAAS_ADMIN)
CREATE POLICY saas_admin_all ON tenants FOR ALL
  USING (current_setting('app.current_role', true) = 'SAAS_ADMIN');
```

---

## Estratégia de Migrations (Flyway)

### Estrutura

```
src/main/resources/db/migration/
├── V1_0_0__baseline.sql                 -- Schema vazio, extensões, funções RLS
├── V1_1_0__tenants_and_saas.sql         -- tenants, saas_plans, subscriptions
├── V1_2_0__users_and_rbac.sql           -- users, roles, permissions, user_tenants
├── V1_3_0__branches_and_gateways.sql    -- branches, gateways, devices, turnstiles
├── V1_4_0__students_and_plans.sql       -- students, plans, student_plans, payments
├── V1_5_0__biometric.sql                -- biometric_profiles, enrollments, consents
├── V1_6_0__access_control.sql           -- access_rules, time_zones, time_spans, rule_tz
├── V1_7_0__logs_and_audit.sql           -- access_logs (partitioned), device_commands, device_events, audit_logs
├── V1_8_0__indexes_and_rls.sql          -- Índices compostos, policies RLS, triggers updated_at
└── V1_9_0__seed_data.sql                -- Roles base, permissions, plano SaaS default
```

### Convenções

- **Nunca** alterar migration já aplicada (exceto em dev local com `flyway clean`)
- **Sempre** `IF NOT EXISTS` para objetos
- **Triggers** `updated_at` via função genérica
- **Dados de seed** apenas em migration dedicada (V1_9_0)
- **Rollback**: Scripts `U{version}__rollback_{description}.sql` opcionais

---

## Capacidade e Particionamento

| Tabela | Volume Estimado (10 academias, 5k alunos) | Particionamento |
|--------|-------------------------------------------|-----------------|
| `access_logs` | ~500k/dia → 15M/mês | **RANGE mensal** em `occurred_at` |
| `device_events` | ~100k/dia | RANGE mensal em `received_at` |
| `audit_logs` | ~50k/dia | RANGE mensal em `occurred_at` |
| `device_commands` | ~10k/dia | RANGE mensal em `created_at` |
| Demais | < 1M total | Não particionado |

### Retenção

| Tabela | Retenção (configurável por plano) | Arquivamento |
|--------|-----------------------------------|--------------|
| `access_logs` | 90d / 1y / 5y | `pg_dump` → S3 Glacier + tabela `access_logs_archive` |
| `device_events` | 30d / 90d | Compactação + S3 |
| `audit_logs` | 5 anos (LGPD) | Imutável, WORM storage |
| `device_commands` | 90d | Purge automático |

---

## Checklist Validação Modelo

- [ ] ER revisado por DBA
- [ ] Naming convention consistente
- [ ] FKs com `ON DELETE RESTRICT` (padrão) ou `SET NULL` onde aplicável
- [ ] Índices cobrindo queries principais (EXPLAIN ANALYZE)
- [ ] RLS policies testadas com múltiplos tenants
- [ ] Particionamento `access_logs` validado
- [ ] Triggers `updated_at` funcionando
- [ ] Seed data: roles, permissions, saas_plans
- [ ] Flyway baseline + migrations aplicam sem erro em DB limpo