# GymSaaS — Documentação do Projeto

Plataforma SaaS multi-tenant para gestão de academias, desenvolvida como **monólito modular** com backend em **Java 21 / Spring Boot 3.5** e frontend em **Next.js 14 (App Router) / TypeScript**. O projeto prioriza isolamento de dados por academia (tenant), segurança, trilha de auditoria e evolução incremental por fases.

> Este documento descreve o estado atual do repositório e serve como referência para publicação no GitHub e para continuidade do desenvolvimento.

---

## Sumário

- [Visão geral](#visão-geral)
- [Stack e arquitetura](#stack-e-arquitetura)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Módulos e funcionalidades implementadas](#módulos-e-funcionalidades-implementadas)
- [Segurança, multi-tenancy e RBAC](#segurança-multi-tenancy-e-rbac)
- [Banco de dados e migrations](#banco-de-dados-e-migrations)
- [API REST](#api-rest)
- [Frontend](#frontend)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Coleção Postman](#coleção-postman)
- [Documentação adicional](#documentação-adicional)
- [Limitações e próximos passos](#limitações-e-próximos-passos)

---

## Visão geral

O GymSaaS é um sistema de gestão para redes/academias com múltiplas unidades (filiais), contemplando:

- **Multi-tenancy**: cada academia (tenant) possui seus dados isolados.
- **Autenticação e sessão**: login por e-mail/senha, JWT (access token) e refresh token rotativo em cookie HttpOnly.
- **RBAC**: papéis e permissões granulares por academia, com administrador de plataforma.
- **Gestão administrativa**: academias (plataforma), unidades, usuários/papéis/vínculos.
- **Gestão de alunos**: cadastro completo, documentos, contatos, endereço, vínculos com unidades, transferências, histórico e observações (Fase 4).
- **Planos, matrículas e contratos (backend)**: módulos de domínio/aplicação/infra/apresentação, regras de vigência, congelamento, renovação, versionamento de contrato/PDF e aceite simples (Fase 5 — backend; frontend dedicado pendente conforme estado atual do repositório).

**Objetivo de longo prazo (conforme documentação do projeto):** evolução para operação completa de academias, incluindo módulos financeiros e integrações (ex.: controle de acesso/catracas/biometria em terminais Control iD) em fases futuras.

---

## Stack e arquitetura

### Backend
- **Java 21**, **Spring Boot 3.5**, **Maven**
- **Spring Security** (OAuth2 Resource Server / JWT), **RBAC** com `@PreAuthorize`
- **Spring Data JPA / Hibernate**
- **PostgreSQL 16** com **Row Level Security (RLS)**
- **Flyway** para versionamento de schema
- **OpenAPI/Swagger** (springdoc)
- **JUnit 5, Mockito, Testcontainers** (PostgreSQL real nos testes de integração)
- **OpenPDF** (geração de contratos em PDF, módulo de contratos)

### Frontend
- **Next.js 14** (App Router), **React 18**, **TypeScript**
- **Tailwind CSS**
- Organização em camadas **MVVM** por feature (`model`, `data`, `viewmodel`, telas em `app/`)
- Tema **claro/escuro** com persistência

### Infraestrutura
- **Docker / Docker Compose** (PostgreSQL, backend, frontend)
- Dois papéis de banco: **owner** (Flyway/migrações) e **app** de runtime (`NOSUPERUSER`, `NOBYPASSRLS`) para efetividade do RLS
- Volume dedicado para armazenamento de documentos de contrato (`gymsaas_contract_storage`)

### Arquitetura
- **Monólito modular** com separação por contexto/domínio: `tenant`, `branch`, `identity`, `rbac`, `audit`, `student`, `plan`, `membership`, `contract`, `shared`.
- Cada módulo segue **Clean Architecture** (camadas `domain`, `application`, `infrastructure`, `presentation`) onde aplicável.
- **TenantContext**: o tenant ativo é derivado exclusivamente do contexto autenticado (JWT), nunca de parâmetros enviados pelo cliente.

---

## Estrutura do repositório

```text
.
├── backend/                  # API Spring Boot (Maven)
│   ├── src/main/java/com/gymsaas/
│   │   ├── audit/
│   │   ├── branch/
│   │   ├── contract/
│   │   ├── identity/         # autenticação e usuários
│   │   ├── membership/
│   │   ├── plan/
│   │   ├── rbac/
│   │   ├── shared/           # segurança, tenant, erros, utilitários
│   │   ├── student/
│   │   └── tenant/
│   ├── src/main/resources/db/migration/  # V1..V12 (Flyway)
│   └── src/test/             # testes unitários e integração (*IT, Testcontainers)
├── frontend/                 # Next.js (App Router)
│   └── src/
│       ├── app/              # rotas/páginas
│       ├── components/
│       ├── core/             # http, sessão, mvvm, tema, validação
│       └── features/         # auth, tenants, branches, users, students (MVVM)
├── docker/postgres/init/     # inicialização do banco (roles)
├── docs/                     # documentação, ADRs, specs, segurança, banco, etc.
├── postman/                  # coleção Postman da API
├── docker-compose.yml
├── .env.example
├── README.md
└── DOCUMENTACAO.md
```

---

## Módulos e funcionalidades implementadas

### 1. Autenticação e sessão (`identity`, `shared`)
- Login (`POST /api/auth/login`) com e-mail/senha; senhas com **Argon2id**.
- **Access token JWT RS256** (curta duração, mantido em memória no frontend).
- **Refresh token opaco** em cookie `HttpOnly` (path `/api/auth`), rotativo, persistido como hash; detecção de reuso com revogação da família.
- Seleção de academia ativa (`/api/auth/select-tenant`) para usuários multi-vínculo.
- Logout com revogação e invalidação; cache de revogação de access token.
- Endpoints: login, refresh, logout, `me`, tenants do usuário, select-tenant.

### 2. Administração de plataforma e academias (`tenant`)
- CRUD e ciclo de vida de academias (admin plataforma): `PENDING`, `ACTIVE`, `SUSPENDED`, `INACTIVE`.
- Consulta de dados da academia ativa e atualização cadastral (`/api/tenants/current`).
- Apenas academias `ACTIVE` permitem login/seleção (conforme regras implementadas).

### 3. Unidades/filiais (`branch`)
- CRUD de unidades por tenant, status `ACTIVE`/`INACTIVE`, configurações operacionais (horários, capacidade, timezone, flags, settings).
- Consulta de unidades por administrador de plataforma no contexto de uma academia.

### 4. Usuários e RBAC (`identity`, `rbac`)
- Gestão de usuários do tenant: criação/vínculo, atualização, status (`ACTIVE`/`INACTIVE`/`LOCKED`), atribuição de papéis e vínculos com unidades.
- Papéis e permissões granulares (`role:read`, `permission:read`, `users:*`, `branches:*`, `tenants:*`, `students:*`, `plans:*`, `memberships:*`, `contracts:*`, `audit:read`, etc., com compatibilidade plural/singular onde previsto).
- Administrador de plataforma (`is_platform_admin` / claim `plat`) com acesso administrativo global.

### 5. Auditoria (`audit`)
- Registro append-only de eventos relevantes (ex.: autenticação, operações sensíveis) consultável via `/api/audit-logs` (paginado, RBAC).

### 6. Alunos (`student`) — Fase 4
- Cadastro, consulta, listagem paginada/filtrada/ordenada, atualização, alteração de status e soft delete.
- Status cadastral: `PENDING`, `ACTIVE`, `INACTIVE`, `BLOCKED`, `ARCHIVED` (histórico preservado).
- Documentos (CPF validado/normalizado, unicidade por tenant entre não excluídos; mascaramento por padrão, revelação com permissão sensível), contatos, endereço.
- Vínculos com unidades (múltiplos, com unidade principal), transferência preservando histórico, observações administrativas.
- Matrícula do aluno gerada por contador atômico por tenant/ano (`AAAA` + sequência).
- Tenant do aluno imutável; operações sempre no contexto autenticado.

### 7. Planos, matrículas e contratos (`plan`, `membership`, `contract`) — Fase 5 (backend)
- **Planos**: definição, duração, política de congelamento, termos contratuais, benefícios (features), status; histórico de preços versionado (adição/cancelamento de preço programado) sem alterar matrículas existentes.
- **Matrículas**: cotação (`quote`) e criação com snapshot de condições/preço calculado no backend, atualização operacional, renovação (nova matrícula vinculada), cancelamento, suspensão/reativação, congelamento/descongelamento, histórico; regras de vigência e impactos conforme domínio implementado.
- **Contratos**: geração versionada por matrícula, histórico, consulta, download de PDF (armazenado fora do banco, volume dedicado) com integridade (hash) e registro de **aceite simples** (evidências; não constitui assinatura digital qualificada).
- **Job de ciclo de vida**: processamento agendado de matrículas (ativação/expiração/descongelamento conforme configuração) considerando fuso do tenant.
- *Observação*: o frontend possui telas consolidadas até a gestão de alunos; interfaces dedicadas de planos/matrículas/contratos podem estar pendentes conforme evolução do repositório.

---

## Segurança, multi-tenancy e RBAC

- **Tenant sempre do JWT/contexto** (`TenantContext`), nunca de payload/header do cliente.
- **RLS no PostgreSQL** com políticas por tenant e helpers de sessão (`app.current_tenant`, `app.current_user`, `app.rls_bypass`, admin plataforma), efetivo com role de runtime sem `BYPASSRLS`.
- **Constraints e FKs compostas** (incluindo `tenant_id`) para integridade e isolamento.
- **RBAC**: permissões verificadas na API (`@PreAuthorize`/authorities no token) e refletidas na navegação/UX do frontend quando aplicável.
- **Proteção de dados sensíveis**: mascaramento de documentos/CPF por padrão, acesso integral condicionado a permissão específica; CPF não deve ser registrado em logs; diretrizes de retenção em `docs/security/06-student-data-retention.md`.
- **Autenticação robusta**: Argon2id, refresh rotativo com detecção de reuso, cookies HttpOnly, CORS configurável.

---

## Banco de dados e migrations

Migrations Flyway em `backend/src/main/resources/db/migration` (aplicar sempre via Flyway; **nunca editar migrations já aplicadas**):

| Versão | Descrição (resumo) |
|---|---|
| V1 | Extensões, funções utilitárias (UUID v7, helpers RLS, `updated_at`) |
| V2 | Tenants, usuários, papéis, permissões e vínculos |
| V3 | Refresh tokens e auditoria (append-only) |
| V4 | Políticas RLS (núcleo multi-tenant/RBAC) |
| V5 | Seeds de papéis/permissões de sistema |
| V6 | Unidades (`branches`), configurações, vínculos usuário-unidade, admin plataforma, ajustes de tenant/RLS |
| V7 | Permissões plurais Fase 3 e concessões por papel |
| V8 | Alunos, documentos, contatos, endereços, contador de matrícula e RLS |
| V9 | Vínculos aluno-unidade, histórico de status, observações, permissões `students:*` e RLS |
| V10 | `tenants.timezone`, planos, preços, benefícios e permissões `plans:*` |
| V11 | Matrículas, congelamentos, histórico, contador e permissões `memberships:*` |
| V12 | Contratos, aceites e permissões `contracts:*` |

**Credenciais típicas (dev/docker):** `gymsaas_owner` (migrações) e `gymsaas_app` (runtime). RLS depende do uso da role de runtime sem privilégios de bypass.

---

## API REST

- Base: `/api`
- Documentação interativa: **Swagger UI** em `/swagger-ui.html` (com backend ativo)
- Padrões: JSON, DTOs com validação (Bean Validation), paginação/filtros em listagens, erros padronizados, `Authorization: Bearer <accessToken>` para rotas protegidas.

### Principais grupos de endpoints

| Grupo | Base | Exemplos |
|---|---|---|
| Autenticação | `/api/auth` | login, refresh, logout, me, tenants, select-tenant |
| Admin plataforma | `/api/admin/tenants` | CRUD/status de academias, unidades por academia |
| Tenant ativo | `/api/tenants/current` | dados e atualização da academia ativa |
| Unidades | `/api/branches` | CRUD, status, settings |
| Usuários | `/api/users` | CRUD, status, papéis, unidades do usuário |
| RBAC | `/api/roles`, `/api/permissions` | consulta de papéis e permissões |
| Auditoria | `/api/audit-logs` | consulta paginada |
| Alunos | `/api/students` | CRUD, status, histórico, documentos, contatos, endereço, unidades, transferências, notas |
| Planos | `/api/plans` | CRUD, status, preços/histórico, features |
| Matrículas | `/api/memberships` | listagem, quote, criação, atualização, renovação, cancelamento, congelamento/suspensão/reativação, histórico |
| Contratos | `/api/memberships/{id}/contracts`, `/api/contracts` | geração, histórico, detalhe, PDF, aceite |

> Para o contrato completo de campos e respostas, utilize a coleção Postman (`postman/GymSaaS-API.postman_collection.json`) e/ou o Swagger.

---

## Frontend

### Rotas principais (App Router)

| Rota | Descrição | Acesso típico |
|---|---|---|
| `/login` | Autenticação | público |
| `/select-tenant` | Seleção de academia ativa | autenticado (multi-tenant) |
| `/dashboard` | Visão geral da sessão/permissões | autenticado |
| `/admin/tenants` | Gestão de academias | admin plataforma |
| `/admin/tenants/[tenantId]` | Detalhe da academia e unidades | admin plataforma |
| `/academy/branches` | Gestão de unidades e configurações | gestão da academia |
| `/academy/users` | Usuários, papéis e vínculos | gestão da academia |
| `/academy/settings` | Dados da academia ativa | gestão da academia |
| `/academy/students` | Listagem e gestão de alunos | conforme permissão (`students:read`) |
| `/academy/students/new` | Cadastro de aluno | conforme permissão |
| `/academy/students/[id]` | Detalhe do aluno (abas) | conforme permissão |
| `/academy/students/[id]/edit` | Edição do aluno | conforme permissão |

### Características de UX
- Layout responsivo, estados de carregamento/vazio/erro, paginação e filtros onde aplicável.
- Tema claro/escuro com toggle persistido.
- Sessão com access token em memória e renovação via refresh.

---

## Como executar

### Pré-requisitos
- **Docker** e **Docker Compose** (recomendado para subir a stack completa)
- Opcional para dev local: **Java 21 + Maven Wrapper**, **Node.js 20+ / npm**, **PostgreSQL 16**

### Stack completa com Docker Compose

```bash
# 1) Configurar variáveis (ajuste conforme necessário)
cp .env.example .env

# 2) Subir serviços (build + execução em background)
docker compose up -d --build

# 3) Acompanhar logs (opcional)
docker compose logs -f backend
```

Serviços padrão:

| Serviço | URL |
|---|---|
| Frontend | http://localhost:3000 |
| Backend | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Health | http://localhost:8080/actuator/health |
| PostgreSQL | localhost:5432 |

Encerrar:
```bash
docker compose down        # mantém volumes
docker compose down -v     # remove volumes (apaga dados)
```

### Desenvolvimento local

**Banco de dados (Docker):**
```bash
docker compose up -d postgres
```

**Backend:**
```bash
cd backend
# executar aplicação (usa variáveis DB/FLYWAY conforme ambiente)
./mvnw spring-boot:run

# build + testes
./mvnw verify
```

> Sem `JWT_PRIVATE_KEY`/`JWT_PUBLIC_KEY`, um par RSA efêmero pode ser gerado no boot (útil em dev, mas tokens deixam de ser válidos após reinício). Em produção, configure chaves PEM.

**Frontend:**
```bash
cd frontend
npm install
NEXT_PUBLIC_API_URL=http://localhost:8080 npm run dev
```

### Usuários de demonstração
Criados quando `DEMO_DATA_ENABLED=true` (padrão no `.env.example`):

| Usuário | Senha | Perfil |
|---|---|---|
| `superadmin@gymsaas.local` | `SuperAdmin@123` | Administrador da plataforma (`SAAS_ADMIN` / `plat`) |
| `admin@gymsaas.local` | `Admin@123` | Administrador da academia demo (`TENANT_ADMIN`) |
| `reception@gymsaas.local` | `Reception@123` | Recepção (`RECEPTIONIST`) |

---

## Variáveis de ambiente

Baseie-se em `.env.example`. Principais variáveis:

| Variável | Descrição |
|---|---|
| `POSTGRES_DB`, `POSTGRES_PORT` | Banco e porta do PostgreSQL |
| `POSTGRES_OWNER_USER`, `POSTGRES_OWNER_PASSWORD` | Credencial owner (Flyway/migrações) |
| `POSTGRES_APP_USER`, `POSTGRES_APP_PASSWORD` | Credencial runtime da aplicação (RLS) |
| `BACKEND_PORT` | Porta exposta do backend |
| `FRONTEND_PORT` | Porta exposta do frontend |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (ex.: `http://localhost:3000`) |
| `DEMO_DATA_ENABLED` | Habilita dados de demonstração |
| `NEXT_PUBLIC_API_URL` | URL da API consumida pelo frontend |
| `COOKIE_SECURE` | Cookie seguro (habilitar em HTTPS/prod) |
| `JWT_PRIVATE_KEY`, `JWT_PUBLIC_KEY` | Chaves PEM (opcionais em dev; recomendadas em prod) |
| `JAVA_OPTS` | Opções da JVM do container backend |
| `CONTRACT_STORAGE_DIR` | Diretório de armazenamento de contratos no container (mapeado para volume) |

---

## Testes

- **Unitários:** validações/utilitários, codecs/tokens, regras de domínio e fronteiras arquiteturais (ex.: ArchUnit), entre outros.
- **Integração (`*IT`) com Testcontainers:** execução contra PostgreSQL real (role de runtime) cobrindo autenticação/sessão, RBAC, RLS/isolamento multi-tenant, gestão de tenants/branches/usuários e módulo de alunos (incluindo cenários de concorrência e constraints).

Executar no backend:
```bash
cd backend
./mvnw verify
```

> O resultado pode variar conforme evolução do código; no estado inspecionado, o baseline de integração apresentou testes verdes com Flyway até a versão mais recente disponível no repositório.

---

## Coleção Postman

Uma coleção pronta para testes manuais da API está disponível em:

- `postman/GymSaaS-API.postman_collection.json`

**Dicas de uso:**
1. Importe a coleção no Postman e ajuste `baseUrl` (padrão `http://localhost:8080`).
2. Execute **Auth > Login** para obter e salvar automaticamente o `accessToken` nas variáveis da coleção.
3. Mantenha cookies habilitados para suportar o fluxo de **refresh token** (`HttpOnly`).
4. Preencha variáveis de IDs (`tenantId`, `branchId`, `studentId`, `planId`, `membershipId`, `contractId`, etc.) conforme os recursos criados.

---

## Documentação adicional

Consulte a pasta `docs/` para materiais complementares, incluindo:

- `docs/00-FASE1-SUMMARY.md` e artefatos de arquitetura/banco/gateway/Control iD
- `docs/adrs/` — decisões arquiteturais
- `docs/specs/` — planos, ledgers e relatórios de fases (ex.: Fase 4, Fase 5)
- `docs/security/06-student-data-retention.md` — retenção e proteção de dados de alunos (LGPD)

---

## Limitações e próximos passos

### Limitações conhecidas (estado atual)
- O repositório pode não conter histórico Git completo neste ambiente; recomenda-se versionar/commitar adequadamente antes da publicação.
- Módulos de **planos/matrículas/contratos** possuem implementação backend relevante, porém a **experiência frontend dedicada** para esses fluxos pode estar parcial/pendente.
- Funcionalidades tipicamente associadas a fases futuras (ex.: gateway de pagamento, cobrança automática, conciliação, emissão fiscal, integração física/biometria com Control iD, motor de catracas) **não** fazem parte do escopo implementado neste estado.
- Recomenda-se executar smoke tests end-to-end no ambiente Docker antes de releases.

### Próximos passos sugeridos
- Consolidar e publicar interfaces frontend para planos, matrículas e contratos (quando aplicável).
- Expandir cobertura de testes end-to-end (API/UI) e pipelines de CI.
- Evoluir observabilidade, hardening de produção (segredos, chaves JWT, cookies seguros/TLS, backups) e rotinas operacionais de retenção/anonimização conforme política.
- Planejar e implementar módulos financeiros e integrações de controle de acesso em fases subsequentes, respeitando o isolamento multi-tenant e as decisões arquiteturais do projeto.
