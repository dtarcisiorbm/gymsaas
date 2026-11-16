# Fase 2 — Relatório de Execução

> Data: 2026-11-12 · Escopo executado: fundação de autenticação, multi-tenancy,
> RBAC e auditoria (backend + frontend + banco + Docker). Fase 3 **não iniciada**.

## 1. O que foi implementado

### Backend (Spring Boot 3.5.16, Java 21, Maven)
- Monólito modular por feature (`identity`, `tenant`, `rbac`, `audit`, `shared`) com
  separação `domain / application / infrastructure / presentation`.
- Fronteiras verificadas por ArchUnit (`ArchitectureBoundaryTest`): domínio sem
  Spring/JPA/Jackson; aplicação sem apresentação; controllers apenas em `presentation`.
- Autenticação:
  - login com e-mail/senha (Argon2id), emissão de access token JWT RS256 (~15 min)
    e refresh token opaco de 256 bits;
  - refresh com **rotação a cada uso**, detecção de reuso (revoga a família inteira) e
    revogação imediata do access token via cache de sessões revogadas;
  - logout revogando família + limpeza do cookie;
  - `GET /api/auth/me`, `GET /api/auth/tenants`, `POST /api/auth/select-tenant`.
- Refresh token persistido apenas como SHA-256, entregue em cookie `HttpOnly`,
  `SameSite=Lax`, `Path=/api/auth`; access token nunca vai para storage do browser.
- RBAC: papéis de sistema + papéis por academia, permissões granulares, validação de
  que os papéis atribuídos pertencem à academia, `@PreAuthorize` por permissão.
- Auditoria append-only: login, logout, reuso de refresh, criação de usuário,
  alteração de papéis/status, seleção de academia.
- Multi-tenancy: tenant exclusivamente do claim `tid` do JWT; `TenantContext`
  preenchido por filtro após a autenticação; `RlsAwareDataSource` injeta
  `app.current_tenant`, `app.current_user` e `app.rls_bypass` em cada conexão.
- Actuator (`health`), springdoc/OpenAPI, tratamento global de erros sem stack trace.
- Seed de demonstração idempotente (`DEMO_DATA_ENABLED=true`).

### Banco (PostgreSQL 16 + Flyway)
- `V1` extensões, `uuid_generate_v7()`, helpers de RLS e `updated_at`.
- `V2` tenants, users, roles, permissions, vínculos e trigger de escopo de papel.
- `V3` refresh tokens + audit logs append-only.
- `V4` RLS habilitada em 6 tabelas com políticas `USING`/`WITH CHECK`.
- `V5` seed de papéis/permissões de sistema.
- Duas credenciais: `gymsaas_owner` (Flyway) e `gymsaas_app` (runtime,
  `NOSUPERUSER NOBYPASSRLS`). Nenhuma migration aplicada foi editada.

### Frontend (Next.js 14.2, TypeScript, Tailwind, MVVM)
- Páginas `/login`, `/select-tenant`, `/dashboard` e redirecionamento em `/`.
- `SessionManager` + `TokenStore` (access token só em memória), bootstrap via refresh
  cookie, renovação proativa e renovação single-flight no 401 (evita rotação paralela
  interpretada como reuso).
- `ApiClient` com `credentials: include`, tratamento padronizado de erros e logout.
- Proteção de rotas (`RequireAuth`), troca de academia, estados de carregamento/erro.
- ViewModels (`Login`, `SelectTenant`, `Dashboard`) separados das views.

### Infra
- `docker-compose.yml` com PostgreSQL, backend e frontend; healthchecks e init script
  que cria o role de runtime.
- `backend/Dockerfile` multi-stage (build Maven + JRE Alpine) e `frontend/Dockerfile`
  multi-stage (standalone).
- `README.md` com subida da stack, endpoints, credenciais demo e notas de segurança.

## 2. Validação executada (resultados reais)

| Validação | Comando | Resultado |
|-----------|---------|-----------|
| Build backend + testes unitários | `./mvnw verify` | **25 testes, 0 falhas** (15 unit + 10 IT) |
| Fronteiras arquiteturais | ArchUnit | 5/5 regras |
| Build frontend | `npm run build` | sucesso, 5 rotas |
| Stack Docker | `docker compose up -d --build` | 3 containers `Up`, backend `healthy` |
| Migrations | `flyway_schema_history` | V1–V5, todas `success=t` |
| RLS habilitada | `pg_class.relrowsecurity` | 6 tabelas |
| Roles de banco | `pg_roles` | `gymsaas_app`: `rolsuper=f`, `rolbypassrls=f` |
| Health | `GET /actuator/health` | 200 `UP` |
| Frontend | `GET /login` | 200 |
| Login + endpoints | `admin@gymsaas.local` | 200 em `/me`, `/tenants/current`, `/users`, `/roles`, `/permissions`, `/audit-logs` |
| RBAC | `reception@gymsaas.local` | `/users` 200; `/roles` 403; `/audit-logs` 403 |
| Rotação de refresh | `POST /api/auth/refresh` | 200 e novo token |
| Reuso de refresh | replay do token rotacionado | 401 `INVALID_REFRESH` |
| Revogação da família | token seguinte após reuso | 401 |
| Logout | `POST /api/auth/logout` | 204; `/me` e `/refresh` passam a 401 |
| RLS cross-tenant | `psql` como `gymsaas_app` | 1 tenant visível; insert em outro tenant rejeitado pela política |
| Auditoria | `GET /api/audit-logs` | `LOGIN`, `LOGOUT`, `REFRESH_REUSE_DETECTED` (CRITICAL) registrados |

Testes de integração (Testcontainers PostgreSQL 16, role de runtime real):
`AuthFlowIT` (4), `RbacIT` (3), `RlsIsolationIT` (3).

## 3. Problemas encontrados e correções

1. **Bean duplicado de `DataSourceProperties`** impedia o boot do contexto de teste.
   Removido o bean manual; usa-se o auto-configurado pelo Spring Boot.
2. **Container PostgreSQL encerrado entre classes de IT** — trocado para o padrão
   singleton (container iniciado uma vez para todo o JVM de testes).
3. **`@AuthenticationPrincipal JwtAuthenticationToken` resolvia `null`** — o principal
   padrão do resource server é o `Jwt`; controllers passaram a receber `Jwt`.
4. **`TenantContextFilter` rodava antes da autenticação bearer** — registrado antes do
   `AuthorizationFilter`, garantindo o contexto de tenant em toda requisição autenticada.
5. **Revogação por reuso era desfeita pelo rollback** — `refresh` anotado com
   `noRollbackFor = ApiException.class`, preservando a revogação da família e a auditoria.
6. **JDK ausente na máquina (apenas JRE)** — validação feita com JDK 21 portátil em
   `/tmp/opencode/tools/jdk21` e repositório Maven isolado (o `~/.m2` local tem diretórios
   de root). Isso é ambiente de execução, não mudança no projeto.

## 4. Pendências / não implementado nesta fase

- Gateway local e integração Control iD (Fase 3+).
- CRUD completo de academia, alunos, planos, matrículas, pagamentos e frequência.
- MFA, recuperação de senha, convite por e-mail.
- Observabilidade distribuída (métricas/tracing), CI/CD e deploy.
- Testes de frontend (unit/E2E) — build e fluxo manual validados, sem suíte automatizada.
- Cache de revogação de access token é em memória (adequado a instância única; para
  múltiplas instâncias exigirá Redis ou tabela compartilhada).
- Rotação de chaves JWT e gestão de segredos em produção (hoje chaves efêmeras se não
  configuradas).

## 5. Checklist de aceite da Fase 2

- [x] Backend compila e `./mvnw verify` passa (unit + integração)
- [x] Frontend compila (`npm run build`)
- [x] Docker Compose sobe a stack completa e backend fica `healthy`
- [x] Migrations Flyway V1–V5 aplicadas e validadas
- [x] Login, refresh com rotação, detecção de reuso, logout e `/me` funcionando
- [x] RBAC por permissão com 403 para acesso indevido
- [x] Isolamento multi-tenant comprovado no banco (RLS) e na aplicação
- [x] Refresh token nunca em texto puro; cookie `HttpOnly`
- [x] Auditoria de eventos sensíveis
- [x] Frontend com login, seleção de academia, renovação e logout
- [ ] Aprovação para iniciar a Fase 3
