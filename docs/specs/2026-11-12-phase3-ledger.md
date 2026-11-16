# Ledger — Fase 3

Plan: `docs/specs/2026-11-12-phase3-plan.md`
Workspace is not a git repository, so this file replaces the SDD ledger and
commit records. Every task is recorded here with the command and result used
as evidence.

## Tasks

- [x] T1 Migrations aditivas V6/V7
- [x] T2 Contexto de requisição, JWT `plat` e RLS
- [x] T3 Administração global de academias
- [x] T4 Unidades, branch_settings e vínculos
- [x] T5 Gestão de usuários paginada
- [x] T6 Compilação do backend (`mvnw -DskipTests compile` → BUILD SUCCESS)
- [x] T7 `mvnw verify` da suíte existente
- [x] T8 Novos testes unitários e de integração
- [x] T9 Frontend: módulos tenants/branches/users
- [x] T10 `npm run build`
- [x] T11 Smoke test HTTP na stack Docker
- [x] T12 README + relatório final

## Evidence

| Task | Command | Result |
|------|---------|--------|
| T6 | `./mvnw -DskipTests compile` | BUILD SUCCESS |
| T7/T8 | `./mvnw verify` | 21 unit + 16 IT, BUILD SUCCESS |
| T10 | `npm run build` | Compiled successfully, 11 rotas geradas (inclui `/admin/tenants/[tenantId]` e `/academy/settings`) |
| T11 | `docker compose up -d --build` + `/tmp/opencode/smoke.sh` | 3 containers; **62 checks, 0 falhas** |
| T11 | `GET /login`, `/admin/tenants`, `/academy/branches`, `/academy/users`, `/academy/settings` | 200 |
| T12 | `docs/specs/2026-11-12-phase3-final-report.md` | relatório com diagnóstico, lacunas, arquivos, migrations, endpoints, funcionalidades, testes, riscos e checklist |

### Targeted suites

- `BrDocumentsTest`: 5 testes de normalização de documento/telefone/CEP, código de
  unidade e parsing de status.
- `TenantAdminIT`: CRUD global, conflito de documento, transições de status,
  suspensão bloqueia login, admin de academia recebe 403.
- `BranchManagementIT`: CRUD, configurações, desativação e invisibilidade de unidade
  de outro tenant.
- `UserBranchAccessIT`: criação com papéis/unidades, substituição de papéis, vínculo
  de unidade, rejeição de unidade inativa, remoção/adição de vínculo, usuário inativo
  não loga e FK composta bloqueia vínculo cross-tenant.

## Rulings

- Sem git: não é possível worktree/commits. Uso este ledger em `docs/specs/` e
  validação por comandos reais. Custo se errado: nenhum, apenas rastreabilidade.
- `SAAS_ADMIN` permanece como papel de plataforma (não renomear) e o flag
  `is_platform_admin`/claim `plat` identifica o administrador global.
- Filtros JPQL paginados usam sentinela de string vazia (`:status = ''`,
  `:search = ''`) porque o PostgreSQL interpreta `null` sem tipo como `bytea` em
  `lower(?)`. Custo se errado: filtros vazios retornariam erro 500.
- `POST /api/users` mantém HTTP 200 para preservar o contrato do `RbacIT` da Fase 2.
- O dashboard usa a listagem paginada de usuários e mostra apenas os 5 primeiros,
  com link para a tela completa.
- Frontend: `platformAdmin` propagado por `SessionResponse`/`MeResponse`, login
  redireciona o administrador global sem academia para `/admin/tenants`, e
  `RequireAuth` ganhou `requirePlatformAdmin`.
