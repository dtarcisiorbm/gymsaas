# Graph Report - gymsaas  (2026-11-19)

## Corpus Check
- 520 files · ~160,979 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 10 file(s) not represented in the graph (top: (none) 6, .example 1, .cmd 1)

## Summary
- 4293 nodes · 14342 edges · 184 communities (125 shown, 59 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 1320 edges (avg confidence: 0.86)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `5f9fb6d6`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- localdate
- UsersViewModel.ts
- PageShell
- plans/model/types.ts
- uuid
- org.springframework.transaction.annotation.Transactional
- org.springframework.security.access.prepost.PreAuthorize
- instant
- ChargeRepository
- Phase5IntegrationSupport
- ContractViewModel.ts
- memberships/[id]/page.tsx
- errorMessage
- MembershipContract
- DemoDataSeeder.java
- .execute
- org.junit.jupiter.api.Test
- org.springframework.stereotype.Service
- StudentRepositoryAdapter
- Tenant
- AuditService
- .execute
- .login
- RoleRepository
- Documento de Segurança — Fase 1
- ContractIT.java
- StudentBranchAssignmentRepository
- UserJpaEntity
- JwtTokenService.java
- .recordForTenant
- Mapeamento da API Control iD — iDFace + iDBlock
- BranchService
- Dicionário de Dados por Tabela
- .generate
- GymSaaS — Documentação do Projeto
- .badRequest
- TenantAdminIT
- StudentStatus
- students/model/types.ts
- SecurityConfig.java
- BranchJpaEntity
- Membership
- ContractController.java
- PlanGuard
- ChargesViewModel
- AuditLogJpaEntity
- .code
- Arquitetura do Gateway Local
- Plano da Fase 2 — Implementação da Infraestrutura
- StudentDetailViewModel
- MembershipEventType
- localdate
- package.json
- .today
- UsersViewModel
- FASE 1 — RESUMO EXECUTIVO E ENTREGÁVEIS
- RbacService
- assertequals
- RegisterContractAcceptanceUseCase.java
- TenantContextFilter.java
- RefreshTokenJpaEntity
- .conflict
- Payment
- MembershipLifecycleIT.java
- org.springframework.jdbc.core.JdbcTemplate
- MembershipStatus
- ChargesViewModel.ts
- StudentBranchAssignmentRepositoryAdapter
- TenantJpaEntity
- StudentIsolationIT
- MembershipDetailViewModel
- MembershipFreeze
- Charge
- Relatório Final — Fase 4: Gestão de Alunos
- compilerOptions
- ContractContentFactory
- StudentNoteRepositoryAdapter
- Arquitetura Geral do Sistema
- GymSaaS — Sistema de Academia Multi-Tenant
- Plan
- jakarta.servlet.http.HttpServletRequest
- Review Focus
- ADR 0006: Biometria: Privacy by Design + LGPD Compliance
- ADR 0010: Autenticação Gateway ↔ SaaS: mTLS + JWT Híbrido
- BillingDtos
- Membership
- StudentContact
- BillingSummaryUseCase.java
- ADR 0008: Sincronização: Event-Driven com Idempotência
- ThemeProvider.tsx
- ADR 0009: Access Logs: Particionamento Mensal + Arquivamento
- Fase 3 — Relatório Final de Execução
- OpenPdfContractDocumentRenderer.java
- GetContractHistoryUseCase.java
- StudentContactRepositoryAdapter
- ADR 0007: Frontend: MVVM com Next.js 14 App Router
- .refresh
- PlanPrice
- AuthService
- app
- CancelMembershipUseCase.java
- docker-compose.yml
- FileSystemContractDocumentStorage.java
- application.yml
- ADR 0002: Estratégia Multi-Tenant: Shared Schema + RLS
- ADR 0003: Gateway Local: Offline-First com Sync Periódico
- ADR 0004: Control iD: Modo Standalone + Monitor + Push Híbrido
- ADR 0005: UUID v7 como Chave Primária Global
- MembershipsViewModel
- ContractAcceptanceRepositoryAdapter
- PlanFeature
- StudentAddress
- health
- frontend
- environment
- ADR 0001: Backend Monólito Modular vs Microsserviços
- ArchitectureBoundaryTest.java
- .execute
- spring
- hikari
- jpa
- backend
- org.springframework.context.annotation.Bean
- flyway
- GymSaasApplication.java
- security
- jwt
- healthcheck
- environment
- postgres
- Retenção e proteção de dados de alunos (LGPD)
- Plano — Fase 5: Planos, Matrículas e Contratos
- User
- .execute
- PlansViewModel
- server
- build
- 4. Backend
- 00-adr-index.md
- SecurityProperties
- Relatório final — Fase 6: Financeiro (cobranças e recebimentos manuais)
- next.config.mjs
- 01-app-role.sh
- com.gymsaas:gymsaas-backend
- PlanFeatureRepositoryAdapter
- PermissionJpaEntity
- MembershipFormViewModel
- BillingDtos.java
- RevokedSessionService
- StudentStatusHistoryRepositoryAdapter
- Plano — Fase 6: Financeiro (cobranças e recebimentos manuais)
- .generate
- ChargeKind
- DemoDataProperties
- TenantRepository
- TenantCalendarRepository

## God Nodes (most connected - your core abstractions)
1. `AuditService` - 97 edges
2. `errorMessage()` - 79 edges
3. `StudentGuard` - 73 edges
4. `TenantCalendar` - 67 edges
5. `PageResult` - 64 edges
6. `Branch` - 63 edges
7. `Membership` - 59 edges
8. `BranchRepository` - 58 edges
9. `ApiException` - 58 edges
10. `MembershipGuard` - 55 edges

## Surprising Connections (you probably didn't know these)
- `2. Pendências das fases anteriores` --references--> `RlsIsolationIT`  [INFERRED]
  docs/specs/2026-11-12-phase4-final-report.md → backend/src/test/java/com/gymsaas/tenant/RlsIsolationIT.java
- `9. Telas implementadas` --references--> `AppNav()`  [INFERRED]
  docs/specs/2026-11-12-phase4-final-report.md → frontend/src/components/layout/AppNav.tsx
- `6. Frontend` --references--> `ChargesViewModel`  [INFERRED]
  docs/specs/2026-11-19-phase6-final-report.md → frontend/src/features/billing/viewmodel/ChargesViewModel.ts
- `Backend (Spring Boot 3.5.16, Java 21, Maven)` --references--> `ArchitectureBoundaryTest`  [INFERRED]
  docs/specs/2026-11-12-phase2-report.md → backend/src/test/java/com/gymsaas/ArchitectureBoundaryTest.java
- `Critérios de Aceite` --references--> `Student`  [INFERRED]
  docs/specs/2026-11-12-phase2-plan.md → backend/src/main/java/com/gymsaas/student/domain/Student.java

## Import Cycles
- None detected.

## Communities (184 total, 59 thin omitted)

### Community 0 - "localdate"
Cohesion: 0.06
Nodes (16): ContractedConditions, BillingPeriod, ANNUAL, MONTHLY, ONE_TIME, QUARTERLY, SEMIANNUAL, DurationType (+8 more)

### Community 1 - "UsersViewModel.ts"
Cohesion: 0.06
Nodes (37): 1. O que foi implementado, Backend (Spring Boot 3.5.16, Java 21, Maven), Banco (PostgreSQL 16 + Flyway), Frontend (Next.js 14.2, TypeScript, Tailwind, MVVM), Infra, 11. Complemento 2026-11-17 — frontend e smoke, AppHeaderProps, API_URL (+29 more)

### Community 2 - "PageShell"
Cohesion: 0.08
Nodes (49): BranchesContent(), BranchesPage(), AcademySettingsContent(), AcademySettingsPage(), EditStudentPage(), Field(), formatDate(), formatDateTime() (+41 more)

### Community 3 - "plans/model/types.ts"
Cohesion: 0.04
Nodes (80): FeedbackState, FeedbackViewModel, normalizeMoney(), PageResult, clean(), createPayload(), membershipApi, money() (+72 more)

### Community 5 - "org.springframework.transaction.annotation.Transactional"
Cohesion: 0.14
Nodes (7): Branch, ApiException, BrDocuments, UuidV7, StudentAssembler, StudentDtos, StudentSummary

### Community 6 - "org.springframework.security.access.prepost.PreAuthorize"
Cohesion: 0.08
Nodes (6): ChargeController, ContractController, UserController, MembershipController, PlanController, BranchAssignmentView

### Community 7 - "instant"
Cohesion: 0.09
Nodes (13): ContractAcceptanceJpaEntity, MembershipContractJpaEntity, MembershipFreezeJpaEntity, MembershipHistoryJpaEntity, PlanJpaEntity, PlanPriceJpaEntity, StudentAddressJpaEntity, StudentBranchAssignmentJpaEntity (+5 more)

### Community 8 - "ChargeRepository"
Cohesion: 0.17
Nodes (15): BillingJournal, BillingLoader, MembershipBillingAdapter, CancelChargeUseCase, CreateManualChargeUseCase, ListChargesUseCase, RefundPaymentUseCase, RegisterPaymentUseCase (+7 more)

### Community 9 - "Phase5IntegrationSupport"
Cohesion: 0.11
Nodes (13): BillingIntegrationSupport, BillingManagementIT, BillingSecurityIT, Other, ContractIT, MembershipConcurrencyIT, MembershipIsolationIT, Other (+5 more)

### Community 10 - "ContractViewModel.ts"
Cohesion: 0.14
Nodes (10): saveBlob(), sha256Hex(), contractApi, ContractAcceptance, ContractStatus, ContractSummary, ContractView, ContractState (+2 more)

### Community 11 - "memberships/[id]/page.tsx"
Cohesion: 0.10
Nodes (62): BillingContent(), BillingPage(), SummaryCard(), ContractContent(), ContractPage(), allTabs, daysBetween(), MembershipDetailContent() (+54 more)

### Community 12 - "errorMessage"
Cohesion: 0.04
Nodes (27): Frontend — modificados, Frontend — novos, errorMessage(), ViewModel, isValidDocument(), isValidEmail(), onlyDigits(), required() (+19 more)

### Community 13 - "MembershipContract"
Cohesion: 0.17
Nodes (7): ContractStatus, ACCEPTED, GENERATED, SUPERSEDED, MembershipContract, ContractRepositoryAdapter, MembershipContractJpaRepository

### Community 14 - "DemoDataSeeder.java"
Cohesion: 0.05
Nodes (15): BranchSettingsRepository, BranchJpaRepository, BranchRepositoryAdapter, BranchSettingsJpaRepository, BranchSettingsRepositoryAdapter, UserBranchAccessJpaEntity, UserBranchAccessJpaRepository, UserBranchAccessRepositoryAdapter (+7 more)

### Community 15 - ".execute"
Cohesion: 0.06
Nodes (11): AddressRequest, AddressView, ChangeStudentStatusRequest, CreateStudentRequest, DocumentRequest, DocumentView, StudentDetail, UpdateStudentRequest (+3 more)

### Community 16 - "org.junit.jupiter.api.Test"
Cohesion: 0.05
Nodes (11): BillingSchedule, Installment, MembershipTerm, Period, BillingScheduleTest, ChargeRulesTest, MembershipRulesTest, MembershipTermTest (+3 more)

### Community 17 - "org.springframework.stereotype.Service"
Cohesion: 0.20
Nodes (12): MembershipDetailLoader, MembershipGuard, MembershipJournal, FindMembershipByIdUseCase, FreezeMembershipUseCase, ListMembershipsUseCase, ReactivateMembershipUseCase, SuspendMembershipUseCase (+4 more)

### Community 18 - "StudentRepositoryAdapter"
Cohesion: 0.11
Nodes (4): StudentDocumentJpaRepository, StudentDocumentRepositoryAdapter, StudentJpaRepository, StudentRepositoryAdapter

### Community 19 - "Tenant"
Cohesion: 0.10
Nodes (11): TenantAdminService, ChangeTenantStatusRequest, CreateTenantRequest, TenantDetail, TenantDtos, TenantSummary, UpdateTenantRequest, Tenant (+3 more)

### Community 20 - "AuditService"
Cohesion: 0.14
Nodes (37): AuditService, BranchRepository, GenerateMembershipContractUseCase, StudentGuard, AddStudentContactUseCase, AddStudentDocumentUseCase, AddStudentNoteUseCase, AssignStudentToBranchUseCase (+29 more)

### Community 21 - ".execute"
Cohesion: 0.13
Nodes (4): FreezeMembershipRequest, OptionalReasonRequest, ReasonRequest, FreezeMembershipUseCaseTest

### Community 22 - ".login"
Cohesion: 0.18
Nodes (8): AuthDtos, LoginRequest, MeResponse, SessionResponse, TenantSelectionResponse, TenantView, UserView, AuthResult

### Community 23 - "RoleRepository"
Cohesion: 0.16
Nodes (3): RoleJpaEntity, RoleJpaRepository, RoleRepositoryAdapter

### Community 24 - "Documento de Segurança — Fase 1"
Cohesion: 0.05
Nodes (42): 10. Riscos de Segurança Residuais, 11. Checklist Validação Segurança Fase 1, 1.1 Backend SaaS (Staff, Admin, Alunos Web), 1.2 Gateway ↔ SaaS, 1.3 Gateway ↔ Dispositivos Control iD, 1.4 Alunos (App Mobile / Portal), 1. Autenticação, 2.1 Matriz de Roles (+34 more)

### Community 26 - "StudentBranchAssignmentRepository"
Cohesion: 0.21
Nodes (3): AssignBranchRequest, TransferBranchRequest, StudentBranchAssignment

### Community 27 - "UserJpaEntity"
Cohesion: 0.10
Nodes (4): TenantMember, TenantMemberProjection, UserJpaEntity, UserRepositoryAdapter

### Community 28 - "JwtTokenService.java"
Cohesion: 0.20
Nodes (3): IssuedAccessToken, JwtTokenService, JwtTokenServiceTest

### Community 29 - ".recordForTenant"
Cohesion: 0.14
Nodes (11): UserBranchAccessRepository, AssignBranchesRequest, AssignRolesRequest, CreateUserRequest, UpdateStatusRequest, UpdateUserRequest, UserAdminDtos, UserBranchView (+3 more)

### Community 30 - "Mapeamento da API Control iD — iDFace + iDBlock"
Cohesion: 0.05
Nodes (39): Autenticação e Sessão, Ações Remotas (API Direta — Não Push/Monitor), Batch Transactions (Múltiplos comandos), Cadastro por Foto (Preferido para iDFace), Cadastro Remoto (`remote_enroll.fcgi`), Checklist Integração Control iD, Comandos Úteis via Push, Configurações Faciais Relevantes (`set_configuration.fcgi`) (+31 more)

### Community 31 - "BranchService"
Cohesion: 0.09
Nodes (11): BranchDetail, BranchDtos, BranchSettingsView, BranchSummary, ChangeBranchStatusRequest, CreateBranchRequest, UpdateBranchRequest, UpdateBranchSettingsRequest (+3 more)

### Community 32 - "Dicionário de Dados por Tabela"
Cohesion: 0.05
Nodes (38): 10. `students` — Alunos, 11. `plans` — Planos de Mensalidade (por Academia), 12. `student_plans` — Vínculo Aluno ↔ Plano (Assinatura), 13. `payments` — Pagamentos, 14. `biometric_profiles` — Perfil Biométrico do Aluno, 15. `biometric_enrollments` — Enrolamentos por Dispositivo, 16. `biometric_consents` — Consentimento LGPD (Art. 7º, 11), 17. `gateways` — Gateways Locais (Hardware por Unidade) (+30 more)

### Community 33 - ".generate"
Cohesion: 0.15
Nodes (4): Membership, MembershipRepository, MembershipProjection, MembershipRepositoryAdapter

### Community 34 - "GymSaaS — Documentação do Projeto"
Cohesion: 0.05
Nodes (37): 1. Autenticação e sessão (`identity`, `shared`), 2. Administração de plataforma e academias (`tenant`), 3. Unidades/filiais (`branch`), 4. Usuários e RBAC (`identity`, `rbac`), 5. Auditoria (`audit`), 6. Alunos (`student`) — Fase 4, 7. Planos, matrículas e contratos (`plan`, `membership`, `contract`) — Fase 5 (backend), 8. Financeiro (`billing`) — Fase 6 (+29 more)

### Community 36 - "TenantAdminIT"
Cohesion: 0.08
Nodes (23): BranchManagementIT, UserBranchAccessIT, BrDocumentsTest, TenantAdminIT, 3. Arquivos criados e modificados, 7. Testes executados e resultados, Backend — novos, Documentação (+15 more)

### Community 37 - "StudentStatus"
Cohesion: 0.06
Nodes (14): HistoryView, NoteRequest, NoteView, StudentFilter, StudentNote, StudentSearchCriteria, StudentStatus, ACTIVE (+6 more)

### Community 38 - "students/model/types.ts"
Cohesion: 0.07
Nodes (63): 27. `device_events` — Eventos Recebidos dos Equipamentos (via Monitor/Push), Task 10: Frontend — tipos e API, apiClient, QueryValue, toQuery(), branchApi, BranchListParams, clean() (+55 more)

### Community 41 - "Membership"
Cohesion: 0.16
Nodes (4): MembershipSearchCriteria, StudentMembershipJpaEntity, StudentMembershipJpaRepository, StudentMembershipRepositoryAdapter

### Community 42 - "ContractController.java"
Cohesion: 0.17
Nodes (5): AuditController, AuditLogResponse, BillingController, PaymentController, TenantContext

### Community 43 - "PlanGuard"
Cohesion: 0.20
Nodes (15): PlanDetailLoader, PlanGuard, PlanPriceFactory, AddPlanPriceUseCase, CancelPlanPriceUseCase, ChangePlanStatusUseCase, CreatePlanUseCase, FindPlanByIdUseCase (+7 more)

### Community 44 - "ChargesViewModel"
Cohesion: 0.09
Nodes (3): ChargesWorkspaceProps, ChargesViewModel, emptyCreate()

### Community 45 - "AuditLogJpaEntity"
Cohesion: 0.12
Nodes (5): AuditLog, AuditRepository, AuditLogJpaEntity, AuditLogJpaRepository, AuditRepositoryAdapter

### Community 47 - ".code"
Cohesion: 0.27
Nodes (4): MembershipPricing, Quote, PlanFeatureRepository, MembershipPricingTest

### Community 48 - "Arquitetura do Gateway Local"
Cohesion: 0.07
Nodes (29): 1. Bootstrap do Gateway (Primeira Inicialização), 1. ControlIdApiClient, 2. ControlIdMonitorServer, 2. Sync Periódico (A cada 15min / configurável), 3. ControlIdPushClient, 3. Evento de Acesso (Monitor → SaaS), 4. Comando Imediato (SaaS → Device via Push/WebSocket), 4. SyncOrchestrator (+21 more)

### Community 49 - "Plano da Fase 2 — Implementação da Infraestrutura"
Cohesion: 0.07
Nodes (29): Checklist Go/No-Go para Fase 3 (Funcionalidades Avançadas), Critérios de Aceite, Critérios de Aceite, Critérios de Aceite, Critérios de Aceite, Critérios de Aceite, Critérios de Aceite, Critérios de Aceite (+21 more)

### Community 50 - "StudentDetailViewModel"
Cohesion: 0.07
Nodes (3): Task 11: Frontend — ViewModels MVVM, StudentDetailViewModel, StudentsViewModel

### Community 51 - "MembershipEventType"
Cohesion: 0.09
Nodes (19): HistoryEntryView, GetMembershipHistoryUseCase, MembershipEvent, MembershipEventType, ACTIVATED, CANCELLED, CONTRACT_ACCEPTED, CONTRACT_GENERATED (+11 more)

### Community 52 - "localdate"
Cohesion: 0.15
Nodes (9): PlanAssembler, FeatureRequest, PlanDtos, PlanSummary, ReplaceFeaturesRequest, PlanPrice, PlanPriceStatus, ACTIVE (+1 more)

### Community 53 - "package.json"
Cohesion: 0.09
Nodes (27): dependencies, next@14.2.35, react@18.3.1, react-dom@18.3.1, devDependencies, autoprefixer@^10.4.20, postcss@^8.4.47, tailwindcss@3.4.19 (+19 more)

### Community 54 - ".today"
Cohesion: 0.12
Nodes (8): CancelPriceRequest, CreatePlanRequest, FreezePolicyRequest, PriceRequest, PriceView, UpdatePlanRequest, AddPlanPriceUseCaseTest, TenantCalendarTest

### Community 56 - "FASE 1 — RESUMO EXECUTIVO E ENTREGÁVEIS"
Cohesion: 0.08
Nodes (25): 10. Checklist para Iniciar Fase 2, 1. Diagnóstico do Projeto, 2. Arquitetura Proposta, 3. Estrutura de Pastas (Proposta), 4. Modelo de Banco de Dados, 5. Integração Control iD (iDFace + iDBlock), 6. Arquitetura do Gateway Local, 7. Documentação Criada (docs/) (+17 more)

### Community 57 - "RbacService"
Cohesion: 0.13
Nodes (8): RbacService, RoleView, Permission, PermissionRepository, PermissionController, PermissionResponse, RoleController, RoleResponse

### Community 58 - "assertequals"
Cohesion: 0.08
Nodes (9): BranchStatus, ACTIVE, INACTIVE, RlsAwareDataSource, TenantStatus, ACTIVE, INACTIVE, PENDING (+1 more)

### Community 59 - "RegisterContractAcceptanceUseCase.java"
Cohesion: 0.17
Nodes (7): ContractGuard, DownloadContractDocumentUseCase, FindContractByIdUseCase, GetContractHistoryUseCase, RegisterContractAcceptanceUseCase, ContractAcceptanceRepository, ContractRepository

### Community 60 - "TenantContextFilter.java"
Cohesion: 0.09
Nodes (5): SelectTenantRequest, CurrentPrincipal, PrincipalMapper, TenantContextFilter, 3. Problemas encontrados e correções

### Community 61 - "RefreshTokenJpaEntity"
Cohesion: 0.16
Nodes (3): RefreshTokenJpaEntity, RefreshTokenJpaRepository, RefreshTokenRepositoryAdapter

### Community 62 - ".conflict"
Cohesion: 0.21
Nodes (4): Draft, MembershipDraftFactory, UpdateMembershipRequest, QuoteMembershipUseCase

### Community 63 - "Payment"
Cohesion: 0.10
Nodes (6): RegisterPaymentRequest, Payment, PaymentStatus, CONFIRMED, REFUNDED, JdbcPaymentRepository

### Community 64 - "MembershipLifecycleIT.java"
Cohesion: 0.13
Nodes (8): CreateMembershipRequest, LifecycleResult, RenewMembershipRequest, ProcessMembershipLifecycleUseCase, MembershipLifecycleScheduler, Context, Fixture, MembershipLifecycleIT

### Community 65 - "org.springframework.jdbc.core.JdbcTemplate"
Cohesion: 0.10
Nodes (5): PostgresBillingNumberGenerator, MembershipNumberGenerator, PostgresMembershipNumberGenerator, PostgresStudentRegistrationGenerator, TenantCalendarJdbcRepository

### Community 66 - "MembershipStatus"
Cohesion: 0.12
Nodes (8): MembershipFilter, MembershipStatus, ACTIVE, CANCELLED, EXPIRED, FROZEN, PENDING, SUSPENDED

### Community 67 - "ChargesViewModel.ts"
Cohesion: 0.11
Nodes (26): billingApi, AmountView, BillingSummary, Charge, CHARGE_KIND_LABELS, CHARGE_STATUS_FILTERS, CHARGE_STATUS_LABELS, CHARGE_STATUS_STYLES (+18 more)

### Community 69 - "TenantJpaEntity"
Cohesion: 0.12
Nodes (3): TenantJpaEntity, TenantJpaRepository, TenantRepositoryAdapter

### Community 70 - "StudentIsolationIT"
Cohesion: 0.09
Nodes (15): StudentIsolationIT, RlsIsolationIT, 10. Checklist de aceite (backend), 1. Diagnóstico inicial, 2. Pendências das fases anteriores (tratadas), 4. Arquivos modificados, 5. Migrations, 6. Regras de negócio (+7 more)

### Community 72 - "MembershipFreeze"
Cohesion: 0.13
Nodes (7): FreezeStatus, ACTIVE, CANCELLED, COMPLETED, MembershipFreeze, MembershipFreezeJpaRepository, MembershipFreezeRepositoryAdapter

### Community 74 - "Relatório Final — Fase 4: Gestão de Alunos"
Cohesion: 0.11
Nodes (17): 10. Testes executados, 11. Resultado dos testes, 12. Pendências, 13. Checklist de aceite, 1. Diagnóstico inicial, 2. Pendências das fases anteriores, 3. Arquivos criados, 5. Migrations (+9 more)

### Community 75 - "compilerOptions"
Cohesion: 0.11
Nodes (18): compilerOptions, allowJs, esModuleInterop, incremental, isolatedModules, jsx, lib, module (+10 more)

### Community 76 - "ContractContentFactory"
Cohesion: 0.24
Nodes (3): ContractContentFactory, ContractData, ContractContentFactoryTest

### Community 78 - "Arquitetura Geral do Sistema"
Cohesion: 0.11
Nodes (17): Arquitetura Geral do Sistema, Comunicação entre Componentes, Critérios de Aceite Fase 1, Decisões Arquiteturais Principais, Diagrama de Componentes (Mermaid), Estratégia Multi-Tenant, Estrutura de Módulos do Backend (Monólito Modular), Formato de Contrato (Exemplo) (+9 more)

### Community 79 - "GymSaaS — Sistema de Academia Multi-Tenant"
Cohesion: 0.08
Nodes (22): AuthFlowIT, 2. Validação executada (resultados reais), 4. Pendências / não implementado nesta fase, 5. Checklist de aceite da Fase 2, Fase 2 — Relatório de Execução, API (alunos), Backend, Backend (+14 more)

### Community 80 - "Plan"
Cohesion: 0.09
Nodes (10): ChangePlanStatusRequest, PlanFilter, Plan, PlanSearchCriteria, PlanStatus, ACTIVE, ARCHIVED, INACTIVE (+2 more)

### Community 81 - "jakarta.servlet.http.HttpServletRequest"
Cohesion: 0.18
Nodes (3): AuthController, ErrorResponse, GlobalExceptionHandler

### Community 82 - "Review Focus"
Cohesion: 0.09
Nodes (20): StudentConcurrencyIT, StudentManagementIT, Execução, Ledger — Fase 4 (Gestão de Alunos), Pre-flight, Fase 4 — Gestão de Alunos: Plano de Implementação, Global Constraints, Review Focus (+12 more)

### Community 83 - "ADR 0006: Biometria: Privacy by Design + LGPD Compliance"
Cohesion: 0.12
Nodes (16): 1. Minimização de Dados, 2. Consentimento Explícito Versionado, 3. Finalidade Restrita, 4. Retenção Limitada, 5. Segurança Reforçada, 6. Direitos do Titular (Automatizados), ADR 0006: Biometria: Privacy by Design + LGPD Compliance, Alternativas Consideradas (+8 more)

### Community 84 - "ADR 0010: Autenticação Gateway ↔ SaaS: mTLS + JWT Híbrido"
Cohesion: 0.12
Nodes (16): 1. PKI Própria (Private CA), 2. Bootstrap do Gateway, 3. Comunicação Runtime, 4. Renovação de JWT (Sem Round-trip Adicional), 5. WebSocket para Comandos Imediatos, 6. Revogação e Comprometimento, ADR 0010: Autenticação Gateway ↔ SaaS: mTLS + JWT Híbrido, Alternativas Consideradas (+8 more)

### Community 85 - "BillingDtos"
Cohesion: 0.13
Nodes (11): BillingAssembler, AmountView, BillingDtos, ChargeDetail, ChargeFilter, ChargeView, MethodAmountView, PaymentView (+3 more)

### Community 86 - "Membership"
Cohesion: 0.11
Nodes (9): MembershipAssembler, ConditionsView, FreezePolicyView, FreezeView, MembershipDetail, MembershipDtos, MembershipSummary, QuoteView (+1 more)

### Community 87 - "StudentContact"
Cohesion: 0.28
Nodes (3): ContactRequest, ContactView, StudentContact

### Community 88 - "BillingSummaryUseCase.java"
Cohesion: 0.12
Nodes (10): BillingSummaryUseCase, Amount, BillingTotals, PaymentMethod, BANK_TRANSFER, CASH, CREDIT_CARD, DEBIT_CARD (+2 more)

### Community 89 - "ADR 0008: Sincronização: Event-Driven com Idempotência"
Cohesion: 0.12
Nodes (15): 1. Idempotency Keys (Todas Operações), 2. Event Sourcing Leve (Gateway), 3. Outbox Pattern (SaaS → Gateway), 4. Reconciliação Periódica, 5. Dead Letter Queue (DLQ), ADR 0008: Sincronização: Event-Driven com Idempotência, Alternativas Consideradas, Consequências (+7 more)

### Community 90 - "ThemeProvider.tsx"
Cohesion: 0.19
Nodes (12): 4. Arquivos modificados, Task 12: Frontend — telas, navegação e tema, metadata, RootLayout(), ThemeToggle(), SessionProvider(), applyTheme(), Theme (+4 more)

### Community 91 - "ADR 0009: Access Logs: Particionamento Mensal + Arquivamento"
Cohesion: 0.13
Nodes (14): 1. Estrutura, 2. Índices (Locais por Partição), 3. Retenção e Arquivamento, 4. Queries Cross-Partition (Raras), ADR 0009: Access Logs: Particionamento Mensal + Arquivamento, Alternativas Consideradas, Consequências, Contexto (+6 more)

### Community 92 - "Fase 3 — Relatório Final de Execução"
Cohesion: 0.13
Nodes (14): 10. Checklist de aceite da Fase 3, 11. Conclusão, 1. Diagnóstico inicial, 2. Lacunas identificadas e tratadas, 4. Migrations Flyway, 5. Endpoints, 6. Funcionalidades entregues, 8. Validação da stack e smoke test HTTP (+6 more)

### Community 94 - "GetContractHistoryUseCase.java"
Cohesion: 0.19
Nodes (6): ContractAssembler, AcceptanceView, ContractDtos, ContractSummary, ContractView, RegisterAcceptanceRequest

### Community 96 - "ADR 0007: Frontend: MVVM com Next.js 14 App Router"
Cohesion: 0.14
Nodes (13): ADR 0007: Frontend: MVVM com Next.js 14 App Router, Alternativas Consideradas, Arquitetura de Camadas, Consequências, Contexto, Decisão, Negativas, Positivas (+5 more)

### Community 97 - ".refresh"
Cohesion: 0.19
Nodes (4): RefreshTokenCodec, RefreshToken, RefreshTokenRepository, RefreshTokenCodecTest

### Community 99 - "AuthService"
Cohesion: 0.22
Nodes (4): AccessGrant, AuthService, Role, RoleRepository

### Community 100 - "app"
Cohesion: 0.17
Nodes (12): app, demo-data, memberships, storage, tenancy, base-dir, enabled, cron (+4 more)

### Community 101 - "CancelMembershipUseCase.java"
Cohesion: 0.46
Nodes (4): MembershipBillingPort, CancelMembershipUseCase, CreateMembershipUseCase, RenewMembershipUseCase

### Community 102 - "docker-compose.yml"
Cohesion: 0.17
Nodes (11): name, driver, name, name, networks, gymsaas, name, services (+3 more)

### Community 103 - "FileSystemContractDocumentStorage.java"
Cohesion: 0.10
Nodes (6): DocumentFile, ContractDocumentStorage, StoredDocument, FileSystemContractDocumentStorage, Sha256, 2. Decisões de negócio aprovadas

### Community 104 - "application.yml"
Cohesion: 0.18
Nodes (10): path, com.gymsaas, org.flywaydb, root, logging, level, springdoc, api-docs (+2 more)

### Community 105 - "ADR 0002: Estratégia Multi-Tenant: Shared Schema + RLS"
Cohesion: 0.18
Nodes (10): ADR 0002: Estratégia Multi-Tenant: Shared Schema + RLS, Alternativas Consideradas, Consequências, Contexto, Decisão, Negativas, Positivas, Referências (+2 more)

### Community 106 - "ADR 0003: Gateway Local: Offline-First com Sync Periódico"
Cohesion: 0.18
Nodes (10): ADR 0003: Gateway Local: Offline-First com Sync Periódico, Alternativas Consideradas, Consequências, Contexto, Decisão, Negativas, Positivas, Referências (+2 more)

### Community 107 - "ADR 0004: Control iD: Modo Standalone + Monitor + Push Híbrido"
Cohesion: 0.18
Nodes (10): ADR 0004: Control iD: Modo Standalone + Monitor + Push Híbrido, Alternativas Consideradas, Consequências, Contexto, Decisão, Negativas, Positivas, Referências (+2 more)

### Community 108 - "ADR 0005: UUID v7 como Chave Primária Global"
Cohesion: 0.18
Nodes (10): ADR 0005: UUID v7 como Chave Primária Global, Alternativas Consideradas, Consequências, Contexto, Decisão, Negativas, Positivas, Referências (+2 more)

### Community 111 - "ContractAcceptanceRepositoryAdapter"
Cohesion: 0.22
Nodes (6): AcceptanceChannel, ELECTRONIC, IN_PERSON, ContractAcceptance, ContractAcceptanceJpaRepository, ContractAcceptanceRepositoryAdapter

### Community 112 - "PlanFeature"
Cohesion: 0.09
Nodes (12): FeatureView, FreezePolicyView, PlanDetail, PlanFeature, PlanFeatureType, ALL_BRANCHES_ACCESS, BRANCH_ACCESS, CUSTOM (+4 more)

### Community 114 - "health"
Cohesion: 0.20
Nodes (10): health, web, include, probes, show-details, management, endpoint, endpoints (+2 more)

### Community 115 - "frontend"
Cohesion: 0.20
Nodes (10): condition, backend, NEXT_PUBLIC_API_URL, container_name, depends_on, environment, networks, ports (+2 more)

### Community 116 - "environment"
Cohesion: 0.20
Nodes (10): environment, CONTRACT_STORAGE_DIR, CORS_ALLOWED_ORIGINS, DB_PASSWORD, DB_URL, DB_USER, DEMO_DATA_ENABLED, FLYWAY_PASSWORD (+2 more)

### Community 117 - "ADR 0001: Backend Monólito Modular vs Microsserviços"
Cohesion: 0.20
Nodes (10): ADR 0001: Backend Monólito Modular vs Microsserviços, Alternativas Consideradas, Consequências, Contexto, Decisão, Negativas, Positivas, Referências (+2 more)

### Community 121 - "spring"
Cohesion: 0.22
Nodes (9): name, default-property-inclusion, serialization, timeout-per-shutdown-phase, write-dates-as-timestamps, spring, application, jackson (+1 more)

### Community 122 - "hikari"
Cohesion: 0.22
Nodes (9): hikari, password, url, username, connection-timeout, maximum-pool-size, minimum-idle, validation-timeout (+1 more)

### Community 123 - "jpa"
Cohesion: 0.22
Nodes (9): ddl-auto, format_sql, jdbc, time_zone, hibernate, open-in-view, properties, hibernate (+1 more)

### Community 124 - "backend"
Cohesion: 0.22
Nodes (9): container_name, depends_on, networks, ports, restart, volumes, postgres, condition (+1 more)

### Community 125 - "org.springframework.context.annotation.Bean"
Cohesion: 0.22
Nodes (3): DataSourceConfig, PasswordConfig, TimeConfig

### Community 126 - "flyway"
Cohesion: 0.25
Nodes (8): baseline-on-migrate, enabled, locations, password, url, user, validate-on-migrate, flyway

### Community 129 - "security"
Cohesion: 0.29
Nodes (7): security, name, same-site, secure, allowed-origins, cookie, cors

### Community 130 - "jwt"
Cohesion: 0.29
Nodes (7): access-token-ttl, audience, issuer, private-key, public-key, refresh-token-ttl, jwt

### Community 131 - "healthcheck"
Cohesion: 0.48
Nodes (7): healthcheck, interval, retries, start_period, test, timeout, healthcheck

### Community 132 - "environment"
Cohesion: 0.29
Nodes (7): APP_DB_PASSWORD, APP_DB_USER, POSTGRES_DB, POSTGRES_PASSWORD, POSTGRES_USER, TZ, environment

### Community 133 - "postgres"
Cohesion: 0.29
Nodes (7): container_name, image, networks, ports, restart, volumes, postgres

### Community 134 - "Retenção e proteção de dados de alunos (LGPD)"
Cohesion: 0.29
Nodes (6): Dados tratados, Exclusão e anonimização, Princípios aplicados, Restrições desta fase, Retenção, Retenção e proteção de dados de alunos (LGPD)

### Community 135 - "Plano — Fase 5: Planos, Matrículas e Contratos"
Cohesion: 0.33
Nodes (5): 3. Modelo de dados (migrations), 4. Casos de uso, 5. Plano de testes, 6. Critérios de aceite, Plano — Fase 5: Planos, Matrículas e Contratos

### Community 136 - "User"
Cohesion: 0.20
Nodes (3): User, UserRepository, Backend — modificados

### Community 139 - "server"
Cohesion: 0.33
Nodes (6): include-message, include-stacktrace, server, error, port, shutdown

### Community 140 - "build"
Cohesion: 0.40
Nodes (6): NEXT_PUBLIC_API_URL, build, args, context, dockerfile, build

### Community 141 - "4. Backend"
Cohesion: 0.40
Nodes (4): 4. Backend, Endpoints, Módulos e arquivos, Regras de negócio

### Community 142 - "00-adr-index.md"
Cohesion: 0.40
Nodes (3): Architecture Decision Records (ADRs) — Índice, Processo de Governança, Template para Novas ADRs

### Community 143 - "SecurityProperties"
Cohesion: 0.26
Nodes (5): JwtConfig, Cookie, Cors, Jwt, SecurityProperties

### Community 144 - "Relatório final — Fase 6: Financeiro (cobranças e recebimentos manuais)"
Cohesion: 0.18
Nodes (9): 4. Casos de uso (módulo `com.gymsaas.billing`), 1. Resultado, 2. Decisões aplicadas, 3. Desvios do plano, 4. API, 6. Frontend, 7. Pendências, Relatório final — Fase 6: Financeiro (cobranças e recebimentos manuais) (+1 more)

### Community 166 - "PlanFeatureRepositoryAdapter"
Cohesion: 0.29
Nodes (3): PlanFeatureJpaEntity, PlanFeatureJpaRepository, PlanFeatureRepositoryAdapter

### Community 167 - "PermissionJpaEntity"
Cohesion: 0.20
Nodes (3): PermissionJpaEntity, PermissionJpaRepository, PermissionRepositoryAdapter

### Community 173 - "Plano — Fase 6: Financeiro (cobranças e recebimentos manuais)"
Cohesion: 0.25
Nodes (7): 1. Diagnóstico, 2. Decisões de negócio, 3. Modelo de dados — V13, 5. API, 6. Frontend, 7. Testes, Plano — Fase 6: Financeiro (cobranças e recebimentos manuais)

### Community 175 - "ChargeKind"
Cohesion: 0.40
Nodes (4): ChargeKind, INSTALLMENT, MANUAL, SETUP_FEE

## Knowledge Gaps
- **75 isolated node(s):** `INSTALLMENT`, `SETUP_FEE`, `MANUAL`, `OPEN`, `PAID` (+70 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 1085 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **59 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `RequireAuth()` connect `PageShell` to `UsersViewModel.ts`, `memberships/[id]/page.tsx`, `errorMessage`, `TenantAdminIT`?**
  _High betweenness centrality (0.108) - this node is a cross-community bridge._
- **What connects `INSTALLMENT`, `SETUP_FEE`, `MANUAL` to the rest of the system?**
  _75 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `localdate` be split into smaller, more focused modules?**
  _Cohesion score 0.06335403726708075 - nodes in this community are weakly interconnected._
- **Why does `RbacIT` connect `ContractIT.java` to `TenantAdminIT`, `StudentIsolationIT`, `GymSaaS — Sistema de Academia Multi-Tenant`?**
  _High betweenness centrality (0.088) - this node is a cross-community bridge._
- **Should `UsersViewModel.ts` be split into smaller, more focused modules?**
  _Cohesion score 0.0625694187338023 - nodes in this community are weakly interconnected._
- **Why does `Rulings` connect `TenantAdminIT` to `ContractIT.java`, `PageShell`?**
  _High betweenness centrality (0.086) - this node is a cross-community bridge._
- **Should `PageShell` be split into smaller, more focused modules?**
  _Cohesion score 0.08286805759623861 - nodes in this community are weakly interconnected._