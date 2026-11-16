# GymSaaS — Sistema de Academia Multi-Tenant

Monólito modular (Spring Boot 3 + Java 21) com frontend Next.js 14, PostgreSQL 16 com
RLS, Flyway, autenticação JWT + refresh token rotativo e RBAC por academia.

> Fases 1–3 concluídas (documentação em `docs/`). Este repositório contém a **Fase 4 — gestão de alunos**.

## Estrutura

```
backend/    Spring Boot 3.5 (Java 21), Maven, JPA, Flyway, Security, Testcontainers
frontend/   Next.js 14 (App Router), TypeScript, Tailwind, MVVM
docker/     init do PostgreSQL (role de runtime NOSUPERUSER/NOBYPASSRLS)
docs/       documentação da Fase 1 + plano da Fase 2
docker-compose.yml
```


## Fase 4 — gestão de alunos

### Backend
- Módulo `com.gymsaas.student` em Clean Architecture (`domain`, `application`, `infrastructure`, `presentation`).
- Casos de uso: cadastro, consulta/listagem, atualização, status, documentos, contatos, endereço, vínculos com unidades, transferência, histórico e observações administrativas.
- Regras: CPF validado/normalizado e único por tenant entre alunos não excluídos; matrícula `AAAA`+5 dígitos por contador atômico (tenant/ano); vários vínculos ativos com exatamente um principal; transferência encerra/abre vínculos preservando histórico; status cadastral `PENDING/ACTIVE/INACTIVE/BLOCKED/ARCHIVED`; soft delete; `tenant_id` imutável e sempre do contexto autenticado.
- Segurança/LGPD: permissões `students:*` (compatíveis com `student:*`), documentos mascarados por padrão e revelação completa apenas com `students:read_sensitive`; CPF nunca registrado em logs. Ver `docs/security/06-student-data-retention.md`.

### API (alunos)
| Método | Endpoint | Permissão |
|--------|----------|-----------|
| GET · POST | `/api/students` | `students:read` · `students:create` |
| GET · PUT · DELETE | `/api/students/{id}` | `students:read` · `students:update` · `students:delete` |
| PATCH | `/api/students/{id}/status` | `students:change_status` |
| GET | `/api/students/{id}/history` | `students:read` |
| GET · POST | `/api/students/{id}/documents` | `students:read` · `students:manage_documents` |
| PUT · DELETE | `/api/students/{id}/documents/{documentId}` | `students:manage_documents` |
| GET · POST | `/api/students/{id}/contacts` | `students:read` · `students:manage_contacts` |
| PUT · DELETE | `/api/students/{id}/contacts/{contactId}` | `students:manage_contacts` |
| GET · PUT | `/api/students/{id}/address` | `students:read` · `students:update` |
| GET · POST | `/api/students/{id}/branches` | `students:read` · `students:manage_branches` |
| POST | `/api/students/{id}/transfer` | `students:manage_branches` |
| DELETE | `/api/students/{id}/branches/{assignmentId}` | `students:manage_branches` |
| GET · POST | `/api/students/{id}/notes` | `students:read` · `students:manage_notes` |

### Frontend
- Módulo `students` em MVVM (`/academy/students`, `/new`, `/[id]`, `/[id]/edit`) com busca por nome/documento, filtros de status/unidade, paginação, ordenação, skeleton e estados vazios.
- Detalhe com abas: visão geral, documentos, contatos, endereço, unidades/vínculos/transferência, histórico e observações.
- Tema claro/escuro com toggle persistido, aplicado globalmente.


## Fase 5 — planos, matrículas e contratos (backend)

- Módulos `com.gymsaas.plan`, `com.gymsaas.membership` e `com.gymsaas.contract` (Clean Architecture).
- Preço sempre calculado no backend e congelado na matrícula (snapshot imutável por trigger); histórico de preços imutável.
- Vigência: `fim = âncora + meses × sequência − 1 dia` (31/01 mensal → 27/02), sem deriva em renovações.
- Congelamento estende a vigência; limites por plano. Renovação cria nova matrícula com o preço vigente.
- Contrato versionado com SHA-256; PDF (OpenPDF) em volume `gymsaas_contract_storage`; aceite simples (não é assinatura digital qualificada).
- Job horário (`app.memberships.lifecycle.cron`) ativa/expira matrículas no fuso do tenant (`tenants.timezone`).
- Endpoints, permissões e regras: `docs/specs/2026-11-13-phase5-final-report.md`. Frontend da Fase 5 pendente.

## Subir a stack

```bash
cp .env.example .env
docker compose up -d --build
```

| Serviço  | URL |
|----------|-----|
| Frontend | http://localhost:3000 |
| Backend  | http://localhost:8080 |
| Swagger  | http://localhost:8080/swagger-ui.html |
| Health   | http://localhost:8080/actuator/health |
| Postgres | localhost:5432 |

Usuários de demonstração (criados apenas com `DEMO_DATA_ENABLED=true`):

| Usuário | Senha | Papel |
|---------|-------|-------|
| `superadmin@gymsaas.local` | `SuperAdmin@123` | `SAAS_ADMIN` + administrador da plataforma |
| `admin@gymsaas.local` | `Admin@123` | `TENANT_ADMIN` da Academia Demo |
| `reception@gymsaas.local` | `Reception@123` | `RECEPTIONIST` da Academia Demo |

O administrador da plataforma (`is_platform_admin`/claim `plat`) acessa
`/admin/tenants` sem vínculo com academia. Os demais usuários operam apenas dentro
das academias às quais pertencem.

## Desenvolvimento

### Backend

```bash
# PostgreSQL local (role de runtime criada pelo init script)
docker compose up -d postgres

cd backend
./mvnw spring-boot:run                       # testes unitários + integração
./mvnw verify                                # unit (*Test) + integração (*IT)

# banco vazio + migrations
DB_URL=jdbc:postgresql://localhost:5432/gymsaas \
DB_USER=gymsaas_app DB_PASSWORD=gymsaas_app_pw \
FLYWAY_USER=gymsaas_owner FLYWAY_PASSWORD=gymsaas_owner_pw \
./mvnw spring-boot:run
```

Sem `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, um par RSA efêmero é gerado no boot — ideal para
dev, mas invalida tokens a cada reinício. Em produção configure as chaves PEM.

### Frontend

```bash
cd frontend
npm install
NEXT_PUBLIC_API_URL=http://localhost:8080 npm run dev
```

## Segurança implementada

- **Access token** JWT RS256 (~15 min) mantido apenas em memória no frontend.
- **Refresh token** opaco (256 bits), persistido somente como SHA-256, em cookie
  `HttpOnly`, `SameSite=Lax`, path `/api/auth`, com rotação a cada uso e detecção de
  reuso (revoga a família inteira).
- **Logout** revoga a família e invalida imediatamente o access token (cache de revogação).
- **Senhas** com Argon2id; nenhuma senha em texto puro é persistida ou logada.
- **RBAC** com permissões granulares (`user:read`, `role:manage`, ...) verificadas por
  `@PreAuthorize`; papéis são validados contra a academia antes da atribuição.
- **Multi-tenancy**: o tenant vem exclusivamente do JWT (`tid`), nunca de header/body.
  O `RlsAwareDataSource` injeta `app.current_tenant`, `app.current_user` e
  `app.rls_bypass` em cada conexão; o banco aplica RLS com role de runtime sem
  `BYPASSRLS`.
- **Auditoria** append-only para login, logout, reuso de refresh token, criação de
  usuários e mudança de papéis/status.

### Fluxos de autenticação

| Método | Endpoint | Descrição |
|--------|----------|-----------|
| POST | `/api/auth/login` | valida credenciais, emite access + refresh |
| POST | `/api/auth/refresh` | rotaciona refresh (cookie) e reemite access |
| POST | `/api/auth/logout` | revoga família e limpa cookie |
| GET  | `/api/auth/me` | usuário, tenant, papéis e permissões |
| GET  | `/api/auth/tenants` | academias autorizadas |
| POST | `/api/auth/select-tenant` | fixa a academia na sessão |

### Endpoints de gestão (RBAC)

**Administração global (somente administrador da plataforma)**

| Método | Endpoint | Permissão |
|--------|----------|-----------|
| GET | `/api/admin/tenants` | `tenants:read` |
| POST | `/api/admin/tenants` | `tenants:create` |
| GET · PUT | `/api/admin/tenants/{id}` | `tenants:read` · `tenants:update` |
| PATCH | `/api/admin/tenants/{id}/status` | `tenants:suspend` |
| GET | `/api/admin/tenants/{id}/branches` | `tenants:read` ou `branches:read` |

**Academia ativa**

| Método | Endpoint | Permissão |
|--------|----------|-----------|
| GET · PUT | `/api/tenants/current` | `tenant:read` · `tenant:update` |
| GET | `/api/branches` | `branches:read` |
| POST | `/api/branches` | `branches:create` |
| GET · PUT | `/api/branches/{id}` | `branches:read` · `branches:update` |
| PATCH | `/api/branches/{id}/status` | `branches:deactivate` |
| GET · PUT | `/api/branches/{id}/settings` | `branches:read` · `branches:update` |
| GET | `/api/roles` · `/api/roles/{id}` | `role:read` |
| GET | `/api/permissions` | `permission:read` |
| GET | `/api/users` | `users:read` |
| POST | `/api/users` | `users:create` |
| PUT | `/api/users/{id}` | `users:update` |
| PATCH | `/api/users/{id}/status` | `users:deactivate` |
| PUT | `/api/users/{id}/roles` | `users:manage_roles` |
| GET · PUT | `/api/users/{id}/branches` | `users:read` · `users:assign_branch` |
| POST · DELETE | `/api/users/{id}/branches/{branchId}` | `users:assign_branch` |
| GET | `/api/audit-logs` | `audit:read` |

As permissões no plural da Fase 3 convivem com as singulares da Fase 2. As listagens
de academias, unidades e usuários são paginadas (`content`, `totalElements`, `page`,
`size`). O status da academia aceita `PENDING`, `ACTIVE`, `SUSPENDED` e `INACTIVE`;
apenas academias `ACTIVE` permitem login e seleção de tenant. Unidades usam
`ACTIVE`/`INACTIVE` e só unidades ativas podem ser vinculadas a usuários.

### Painéis do frontend

| Rota | Perfil | Função |
|------|--------|--------|
| `/dashboard` | autenticado | visão geral, sessão e permissões |
| `/admin/tenants` | administrador da plataforma | CRUD e ciclo de vida das academias |
| `/admin/tenants/[tenantId]` | administrador da plataforma | dados da academia e unidades vinculadas |
| `/academy/branches` | administrador da academia | CRUD, status e configurações das unidades |
| `/academy/users` | administrador da academia | usuários, papéis e vínculos com unidades |
| `/academy/settings` | administrador da academia | dados cadastrais da academia ativa |

## Banco de dados

Migrations em `backend/src/main/resources/db/migration`:

- `V1` extensões, `uuid_generate_v7()`, helpers de RLS e `updated_at`
- `V2` tenants, users, roles, permissions, vínculos e trigger de escopo de papel
- `V3` refresh tokens + audit logs append-only
- `V4` políticas RLS (tenants, roles, vínculos, refresh tokens, auditoria)
- `V5` seed de papéis/permissões de sistema
- `V6` branches, branch_settings, user_branch_access, `is_platform_admin`, documento
  do tenant, novos status e políticas RLS (inclui helper de administrador global)
- `V7` permissões plurais da Fase 3 e concessões para `SAAS_ADMIN`, `TENANT_ADMIN`,
  `BRANCH_MANAGER` e `RECEPTIONIST`
- `V8` alunos, documentos, contatos, endereços, contador de matrícula e RLS
- `V9` vínculos com unidades, histórico de status, observações, permissões `students:*` e RLS
- `V10` `tenants.timezone`, planos, histórico de preços, benefícios, permissões `plans:*`
- `V11` matrículas, congelamentos, histórico, contador de matrícula, permissões `memberships:*`
- `V12` contratos e aceites, permissões `contracts:*`

Duas credenciais de banco: `gymsaas_owner` (Flyway/superuser local) e `gymsaas_app`
(runtime, `NOSUPERUSER NOBYPASSRLS`). RLS só é efetiva para o segundo.

## Testes

- Unitários (21): `UuidV7`, `RefreshTokenCodec`, `JwtTokenService`, `BrDocuments`,
  fronteiras ArchUnit.
- Integração (16, Testcontainers PostgreSQL 16, role de runtime real):
  `AuthFlowIT` (login/refresh/rotação/reuso/logout), `RbacIT` (403/200 por permissão,
  criação de usuário, auditoria), `RlsIsolationIT` (isolamento e bloqueio cross-tenant
  no banco), `TenantAdminIT` (CRUD global, suspensão bloqueia login, 403 para admin de
  academia), `BranchManagementIT` (CRUD/configurações de unidade e invisibilidade
  cross-tenant) e `UserBranchAccessIT` (papéis, vínculos, unidade inativa e FK composta
  cross-tenant).
