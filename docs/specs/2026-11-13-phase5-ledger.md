# Ledger — Fase 5 (Planos, Matrículas e Contratos)

Plano: `docs/specs/2026-11-13-phase5-plan.md`. Workspace sem git: registro manual.
Escopo desta entrega: **backend** (frontend adiado a pedido do usuário em 2026-11-13).

## Execução

- Task 0 — baseline: `mvnw verify` BUILD SUCCESS (61 unit / 21 IT) porém sem recompilar (JRE sem `javac`); `npm run build` OK (13 rotas). Banco do Compose em V7.
- Task 1 — decisões D1–D5 aprovadas pelo usuário (vigência, congelamento, renovação, PDF).
- Task 2 — migrations V10/V11/V12 criadas e validadas em PostgreSQL 16 descartável (aplicação V1–V12 + smoke de constraints: sobreposição de preço/matrícula, imutabilidade, FK cross-tenant, desconto, append-only).
- Task 3 — base: `Money`, `Sha256`, `TenantCalendarRepository`/`TenantCalendar`, bean `Clock`.
- Task 4 — módulo `plan` (domínio, casos de uso, JPA, `PlanController`).
- Task 5 — módulo `membership` (domínio com `MembershipTerm`, casos de uso, JPA, gerador de número, `MembershipLifecycleScheduler`, `MembershipController`). Porta `StudentRepository.findAllByIds` adicionada (aditivo).
- Task 6 — módulo `contract` (domínio, casos de uso, JPA, `FileSystemContractDocumentStorage`, `OpenPdfContractDocumentRenderer`, `ContractController`); dependência OpenPDF 2.0.2.
- Task 7 — testes unitários (51 novos) e de integração (19 novos); correções em `StudentIsolationIT` (asserção RLS vazia) e `RlsIsolationIT` (12 migrations / 26 tabelas).
- Task 8 — validação. Ajustes durante a validação:
  1. Conflito de nome de bean `membershipRepositoryAdapter` com `tenant.infrastructure.persistence.MembershipRepositoryAdapter` → classe renomeada para `StudentMembershipRepositoryAdapter`.
  2. `tenants.timezone` não existia (erro do diagnóstico) → coluna aditiva adicionada no início da V10 (V10 nunca aplicada em banco persistente).
  3. Teste de formatação monetária ajustado para o espaço não separável (U+00A0) do `NumberFormat` pt-BR.
- Task 9 — Docker: volume `gymsaas_contract_storage`, `CONTRACT_STORAGE_DIR`, diretório gravável no Dockerfile; `docker compose config` OK.
- Resultado final: `mvnw verify` → BUILD SUCCESS, 112 unit + 40 IT, 0 falhas.
