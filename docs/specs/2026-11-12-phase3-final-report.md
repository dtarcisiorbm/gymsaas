# Fase 3 — Relatório Final de Execução

> Data: 2026-11-12 · Escopo: gestão de academias (tenants), unidades (branches) e
> usuários, RBAC, painéis SaaS/academia e validação ponta a ponta.
> **Fase 4 não iniciada** — depende de aprovação explícita.

## 1. Diagnóstico inicial

A Fase 2 entregou autenticação JWT com refresh rotativo, multi-tenancy por claim
`tid`, RBAC por permissão, auditoria append-only, RLS no PostgreSQL e o esqueleto do
frontend (`/login`, `/select-tenant`, `/dashboard`). O tenant era somente leitura
(`GET /api/tenants/current`), não existia administração global de academias, não havia
unidades nem vínculo de usuário com unidade, e a criação de usuários não permitia
selecionar papéis ou unidades. Não havia como um administrador da plataforma operar
sem vínculo com uma academia.

## 2. Lacunas identificadas e tratadas

| Lacuna | Tratamento |
|--------|-----------|
| Sem CRUD global de academias | `TenantAdminService` + `/api/admin/tenants`, restrito ao administrador da plataforma |
| Status de academia limitado/inconsistente | `PENDING`, `ACTIVE`, `SUSPENDED`, `INACTIVE`; `CANCELLED` convertido para `INACTIVE` |
| Sem identidade de administrador global | `users.is_platform_admin`, claim JWT `plat` e GUC `app.platform_admin` |
| Sem unidades | Módulo `branch` com CRUD, endereço, status e `branch_settings` 1:1 |
| Sem vínculo usuário–unidade | `user_branch_access` com FKs compostas por tenant |
| Criação de usuário sem papéis/unidades | `POST /api/users` aceita `roleIds` e `branchIds`; endpoints dedicados para papéis e unidades |
| Sem painéis de gestão | `/admin/tenants`, `/admin/tenants/[tenantId]`, `/academy/branches`, `/academy/users`, `/academy/settings` |
| Login de plataforma sem destino | Redireciona para `/admin/tenants` quando não há academia ativa |
| Isolamento cross-tenant em novas tabelas | RLS + FKs compostas + resolução de tenant exclusivamente pelo token |

## 3. Arquivos criados e modificados

### Backend — novos

- `com/gymsaas/branch/` (domínio, aplicação, persistência e apresentação):
  `Branch`, `BranchSettings`, `BranchStatus`, `UserBranchAccessRepository`,
  `BranchService`, `BranchDtos`, `BranchController` e adaptadores JPA.
- `com/gymsaas/tenant/application/TenantAdminService.java`
- `com/gymsaas/tenant/presentation/TenantAdminController.java`
- `com/gymsaas/identity/application/UserAdminService.java`
- `com/gymsaas/identity/application/UserAdminDtos.java`
- `com/gymsaas/shared/domain/PageResult.java`
- `com/gymsaas/shared/domain/BrDocuments.java`
- `com/gymsaas/shared/application/CurrentPrincipal.java`
- Testes: `BrDocumentsTest`, `TenantAdminIT`, `BranchManagementIT`,
  `UserBranchAccessIT`.

### Backend — modificados

- `AuthService` / `AuthDtos`: status `ACTIVE` obrigatório, `platformAdmin` na sessão,
  concessão de `SAAS_ADMIN` ao administrador da plataforma.
- `User` / `UserRepository` / JPA e adaptador: flag de plataforma e listagem paginada.
- `UserController`: listagem paginada, edição, status, papéis e unidades.
- `Tenant` / `TenantStatus` / `TenantDtos` / `TenantController`: documento, novos
  status e atualização da academia ativa.
- `RbacService` e repositórios: concessões plurais e validação de escopo.
- `JwtTokenService` / `PrincipalMapper` / `TenantContextFilter` /
  `RlsAwareDataSource`: claim `plat` e GUC de administrador.
- `GlobalExceptionHandler`: novos códigos de erro de conflito/validação.
- `DemoDataSeeder` / `DemoDataProperties`: academia demo, unidade demo, superadmin.
- `application.yml`: seed e configuração da fase.
- `RlsIsolationIT` / `AbstractIntegrationTest`: 9 tabelas com RLS e apoio aos novos ITs.

### Frontend — novos

- `src/features/tenants/{model,data,viewmodel}` (`TenantApi`, `TenantsViewModel`,
  `TenantDetailViewModel`, `AcademySettingsViewModel`).
- `src/features/branches/{model,data,viewmodel}`.
- `src/features/users/{model,data,viewmodel}`.
- `src/components/layout/AppNav.tsx` e `PageShell.tsx`.
- `src/components/ui/{Alert,Modal,Pagination}.tsx`.
- `src/core/{format,validation}.ts` e `src/core/http/query.ts`.
- Páginas `src/app/admin/tenants/page.tsx`,
  `src/app/admin/tenants/[tenantId]/page.tsx`,
  `src/app/academy/branches/page.tsx`,
  `src/app/academy/users/page.tsx`,
  `src/app/academy/settings/page.tsx`.

### Frontend — modificados

- `ApiClient` (`put`, `patch`, `delete`), `SessionManager`/tipos de auth
  (`platformAdmin`), `RequireAuth` (`requireTenant`, `requirePlatformAdmin`).
- `AppHeader`, `login`, `select-tenant`, `dashboard`, `LoginViewModel`,
  `DashboardApi`/`DashboardViewModel`.

### Documentação

- `README.md` (endpoints, painéis, credenciais, migrations e testes).
- `docs/specs/2026-11-12-phase3-plan.md` (plano aprovado).
- `docs/specs/2026-11-12-phase3-ledger.md` (evidências).
- Este relatório.

## 4. Migrations Flyway

Nenhuma migration já aplicada foi editada; apenas duas migrations aditivas foram
criadas.

### `V6__branches_and_platform_admin.sql`

- `tenants.document` adicionado, preenchido a partir de `cnpj` e tornado `NOT NULL`;
  índice único parcial por documento ativo.
- Status de tenant restrito a `PENDING`, `ACTIVE`, `SUSPENDED`, `INACTIVE`;
  `CANCELLED` migrado para `INACTIVE`.
- `users.is_platform_admin` + índice parcial.
- `branches` (nome, código único por tenant, documento, contato, endereço, status
  `ACTIVE`/`INACTIVE`, timestamps e trigger de `updated_at`).
- `branch_settings` 1:1, timezone, janela de funcionamento, capacidade, acesso 24h e
  `settings` JSONB; FK composta `(tenant_id, branch_id)`.
- `user_branch_access` com FKs compostas para `user_tenants(tenant_id, id)` e
  `branches(tenant_id, id)`, impedindo vínculo cross-tenant no banco.
- RLS habilitada em `branches`, `branch_settings` e `user_branch_access`.
- Função `current_is_platform_admin()` e políticas atualizadas para academias,
  papéis, vínculos, papéis de usuário e auditoria.

### `V7__phase3_permissions.sql`

- Novas permissões: `tenants:create|read|update|suspend`,
  `branches:create|read|update|deactivate`,
  `users:create|read|update|deactivate|assign_branch|manage_roles`.
- Concessão a `SAAS_ADMIN`, `TENANT_ADMIN`, `BRANCH_MANAGER` e `RECEPTIONIST`
  conforme o escopo de cada papel. As permissões singulares da Fase 2 foram mantidas
  para compatibilidade.

## 5. Endpoints

### Administração global (somente administrador da plataforma)

| Método | Endpoint | Permissão |
|--------|----------|-----------|
| GET | `/api/admin/tenants` (paginado, filtros `search`/`status`) | `tenants:read` |
| POST | `/api/admin/tenants` | `tenants:create` |
| GET | `/api/admin/tenants/{id}` | `tenants:read` |
| PUT | `/api/admin/tenants/{id}` | `tenants:update` |
| PATCH | `/api/admin/tenants/{id}/status` | `tenants:suspend` |
| GET | `/api/admin/tenants/{id}/branches` (paginado) | `tenants:read` ou `branches:read` |

### Academia ativa

| Método | Endpoint | Permissão |
|--------|----------|-----------|
| GET · PUT | `/api/tenants/current` | `tenant:read` / `tenant:update` (ou plurais) |
| GET | `/api/branches` (paginado) | `branches:read` |
| POST | `/api/branches` | `branches:create` |
| GET · PUT | `/api/branches/{id}` | `branches:read` / `branches:update` |
| PATCH | `/api/branches/{id}/status` | `branches:deactivate` |
| GET · PUT | `/api/branches/{id}/settings` | `branches:read` / `branches:update` |
| GET | `/api/users` (paginado, filtros `search`/`status`) | `users:read` ou `user:read` |
| POST | `/api/users` (papéis e unidades opcionais) | `users:create` ou `user:create` |
| PUT | `/api/users/{id}` | `users:update` ou `user:update` |
| PATCH | `/api/users/{id}/status` | `users:deactivate` ou `user:update` |
| PUT | `/api/users/{id}/roles` | `users:manage_roles` ou `user:assign_role` |
| GET · PUT | `/api/users/{id}/branches` | `users:read` / `users:assign_branch` |
| POST · DELETE | `/api/users/{id}/branches/{branchId}` | `users:assign_branch` |

Endpoints existentes preservados: `/api/auth/*`, `/api/roles`, `/api/roles/{id}`,
`/api/permissions`, `/api/audit-logs`, `/actuator/health`.

Erros de domínio principais: `TENANT_DOCUMENT_EXISTS`, `BRANCH_CODE_EXISTS`,
`BRANCH_NOT_ALLOWED`, `INVALID_CREDENTIALS`, validação `400` e acesso `403`.

## 6. Funcionalidades entregues

- **Academias**: cadastro com nome, razão social, CPF/CNPJ, e-mail e telefone;
  documento normalizado e único; edição; ciclo de vida
  `PENDING → ACTIVE → SUSPENDED → INACTIVE`; listagem com busca, filtro de status e
  paginação; tela de detalhe com dados e unidades.
- **Administrador da plataforma**: usuário sem vínculo de academia, flag
  `is_platform_admin`, claim JWT `plat`, acesso global, redirecionamento de login e
  navegação dedicada. Nenhum outro perfil acessa `/api/admin/tenants` (403 comprovado).
- **Unidades**: CRUD, código único por academia, endereço, contato, status
  `ACTIVE`/`INACTIVE` e configurações (timezone, horário, capacidade, acesso 24h).
- **Usuários**: listagem paginada, criação com senha, papéis e unidades, edição de
  dados, ativação/desativação, substituição de papéis e gestão de vínculos com
  unidades. Unidades inativas ou de outro tenant são rejeitadas.
- **Isolamento**: tenant sempre resolvido do JWT; consultas escopadas; RLS em 9 tabelas;
  FKs compostas; administrador de academia recebe 403 em rotas globais; unidade de
  outro tenant é invisível e não vinculável.
- **Sessão**: login/refresh/seleção de academia exigem academia `ACTIVE`; usuário
  inativo não autentica; `platformAdmin` propagado em `/me` e na sessão.
- **Frontend**: MVVM (view → ViewModel → API), formulários validados, feedback de
  erro/sucesso, estados de carregamento, tabelas com filtros e paginação, navegação
  por permissão e layout consistente com a Fase 2.
- **Dados demo**: superadmin, academia demo, unidade `BR-DEMO` e usuários existentes,
  criados de forma idempotente quando `DEMO_DATA_ENABLED=true`.

## 7. Testes executados e resultados

Comando: `./mvnw -Dmaven.repo.local=/tmp/opencode/m2repo verify` (JDK 21).

| Suíte | Testes | Resultado |
|-------|--------|-----------|
| Unitários | 21 | 0 falhas |
| Integração (Testcontainers PostgreSQL 16) | 16 | 0 falhas |
| **Total** | **37** | **BUILD SUCCESS** |

Unitários: `BrDocumentsTest` (5), `UuidV7Test` (4), `RefreshTokenCodecTest` (3),
`JwtTokenServiceTest` (4), `ArchitectureBoundaryTest` (5).

Integração: `AuthFlowIT` (4), `RbacIT` (3), `RlsIsolationIT` (3),
`TenantAdminIT` (2), `BranchManagementIT` (2), `UserBranchAccessIT` (2).

Cobertura funcional dos novos ITs:

- criação/edição/status de academia, conflito de documento, suspensão bloqueando login
  e 403 para administrador de academia;
- CRUD de unidade, configurações, desativação e invisibilidade cross-tenant;
- criação de usuário com papéis/unidades, troca de papéis, vínculo/desvínculo,
  rejeição de unidade inativa e de vínculo cross-tenant, usuário inativo sem login;
- RLS real com o role de runtime `gymsaas_app` (`NOSUPERUSER`, `NOBYPASSRLS`) em
  9 tabelas.

## 8. Validação da stack e smoke test HTTP

- `docker compose up -d --build`: PostgreSQL, backend e frontend construídos;
  PostgreSQL e backend `healthy`, frontend `Up`.
- `GET /actuator/health` → `200 {"status":"UP"}`.
- Frontend: `/login`, `/admin/tenants`, `/academy/branches`, `/academy/users` e
  `/academy/settings` → `200`; build de produção gerou 11 rotas, incluindo
  `/admin/tenants/[tenantId]` dinâmica.
- Smoke test real em `/tmp/opencode/smoke.sh` contra a stack Docker:
  **62 checks, 0 falhas**, cobrindo:
  - login do superadmin, ausência de tenant ativo e listagem paginada;
  - criação, conflito de documento, edição, ativação e suspensão de academia;
  - rejeição de status inválido e 403 para administrador de academia;
  - criação de unidade, código único, edição, configurações e desativação;
  - papéis, criação de usuário com papel/unidade, busca, troca de papéis e vínculos;
  - rejeição de unidade inexistente e login de usuário inativo;
  - leitura/atualização da academia ativa com documento preservado.

Observação: o smoke test cria dados de teste no volume Docker local (academia,
unidade e usuário com prefixo `smoke`); isso não afeta o código nem os testes
automatizados, que usam Testcontainers isolados.

## 9. Riscos e pendências

- **Testes de frontend**: há build de produção e smoke HTTP, mas não há suíte
  automatizada de componentes/E2E. Recomenda-se Playwright para os fluxos críticos.
- **Cache de revogação de access token** em memória: adequado a instância única;
  múltiplas instâncias exigem Redis ou tabela compartilhada.
- **Chaves JWT**: se não configuradas, são efêmeras; produção exige rotação e gestão
  de segredos.
- **Seed demo**: deve permanecer desabilitado em produção (`DEMO_DATA_ENABLED=false`).
- **Validação de documento**: valida formato (11/14 dígitos) e unicidade, não os
  dígitos verificadores de CPF/CNPJ.
- **Concessões do administrador da plataforma**: derivadas no momento da emissão do
  token; mudanças de papel só refletem após novo login/refresh.
- **Detalhe da academia** é somente leitura; edição ocorre na listagem. A academia
  ativa é editável em `/academy/settings`.
- **Fora do escopo e não iniciados**: Fase 4 (alunos, planos, matrículas, pagamentos,
  frequência), integração física Control iD, endpoints de dispositivos, microsserviços,
  CI/CD e observabilidade distribuída.

## 10. Checklist de aceite da Fase 3

- [x] CRUD de academias com `PENDING`/`ACTIVE`/`SUSPENDED`/`INACTIVE`
- [x] Administração global restrita ao administrador da plataforma (403 para os demais)
- [x] CRUD de unidades, status e `branch_settings`
- [x] Criação/edição de usuários, papéis e vínculos com unidades
- [x] Permissões plurais criadas e concedidas por papel
- [x] `tenant_id` nunca aceito do cliente; isolamento por JWT, RLS e FKs compostas
- [x] Bloqueio de login para academia suspensa/inativa e usuário inativo
- [x] Rejeição de vínculo com unidade inativa ou de outro tenant
- [x] Migrations aditivas V6/V7; nenhuma migration aplicada editada
- [x] Backend compila e `./mvnw verify` passa (21 unit + 16 IT)
- [x] Frontend compila (`npm run build`, 11 rotas)
- [x] Stack Docker sobe e backend fica `healthy`
- [x] Smoke HTTP ponta a ponta: 62 checks, 0 falhas
- [x] README e relatório final atualizados
- [ ] Fase 4 — **não iniciada**, aguardando aprovação explícita

## 11. Conclusão

A Fase 3 está implementada e validada de ponta a ponta: banco (migrations + RLS +
constraints), backend (módulos, RBAC e endpoints), frontend (painéis MVVM) e stack
Docker. As evidências são comandos reais executados nesta sessão — suíte Maven
completa, build de produção do Next.js, containers saudáveis e smoke HTTP com
62 verificações sem falha. Nenhuma funcionalidade da Fase 2 foi removida. A Fase 4
permanece bloqueada até aprovação.
