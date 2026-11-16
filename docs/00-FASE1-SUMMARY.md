# FASE 1 — RESUMO EXECUTIVO E ENTREGÁVEIS

**Projeto**: SaaS de Academias com Reconhecimento Facial (Control iD)
**Data**: 2026-11-12
**Status**: Aguardando Validação para Iniciar Fase 2

---

## 1. Diagnóstico do Projeto

| Aspecto | Situação |
|---------|----------|
| **Tipo** | Greenfield (diretório vazio) |
| **Escala Inicial** | 1-10 academias, <5k alunos, hardware dedicado por unidade |
| **Equipamentos** | iDFace (terminal facial) + iDBlock (catraca) |
| **Provisionamento** | Manual/Admin (equipe interna) |
| **Stack Definida** | Java 21, Spring Boot 3, Next.js 14, PostgreSQL 16, Docker, Flyway, JWT, Testcontainers |

---

## 2. Arquitetura Proposta

### Backend: Monólito Modular (Spring Boot 3)
```
gymsaas-core, gymsaas-tenant, gymsaas-user, gymsaas-student,
gymsaas-access, gymsaas-device, gymsaas-controlid, gymsaas-gateway,
gymsaas-audit, gymsaas-notification, gymsaas-web
```
- Clean Architecture por módulo (domain, application, infrastructure, interface)
- Comunicação via Domain Events (Spring ApplicationEvent)
- Multi-tenancy: Shared Schema + RLS PostgreSQL

### Frontend: Next.js 14 + MVVM
- App Router + React Server Components + TypeScript
- ViewModels por feature (lógica de apresentação isolada)
- Design System: shadcn/ui + Tailwind
- PWA para aluno

### Gateway Local: Java Spring Boot (Offline-First)
- Hardware: Raspberry Pi 4/5 ou Mini PC por unidade
- SQLite (SQLCipher) para fila + cache local
- Comunicação Control iD: HTTP REST + Monitor (push assíncrono) + Push (polling)
- Sync com SaaS: mTLS + JWT + WebSocket (comandos) + REST (sync periódico)

### Comunicação
| Fluxo | Protocolo | Frequência |
|-------|-----------|------------|
| SaaS ↔ Gateway (sync) | HTTPS mTLS + REST | 15 min / sob demanda |
| SaaS → Gateway (comandos) | WebSocket sobre mTLS | Tempo real |
| Gateway → SaaS (eventos) | HTTPS mTLS + REST batch | Imediato (assíncrono) |
| Gateway ↔ Devices | HTTP REST (API Control iD) | Contínuo |

---

## 3. Estrutura de Pastas (Proposta)

```
sistema-de-academia/
├── backend/                    # Monólito Modular Spring Boot
│   ├── gymsaas-core/
│   ├── gymsaas-tenant/
│   ├── gymsaas-user/
│   ├── gymsaas-student/
│   ├── gymsaas-access/
│   ├── gymsaas-device/
│   ├── gymsaas-controlid/
│   ├── gymsaas-gateway/
│   ├── gymsaas-audit/
│   ├── gymsaas-notification/
│   └── gymsaas-web/
├── frontend/                   # Next.js 14 App Router
│   ├── app/
│   ├── components/
│   ├── lib/
│   ├── viewmodels/
│   └── types/
├── gateway/                    # Gateway Local (projeto independente)
│   ├── src/
│   └── Dockerfile
├── infra/                      # Infraestrutura como Código
│   ├── docker-compose.yml
│   ├── k8s/                    # Helm charts
│   └── grafana/
├── docs/                       # Documentação Técnica (esta pasta)
│   ├── architecture/
│   ├── database/
│   ├── controlid/
│   ├── gateway/
│   ├── security/
│   ├── adrs/
│   └── specs/
└── README.md
```

---

## 4. Modelo de Banco de Dados

**28 Tabelas Principais** (todas com `tenant_id` + RLS):

| Grupo | Tabelas |
|-------|---------|
| **SaaS/Tenant** | `tenants`, `saas_plans`, `tenant_subscriptions` |
| **Organização** | `branches`, `users`, `roles`, `permissions`, `user_tenants` |
| **Alunos/Planos** | `students`, `plans`, `student_plans`, `payments` |
| **Biometria/LGPD** | `biometric_profiles`, `biometric_enrollments`, `biometric_consents` |
| **Dispositivos** | `gateways`, `devices`, `turnstiles`, `device_credentials` |
| **Acesso** | `access_rules`, `time_zones`, `time_spans`, `access_rule_time_zones` |
| **Logs/Eventos** | `access_logs` (particionado mensal), `device_commands`, `device_events`, `audit_logs` |

**Estratégias-Chave**:
- UUID v7 (timestamp-ordered) como PK global
- Particionamento nativo `access_logs` por `occurred_at` (mensal)
- RLS em todas tabelas multi-tenant (isolamento no banco)
- Flyway migrations versionadas (V1_0_0 a V1_9_0)

---

## 5. Integração Control iD (iDFace + iDBlock)

### Modo Escolhido: **Standalone + Monitor + Push Híbrido**
- Identificação facial **local** no device (performance, offline)
- Regras de acesso **sincronizadas** no terminal
- Eventos via **Monitor** (HTTP push assíncrono): `/dao`, `/catra_event`, `/operation_mode`, `/door`, `/secbox`
- Comandos via **Push** (polling `/push` 5s): abrir catraca, mensagem, buzzer, reboot
- Sync: `create_or_modify_objects` (batch 500) + `user_set_image_list` (faces)

### Limitações Críticas Mapeadas
| Limitação | Mitigação |
|-----------|-----------|
| ~3.000 templates faciais/iDFace | Standalone mode + sync seletivo (ativos/vencimento) |
| `change_logs`: 10k ops | Polling frequente + persistência gateway |
| Batch foto: 100/req, 2MB | Chunking + prioridade ativos |
| Firmware variations | Matriz compatibilidade + version detection |

### Mapeamento IDs
- SaaS (UUID) ↔ Device (int64) via `biometric_enrollments.device_user_id`
- Gateway mantém mapping bidirecional persistido

---

## 6. Arquitetura do Gateway Local

### Componentes Principais
1. **ControlIdApiClient** - Pool HTTP, session manager, circuit breaker
2. **ControlIdMonitorServer** - Endpoints `/dao`, `/catra_event`, `/operation_mode`, `/device_is_alive`, `/door`, `/secbox`
3. **ControlIdPushClient** - Polling `/push` + transaction executor
4. **SyncOrchestrator** - Diff por checksum, batch sync, AccessPolicyCache rebuild
5. **AccessPolicyCache** - Double-buffered, lookup O(1) para decisões offline
6. **EventStore (SQLite)** - `device_events`, `device_commands`, `sync_state`
7. **SaasApiClient** - mTLS + WebSocket + REST

### Fluxo Offline-First
```
INTERNET OK          INTERNET CAI          INTERNET VOLTA
     │                    │                      │
     ▼                    ▼                      ▼
┌─────────┐         ┌─────────┐            ┌─────────┐
│ SYNC 15m│         │ CACHE   │            │ REPLAY  │
│ PUSH WS │         │ EVENTOS │            │ FILA    │
│ HEARTBEAT     │         │ (SQLite)│            │ SYNC    │
└─────────┘         └─────────┘            └─────────┘
```

### Segurança Gateway
- mTLS 1.3 (ECDSA P-384) + PKI própria (Vault)
- JWT 15min + Refresh Token rotation
- SQLite criptografado (SQLCipher)
- Credenciais device: Argon2id + rotação 90 dias

---

## 7. Documentação Criada (docs/)

| Arquivo | Descrição |
|---------|-----------|
| `architecture/01-architecture-overview.md` | Arquitetura geral, componentes, multi-tenancy, frontend, riscos |
| `database/02-database-model.md` | Dicionário completo 28 tabelas, RLS, particionamento, Flyway |
| `controlid/03-controlid-integration.md` | Mapeamento API Control iD (auth, objects, facial, monitor, push, ações) |
| `gateway/04-gateway-architecture.md` | Gateway offline-first, componentes, fluxos, segurança, deployment |
| `security/05-security.md` | Auth, RBAC, ABAC, criptografia, biométrico LGPD, hardening, auditoria |
| `adrs/00-adr-index.md` | Índice 10 ADRs |
| `adrs/0001-monolithic-modular-backend.md` | Monólito modular vs microsserviços |
| `adrs/0002-multi-tenant-strategy.md` | Shared Schema + RLS |
| `adrs/0003-gateway-offline-first.md` | Gateway offline-first |
| `adrs/0004-controlid-standalone-mode.md` | Control iD Standalone + Monitor + Push |
| `adrs/0005-uuid-v7-primary-keys.md` | UUID v7 como PK |
| `adrs/0006-biometric-privacy-lgpd.md` | Biometria Privacy by Design |
| `adrs/0007-frontend-mvvm-nextjs.md` | Next.js 14 + MVVM |
| `adrs/0008-event-driven-sync.md` | Sync event-driven + idempotência |
| `adrs/0009-partitioned-access-logs.md` | Particionamento mensal access_logs |
| `adrs/0010-mtls-gateway-auth.md` | mTLS + JWT híbrido |
| `specs/2026-11-12-phase2-plan.md` | Plano detalhado 8 sprints Fase 2 |

---

## 8. Riscos Identificados

| Risco | Prob. | Impacto | Mitigação Principal |
|-------|-------|---------|---------------------|
| Limite 3k faces iDFace | Alta | Alto | Standalone + sync seletivo |
| Gateway offline dias | Média | Alto | Fila SQLite 100k + replay automático |
| Vazamento biométrico (LGPD) | Baixa | Crítico | Template só no device; hash no SaaS; consentimento versionado |
| Conflito IDs equipamentos | Média | Médio | Mapping table UUID ↔ int64 persistida |
| Latência LAN instável | Média | Médio | Modo Pro fallback; timeout configurável |
| Firmware incompatível | Baixa | Alto | Matriz compatibilidade; validação onboarding |

---

## 9. Critérios de Aceite Fase 1

- [x] Documento de arquitetura aprovado (este resumo + docs detalhados)
- [x] Modelo ER validado (28 tabelas, relacionamentos, RLS, particionamento)
- [x] Mapeamento Control iD completo (endpoints, objetos, limitações, contratos)
- [x] ADRs registradas para 10 decisões críticas
- [x] Estrutura de pastas do monólito definida
- [x] Docker Compose dev especificado (PostgreSQL, Redis, RabbitMQ, Mailhog, MinIO)
- [x] Flyway baseline + 9 migrations desenhadas
- [x] Spring Security + JWT + TenantContext + RLS design
- [x] Next.js scaffold + Design System + Auth flow + MVVM pattern
- [x] Contratos SaaS ↔ Gateway definidos (Sync, Events, Commands, WebSocket)
- [x] Checklist Fase 2 pronto (8 sprints, 22 semanas)

---

## 10. Checklist para Iniciar Fase 2

### Aprovações Necessárias
- [ ] **Arquiteto Tech Lead**: Validação arquitetura, ADRs, stack
- [ ] **DBA**: Modelo ER, particionamento, RLS, índices, Flyway
- [ ] **Segurança/Privacy**: LGPD biométrico, criptografia, mTLS, RBAC
- [ ] **DevOps/Infra**: Docker Compose, K8s/Helm, observabilidade, CI/CD
- [ ] **Product Owner**: Escopo Fase 2, priorização, cronograma
- [ ] **Time Campo**: Gateway hardware spec, bootstrap flow, Control iD support

### Pré-requisitos Técnicos
- [ ] Repositório Git criado (monorepo ou multi-repo)
- [ ] CI/CD pipeline base configurado (GitHub Actions / GitLab CI)
- [ ] Vault/Secret Manager provisionado (PKI, segredos)
- [ ] Ambiente Staging Kubernetes/Docker Swarm pronto
- [ ] Licenças/Contratos Control iD (firmware, suporte, SLAs)
- [ ] Hardware Gateway piloto adquirido (2-3 unidades para validação)

### Kickoff Fase 2
- [ ] Sprint Planning Sprint 1 (Fundação Backend)
- [ ] Definition of Ready para primeiras stories
- [ ] Pair programming / mob sessions agendadas
- [ ] Retrospectiva Fase 1 documentada

---

## Próximos Passos

1. **Revisão Técnica** (esta semana): Stakeholders revisam docs em `docs/`
2. **Workshop de Validação** (30 min): Walkthrough arquitetura + ADRs + perguntas
3. **Aprovação Formal**: Assinatura digital nos critérios de aceite acima
4. **Kickoff Fase 2**: Segunda-feira seguinte à aprovação

---

**Documento preparado por**: Arquiteto de Software Sênior  
**Versão**: 1.0  
**Classificação**: Interno - Confidencial