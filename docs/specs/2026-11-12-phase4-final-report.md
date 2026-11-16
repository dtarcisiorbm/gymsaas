# Relatório Final — Fase 4: Gestão de Alunos

Data: 2026-11-12  
Workspace: `/home/tarcisio/Documents/sistema de academia` (sem repositório git; alterações não versionadas em disco)

## 1. Diagnóstico inicial

- Stack e estrutura conforme Fases 1–3: backend Spring Boot 3.5/Java 21/Maven, PostgreSQL 16, Flyway, Spring Security/JWT, JPA; frontend Next.js 14 App Router + TypeScript + Tailwind; Docker Compose (frontend 3000, backend 8080, postgres 5432).
- Baseline executado antes de alterações:
  - Backend: `JAVA_HOME=/tmp/opencode/tools/jdk21 ./mvnw -Dmaven.repo.local=/tmp/opencode/m2repo verify` → **BUILD SUCCESS** (16 testes de integração, unitários verdes).
  - Frontend: `npm run build` → **sucesso** (11 rotas).
- Última migration aplicada: **V7**. Módulo `student` inexistente no backend/frontend. Isolamento multi-tenant/RLS (role runtime `gymsaas_app` NOSUPERUSER/NOBYPASSRLS) e RBAC validados pelas fases anteriores e pelo baseline.
- Lacuna identificada: frontend era apenas dark (`color-scheme: dark`, paleta fixa), exigindo implementação de tema claro/escuro.

## 2. Pendências das fases anteriores

- **Nenhuma pendência bloqueante identificada.** Fase 3 validada (migrations V1–V7, RLS, permissões plurais/singulares, CRUD tenants/branches/users). O teste `RlsIsolationIT` foi atualizado para refletir as novas migrations/tabelas com RLS (V8/V9), mantendo a garantia de isolamento.

## 3. Arquivos criados

### Backend — migrations e domínio/aplicação/infra/apresentação
- `backend/src/main/resources/db/migration/V8__students_core.sql`
- `backend/src/main/resources/db/migration/V9__student_branches_history_notes_permissions.sql`
- `backend/src/main/java/com/gymsaas/student/domain/` — `Student`, `StudentStatus`, `StudentDocument`, `StudentContact`, `StudentAddress`, `StudentBranchAssignment`, `StudentStatusEntry`, `StudentNote`, `StudentHistoryEntry`, `StudentSearchCriteria`, `StudentRegistrationGenerator`, portas `StudentRepository`, `StudentDocumentRepository`, `StudentContactRepository`, `StudentAddressRepository`, `StudentBranchAssignmentRepository`, `StudentStatusHistoryRepository`, `StudentNoteRepository`.
- `backend/src/main/java/com/gymsaas/student/application/` — `StudentDtos`, `StudentGuard`, `StudentAssembler`, e casos de uso: `CreateStudentUseCase`, `UpdateStudentUseCase`, `FindStudentByIdUseCase`, `ListStudentsUseCase`, `ChangeStudentStatusUseCase`, `AddStudentDocumentUseCase`, `UpdateStudentDocumentUseCase`, `RemoveStudentDocumentUseCase`, `AddStudentContactUseCase`, `UpdateStudentContactUseCase`, `RemoveStudentContactUseCase`, `UpsertStudentAddressUseCase`, `GetStudentAddressUseCase`, `AssignStudentToBranchUseCase`, `TransferStudentBranchUseCase`, `RemoveStudentBranchAssignmentUseCase`, `GetStudentHistoryUseCase`, `AddStudentNoteUseCase`, `ListStudentNotesUseCase`.
- `backend/src/main/java/com/gymsaas/student/infrastructure/persistence/` — entidades JPA, repositórios Spring Data e adaptadores para `students`, `student_documents`, `student_contacts`, `student_addresses`, `student_branch_assignments`, `student_status_history`, `student_notes`; `PostgresStudentRegistrationGenerator`.
- `backend/src/main/java/com/gymsaas/student/presentation/StudentController.java`

### Backend — testes
- `backend/src/test/java/com/gymsaas/student/domain/StudentRegistrationTest.java`
- `backend/src/test/java/com/gymsaas/student/domain/StudentDomainTest.java`
- `backend/src/test/java/com/gymsaas/student/application/StudentCoreUseCasesTest.java`
- `backend/src/test/java/com/gymsaas/student/application/StudentProfileUseCasesTest.java`
- `backend/src/test/java/com/gymsaas/student/application/StudentJourneyUseCasesTest.java`
- `backend/src/test/java/com/gymsaas/student/StudentManagementIT.java`
- `backend/src/test/java/com/gymsaas/student/StudentIsolationIT.java`
- `backend/src/test/java/com/gymsaas/student/StudentConcurrencyIT.java`

### Frontend
- `frontend/src/features/students/model/types.ts`
- `frontend/src/features/students/data/StudentApi.ts`
- `frontend/src/features/students/viewmodel/StudentsViewModel.ts`
- `frontend/src/features/students/viewmodel/StudentFormViewModel.ts`
- `frontend/src/features/students/viewmodel/StudentDetailViewModel.ts`
- `frontend/src/app/academy/students/page.tsx`
- `frontend/src/app/academy/students/new/page.tsx`
- `frontend/src/app/academy/students/[id]/page.tsx`
- `frontend/src/app/academy/students/[id]/edit/page.tsx`
- `frontend/src/core/theme/ThemeProvider.tsx`
- `frontend/src/components/layout/ThemeToggle.tsx`

### Documentação
- `docs/specs/2026-11-12-phase4-plan.md`
- `docs/specs/2026-11-12-phase4-ledger.md`
- `docs/specs/2026-11-12-phase4-final-report.md`
- `docs/security/06-student-data-retention.md`

## 4. Arquivos modificados

- `backend/src/main/java/com/gymsaas/shared/domain/BrDocuments.java` — validação/normalização/máscara de CPF.
- `backend/src/main/java/com/gymsaas/shared/infrastructure/web/GlobalExceptionHandler.java` — tratamento de `ObjectOptimisticLockingFailureException`/`OptimisticLockingFailureException` → `409 CONCURRENT_UPDATE`.
- `backend/src/test/java/com/gymsaas/tenant/RlsIsolationIT.java` — ajuste para 9 migrations e 17 tabelas tenant-scoped com RLS.
- `frontend/src/app/globals.css` — variáveis de tema claro/escuro e overrides de compatibilidade.
- `frontend/src/app/layout.tsx` — `ThemeProvider` e script de inicialização de tema (evita flash).
- `frontend/tailwind.config.js` — cores via CSS variables (`surface`, `accent`).
- `frontend/src/components/layout/AppHeader.tsx` — inclusão do `ThemeToggle`.
- `frontend/src/components/layout/AppNav.tsx` — item "Alunos" (`students:read`/`student:read`).
- `frontend/src/core/mvvm/ViewModel.ts` — getter protegido `state` para uso nos ViewModels (compatível com MVVM existente).
- `README.md` — Fase 4, endpoints, migrations V8/V9, frontend/tema.

## 5. Migrations

- **V8__students_core.sql** (nova): `students` (campos do prompt, `version`, `deleted_at`, checks de status/nascimento, unicidade `tenant_id+registration_number`, índices), `student_registration_counters` (tenant/ano), `student_documents` (CPF normalizado, unicidade parcial de CPF por tenant, principal por tipo), `student_contacts`, `student_addresses` (1:1), triggers `set_updated_at`, RLS e políticas de isolamento em todas.
- **V9__student_branches_history_notes_permissions.sql** (nova): `student_branch_assignments` (FKs compostas para students/branches, principal ativo único, vínculo ativo único por unidade, encerramento por `ended_at`), `student_status_history` (append-only por convenção), `student_notes`, RLS, permissões `students:*` e concessões por papel (SAAS_ADMIN, TENANT_ADMIN, BRANCH_MANAGER, RECEPTIONIST, TRAINER).
- Migrations V1–V7 **não foram alteradas**. Flyway validou/aplicou V1–V9 com sucesso nos testes.

## 6. Entidades

- Domínio (records): `Student`, `StudentDocument`, `StudentContact`, `StudentAddress`, `StudentBranchAssignment`, `StudentStatusEntry`, `StudentNote`, `StudentHistoryEntry`; enum `StudentStatus` com transições; `StudentSearchCriteria`.
- JPA: `StudentJpaEntity` (`@Version`), `StudentDocumentJpaEntity`, `StudentContactJpaEntity`, `StudentAddressJpaEntity`, `StudentBranchAssignmentJpaEntity`, `StudentStatusHistoryJpaEntity`, `StudentNoteJpaEntity` — mapeadas às tabelas V8/V9.

## 7. Casos de uso

- `CreateStudentUseCase`, `UpdateStudentUseCase`, `FindStudentByIdUseCase`, `ListStudentsUseCase`, `ChangeStudentStatusUseCase`
- `AddStudentDocumentUseCase`, `UpdateStudentDocumentUseCase`, `RemoveStudentDocumentUseCase`
- `AddStudentContactUseCase`, `UpdateStudentContactUseCase`, `RemoveStudentContactUseCase`
- `UpsertStudentAddressUseCase`, `GetStudentAddressUseCase`
- `AssignStudentToBranchUseCase`, `TransferStudentBranchUseCase`, `RemoveStudentBranchAssignmentUseCase`
- `GetStudentHistoryUseCase`, `AddStudentNoteUseCase`, `ListStudentNotesUseCase`
- Infra compartilhada de aplicação: `StudentGuard`, `StudentAssembler`, `PostgresStudentRegistrationGenerator`.

## 8. Endpoints

Base: `/api/students` (tenant sempre do `TenantContext` autenticado).

- `POST /api/students` — cadastro (201)
- `GET /api/students?search=&document=&status=&branchId=&page=&size=&sort=&direction=` — listagem paginada/filtrada
- `GET /api/students/{id}[?reveal=]` — detalhe (CPF/documentos mascarados por padrão; revelação exige `students:read_sensitive`)
- `PUT /api/students/{id}` — atualização
- `DELETE /api/students/{id}` — soft delete (204)
- `PATCH /api/students/{id}/status` — alteração de status com histórico
- `GET /api/students/{id}/history` — histórico unificado
- `GET|POST /api/students/{id}/documents`, `PUT|DELETE /api/students/{id}/documents/{documentId}`
- `GET|POST /api/students/{id}/contacts`, `PUT|DELETE /api/students/{id}/contacts/{contactId}`
- `GET|PUT /api/students/{id}/address`
- `GET|POST /api/students/{id}/branches`, `POST /api/students/{id}/transfer`, `DELETE /api/students/{id}/branches/{assignmentId}`
- `GET|POST /api/students/{id}/notes`

## 9. Telas implementadas

- `/academy/students` — listagem com busca por nome/documento, filtros de status/unidade, ordenação, paginação, skeleton, estado vazio, feedback e ações (detalhes, editar, status, excluir).
- `/academy/students/new` — cadastro de aluno (dados pessoais, CPF, status, unidade principal, observações).
- `/academy/students/[id]/edit` — edição cadastral.
- `/academy/students/[id]` — detalhes com abas: visão geral, documentos (CRUD + revelação), contatos (CRUD), endereço (upsert), unidades (vínculo/transferência/encerramento), histórico e observações.
- Navegação: item "Alunos" no `AppNav` (permissão `students:read`/`student:read`).
- Tema: toggle claro/escuro persistido em `localStorage`, aplicado globalmente via CSS variables.

## 10. Testes executados

### Backend — direcionados (durante implementação)
- `StudentRegistrationTest` — 8 testes, 0 falhas.
- `StudentDomainTest` — 4 testes, 0 falhas.
- `StudentCoreUseCasesTest` — 10 testes, 0 falhas.
- `StudentProfileUseCasesTest` — 9 testes, 0 falhas.
- `StudentJourneyUseCasesTest` — 9 testes, 0 falhas.
- `StudentManagementIT` — 1 teste, 0 falhas.
- `StudentIsolationIT` — 2 testes, 0 falhas.
- `StudentConcurrencyIT` — 2 testes, 0 falhas.
- `RlsIsolationIT` — 3 testes, 0 falhas (ajustado para V8/V9).

### Backend — validação completa
- Comando: `JAVA_HOME=/tmp/opencode/tools/jdk21 PATH=/tmp/opencode/tools/jdk21/bin:$PATH ./mvnw -Dmaven.repo.local=/tmp/opencode/m2repo verify`
- Resultado: **BUILD SUCCESS** (Total time 18.810 s)
- Integração (failsafe): **Tests run: 21, Failures: 0, Errors: 0, Skipped: 0** — inclui `AuthFlowIT(4)`, `UserBranchAccessIT(2)`, `StudentConcurrencyIT(2)`, `StudentIsolationIT(2)`, `StudentManagementIT(1)`, `RlsIsolationIT(3)`, `TenantAdminIT(2)`, `BranchManagementIT(2)`, `RbacIT(3)`.
- Unitários (surefire): concluídos dentro do build com sucesso (inclui 40 testes novos do módulo student + testes existentes).
- Flyway: V1–V9 validadas/aplicadas com sucesso em banco PostgreSQL real via Testcontainers.

### Frontend
- Comando: `cd frontend && npm run build`
- Resultado: **sucesso** (13 rotas), incluindo `/academy/students`, `/academy/students/new`, `/academy/students/[id]`, `/academy/students/[id]/edit`.

## 11. Resultado dos testes

- **Aprovado (com execução real):** migrations V8/V9, cadastro/validação/duplicidade de CPF, consulta/atualização, mudança de status e histórico, documentos/contatos/endereço, vínculos/transferências, observações, paginação/ordenação segura, isolamento multi-tenant via API e RLS, concorrência (CPF duplicado e unicidade de matrícula), e regressão das fases anteriores.
- Observação: em corrida de CPF, a segunda requisição pode retornar `409 CPF_ALREADY_EXISTS` (pré-checagem) ou `409 DATA_CONFLICT` (restrição única no banco) — ambas preservam integridade.

## 12. Pendências

- **Smoke HTTP em stack Docker (`docker compose up -d --build`) e checklist de 62+ passos**: não executado nesta sessão (Testcontainers já validou PostgreSQL/RLS/fluxos com app real; ambiente Docker local não foi revalidado aqui). Recomenda-se rodar smoke end-to-end no Compose antes de promover.
- **Política de retenção/anonimização operacional**: documentada em `docs/security/06-student-data-retention.md`, mas rotinas automatizadas de expurgo/anonimização por prazo não foram implementadas (decisão de processo/infra).
- **Revelação sensível no detalhe**: endpoint de detalhe aceita `?reveal=true` condicionado a `students:read_sensitive`; a UI atual revela documentos via aba de documentos (endpoint dedicado). CPF completo no topo do detalhe permanece mascarado por padrão.
- **Foto (`photo_url`)**: armazenada como URL; upload/armazenamento de arquivos não faz parte desta fase.

## 13. Checklist de aceite

- [x] Inspeção do projeto, docs Fases 1–3, migrations, entidades, tenant/branch, auth/RBAC, TenantContext/RLS e testes existentes executados.
- [x] Isolamento multi-tenant preservado/corrigido (tenant do contexto, RLS/FKs compostas, testes de isolamento).
- [x] Tabelas de alunos/documentos/contatos/endereços/vínculos/histórico/observações com UUID, constraints e RLS.
- [x] Regras de negócio: CPF validado/normalizado, duplicidade por tenant, matrícula própria, múltiplos vínculos (um principal), transferência com histórico, status registrado, sem exclusão física indevida, tenant imutável.
- [x] Clean Architecture e casos de uso implementados no módulo `student`.
- [x] API REST com DTOs, validação, paginação/filtros/ordenação e erros padronizados.
- [x] Frontend MVVM com listagem, cadastro, edição, detalhes e gestão de documentos/contatos/endereço/unidades/histórico/observações; responsivo e com tema claro/escuro.
- [x] Segurança/LGPD: permissões, mascaramento, sem CPF em logs, sem biometria/reconhecimento facial; retenção documentada.
- [x] Migrations Flyway novas (V8/V9), sem alterar aplicadas.
- [x] Testes unitários e de integração (Testcontainers) cobrindo os cenários exigidos, incluindo RLS/isolamento/paginação/concorrência.
- [x] Build backend, testes unitários/integração, validação Flyway e build frontend executados com sucesso.
- [ ] Smoke Docker/HTTP end-to-end no Compose (pendente de execução nesta sessão).
- [x] Nenhum avanço para a Fase 5.

**Aguardando aprovação antes de iniciar a Fase 5.**
