# Relatório Final — Fase 5: Planos, Matrículas e Contratos (backend)

Data: 2026-11-13. Escopo entregue: **backend completo**. Frontend (`plans`, `memberships`, `contracts`) **adiado a pedido do usuário**.

## 1. Diagnóstico inicial
Ver `2026-11-13-phase5-plan.md` §1. Fases 2–4 implementadas e validadas; planos/matrículas/contratos inexistentes; migrations V1–V9 (Compose local em V7).

## 2. Pendências das fases anteriores (tratadas)
- `StudentIsolationIT`: asserção de RLS vazia (GUCs inexistentes em CTE não referenciada) → corrigida; agora verifica 0 linhas no tenant A **e** 1 no tenant B.
- Ambiente: JDK do sistema é só JRE; `~/.m2` com 74 entradas de `root`. Contornados sem alterar o sistema (JDK Temurin extraído da imagem local `maven:3.9-eclipse-temurin-21`; repositório Maven alternativo com `maven.repo.local.tail`). Recomenda-se `sudo apt install openjdk-21-jdk-headless` e `sudo chown -R $USER: ~/.m2`.
- `tenants` não tinha `timezone` → adicionada (aditiva) na V10.

## 3. Arquivos criados
- Migrations: `V10__plans_prices_features.sql`, `V11__memberships_freezes_history.sql`, `V12__membership_contracts.sql`.
- `shared/domain`: `Money`, `Sha256`; `shared/infrastructure/time/TimeConfig`.
- `tenant`: `domain/TenantCalendarRepository`, `application/TenantCalendar`, `infrastructure/persistence/TenantCalendarJdbcRepository`.
- `plan/**` (domínio: `Plan`, `PlanPrice`, `PlanFeature`, `DurationType`, `BillingPeriod`, `FreezePolicy`, status/tipos, portas; aplicação: DTOs, guard, assembler, loader, factory, 9 casos de uso; JPA: 3 entidades/repos/adapters; `PlanController`).
- `membership/**` (domínio: `Membership`, `ContractedConditions`, `MembershipTerm`, `MembershipFreeze`, `MembershipEvent`, status/eventos, portas; aplicação: DTOs, guard, pricing, draft factory, journal, assembler, loader, 13 casos de uso; JPA: 3 entidades/repos/adapters + `PostgresMembershipNumberGenerator`; `MembershipLifecycleScheduler`; `MembershipController`).
- `contract/**` (domínio: `MembershipContract`, `ContractAcceptance`, portas de storage/renderer; aplicação: DTOs, content factory, guard, assembler, 5 casos de uso; JPA; `FileSystemContractDocumentStorage`; `OpenPdfContractDocumentRenderer`; `ContractController`).
- Testes: `MembershipTermTest`, `MembershipRulesTest`, `MoneyTest`, `TenantCalendarTest`, `MembershipPricingTest`, `FreezeMembershipUseCaseTest`, `AddPlanPriceUseCaseTest`, `ContractContentFactoryTest`, `PlanManagementIT`, `MembershipManagementIT`, `MembershipLifecycleIT`, `MembershipIsolationIT`, `MembershipConcurrencyIT`, `ContractIT`, `support/Phase5IntegrationSupport`.
- Docs: plano, ledger e este relatório.

## 4. Arquivos modificados
`pom.xml` (OpenPDF 2.0.2), `application.yml` (storage e job), `Dockerfile`, `docker-compose.yml` (volume), `backend/.gitignore`, `StudentRepository` + `StudentRepositoryAdapter` + `StudentJpaRepository` (`findAllByIds`, aditivo), `AbstractIntegrationTest` (diretório temporário de PDFs, job desligado), `RlsIsolationIT`, `StudentIsolationIT`, `README.md`.

## 5. Migrations
- **V10**: `tenants.timezone`; `plans`; `plan_prices` (imutável por trigger, `EXCLUDE` sem sobreposição de vigência, `btree_gist`); `plan_features` (CHECK de forma por tipo); RLS; permissões `plans:*`.
- **V11**: `membership_number_counters`; `student_memberships` (snapshot imutável por trigger, FKs compostas inclusive `(tenant, plan, price)`, `EXCLUDE` contra sobreposição do mesmo plano, CHECKs de desconto/cancelamento/datas); `membership_freezes`; `membership_status_history` (append-only); RLS; permissões `memberships:*`.
- **V12**: `membership_contracts` (conteúdo/hash imutáveis, um contrato corrente por matrícula); `contract_acceptances` (append-only, um por contrato); RLS; permissões `contracts:*`.

## 6. Regras de negócio
- **Vigência (D1/D2)**: `fim = âncora + meses×sequência − 1 dia` (`plusMonths` ajusta ao último dia). 31/01 mensal → 27/02; 29/02/2028 anual → 27/02/2029; `CUSTOM_DAYS`: início + dias − 1. Renovação contínua mantém a âncora (31/01→27/02, 28/02→30/03, 31/03→29/04…); gap, congelamento que deslocou o fim ou mudança de duração → nova âncora.
- Duração do contrato (`plans.duration_*`) ≠ frequência de cobrança (`plan_prices.billing_*`); cobrança não pode exceder a duração; planos em dias só aceitam `ONE_TIME`.
- **Preço**: histórico imutável; novo preço nunca retroativo, encerra o anterior no dia anterior; preço programado pode ser cancelado (reabre o anterior). Matrícula grava snapshot (valor, taxa, moeda, periodicidade, desconto, política de congelamento, benefícios, cláusulas) e o banco impede alterá-lo.
- **Desconto**: exige `memberships:apply_discount`, ≤ valor, motivo obrigatório, no máximo 2 casas.
- **Status**: `PENDING` (início futuro) → `ACTIVE` → `FROZEN`/`SUSPENDED` → `EXPIRED`/`CANCELLED` (terminais). Aluno `BLOCKED`/`ARCHIVED`, plano não ativo e unidade inativa bloqueiam contratação.
- **Congelamento (D3)**: começa hoje (fuso do tenant), estende o fim pelos dias planejados; descongelamento antecipado devolve os dias não usados; limites do snapshot (máx. congelamentos, máx. dias, mín. por congelamento); bloqueado se já houver renovação.
- **Suspensão**: administrativa, não estende a vigência.
- **Renovação (D4)**: nova matrícula vinculada, preço vigente na data de início, desconto não herdado, uma renovação viva por matrícula.
- **Cancelamento**: motivo, data e responsável obrigatórios; fecha congelamento ativo; sem efeito financeiro.
- **Ciclo de vida**: job horário por tenant (contexto RLS do tenant, sem bypass), idempotente.
- **Contrato (D5)**: texto gerado do snapshot, sem cláusulas inventadas; SHA-256 do conteúdo; PDF em disco/volume (chave validada, sem path traversal, sem sobrescrita), hash verificado no download; nova versão substitui a não aceita; contrato aceito não é regenerado.
- **Aceite**: presencial pelo operador, exige hash do conteúdo exibido e confirmação; registra nome, CPF mascarado, IP, user-agent, horário e operador; declarado como **aceite simples, não assinatura digital qualificada**.

## 7. Endpoints
| Método | Endpoint | Permissão |
|---|---|---|
| GET · POST | `/api/plans` | `plans:read`\|`plan:read` · `plans:create`\|`plan:manage` |
| GET · PUT | `/api/plans/{id}` | read · `plans:update`\|`plan:manage` |
| PATCH | `/api/plans/{id}/status` | `plans:update`\|`plan:manage` |
| GET · POST | `/api/plans/{id}/prices` | read · `plans:manage_prices`\|`plan:manage` |
| POST | `/api/plans/{id}/prices/{priceId}/cancel` | `plans:manage_prices`\|`plan:manage` |
| PUT | `/api/plans/{id}/features` | `plans:update`\|`plan:manage` |
| GET · POST | `/api/memberships` | `memberships:read` · `memberships:create` |
| POST | `/api/memberships/quote` | `memberships:create` |
| GET · PUT | `/api/memberships/{id}` | `memberships:read` · `memberships:update` |
| POST | `/api/memberships/{id}/renew` | `memberships:renew` |
| POST | `/api/memberships/{id}/cancel` | `memberships:cancel` |
| POST | `/api/memberships/{id}/freeze` · `/unfreeze` | `memberships:freeze` |
| POST | `/api/memberships/{id}/suspend` · `/reactivate` | `memberships:suspend` |
| GET | `/api/memberships/{id}/history` | `memberships:read` |
| GET · POST | `/api/memberships/{id}/contracts` | `contracts:read` · `contracts:generate` |
| GET | `/api/contracts/{id}` · `/api/contracts/{id}/document` | `contracts:read` |
| POST | `/api/contracts/{id}/acceptance` | `contracts:register_acceptance` |

Concessões: TENANT_ADMIN e BRANCH_MANAGER têm `memberships:*`/`contracts:*` (BRANCH_MANAGER só `plans:read`); RECEPTIONIST: `plans:read`, `memberships:read/create/update/renew/freeze`, `contracts:*`; TRAINER: `plans:read`, `memberships:read`. DTOs com validação, paginação, erros padronizados (`ErrorResponse`) e OpenAPI (`@Tag`/`@Operation`). Dinheiro serializado como string decimal.

## 8. Testes executados e resultados (reais)
- Comando: `JAVA_HOME=<Temurin 21> ./mvnw -B -Dmaven.repo.local=<scratch>/m2 -Dmaven.repo.local.tail=$HOME/.m2/repository verify`
- **BUILD SUCCESS** — unitários **112/112**, integração **40/40** (inclui as 21 ITs anteriores). Flyway aplicou V1–V12 em PostgreSQL 16 real (Testcontainers) com role `NOSUPERUSER NOBYPASSRLS`.
- Cobertura dos cenários pedidos: planos, preços, histórico, vigência (mensal/trimestral/semestral/anual/custom, dia 31, fevereiro, bissexto, renovação sem deriva, congelamento), matrículas, renovação, cancelamento, congelamento, contratos, aceite, isolamento multi-tenant (API + RLS real não vazio + FK composta), constraints (imutabilidade, sobreposição, append-only), concorrência (matrículas, congelamentos e aceites simultâneos), permissões (403).

## 9. Pendências
- ~~Frontend da Fase 5~~ — concluído em 2026-11-17 (§11).
- ~~Smoke no Docker Compose~~ — executado em 2026-11-17 (§11).
- Edição de `tenants.timezone` pela API de academia não foi exposta (usa o default `America/Sao_Paulo`); fuso por unidade (`branch_settings.timezone`) não é usado.
- Arquivos PDF órfãos em caso de rollback após gravação (raro): rotina de limpeza não implementada.
- Storage S3/MinIO: porta pronta, adapter não implementado.
- Nomes simples repetidos (`Membership`/`MembershipRepository` em `tenant.domain` e `membership.domain`) — sem conflito técnico, mas convém revisar a nomenclatura.
- Validação jurídica do conteúdo do contrato (CPF mascarado, cláusulas) pela academia.

## 10. Checklist de aceite (backend)
- [x] Inspeção, diagnóstico e decisões aprovadas antes de implementar
- [x] Bloqueio de isolamento em teste corrigido (`StudentIsolationIT`)
- [x] Planos, preços com histórico, benefícios tipados
- [x] Matrículas com preço calculado no backend e snapshot imutável
- [x] Renovação, cancelamento, congelamento, reativação, histórico
- [x] Contratos versionados com hash, PDF fora do banco, aceite simples com evidências
- [x] TenantContext, RBAC, RLS, FKs compostas, auditoria
- [x] Migrations novas sem alterar aplicadas
- [x] Testes unitários e de integração executados e verdes
- [x] Frontend (2026-11-17 — ver §11)
- [x] Smoke Docker Compose (2026-11-17 — ver §11)

## 11. Complemento 2026-11-17 — frontend e smoke

- Frontend MVVM: `features/plans`, `features/memberships`, `features/contracts` (model/data/viewmodel) e rotas
  `/academy/plans`, `/academy/plans/[id]`, `/academy/memberships`, `/academy/memberships/new`,
  `/academy/memberships/[id]`, `/academy/contracts/[id]`; itens "Planos" e "Matrículas" no menu; atalhos na
  página do aluno. Dinheiro trafega como string decimal; datas `LocalDate` formatadas sem conversão de fuso.
- Aceite: o navegador calcula SHA-256 (UTF-8) do conteúdo exibido e envia esse hash; se divergir do servidor, o
  aceite é bloqueado. Sem HTTPS (SubtleCrypto indisponível) usa o hash do servidor e avisa na tela.
- Correção transversal: `AuthRepository.bootstrap()` passou a carregar `/api/auth/me` após restaurar a sessão;
  antes, um recarregamento de página deixava o menu e as ações dependentes de permissão vazios.
- Verificação: `tsc --noEmit` e `next build` sem erros; `docker compose up -d --build` com backend saudável e
  Flyway em V12 (V8–V12 já estavam aplicadas desde 2026-11-15); smoke E2E via API — 28/28 verificações
  (plano, preços, benefícios, cotação, matrícula, congelamento, contrato, PDF com hash, aceite, rejeição de hash
  inválido, renovação, cancelamento, 403 da recepção, 6 rotas do frontend). Verificação em navegador: menu,
  ações por permissão após reload, detalhe da matrícula e verificação local do hash do contrato. O fluxo de
  cotação/criação pela UI não foi exercitado no navegador (coberto apenas pela API).
- Dados de teste do smoke ficaram no banco local de demonstração (plano `SMOKE-*`, aluno "Aluno Smoke *").

**Não avançar para a Fase 6 sem aprovação.**
