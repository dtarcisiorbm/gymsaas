# ADR 0001: Backend Monólito Modular vs Microsserviços

## Status
Accepted

## Contexto
Sistema SaaS para academias com escopo inicial pequeno (1-10 tenants, <5k alunos). Necessidade de:
- Velocidade de desenvolvimento e deploy
- Simplicidade operacional (um time, poucos ambientes)
- Consistência transacional entre domínios (tenant, student, access, billing)
- Evolução futura para microsserviços se escala demandar

## Decisão
**Monólito Modular (Modulith)** com Spring Boot 3.x:
- Módulos isolados por domínio (`gymsaas-tenant`, `gymsaas-student`, `gymsaas-access`, etc)
- Comunicação interna via **domain events** (Spring ApplicationEvent) — sem acoplamento temporal
- Banco único (PostgreSQL) com **schema compartilhado + RLS**
- Deploy único (JAR/Docker); feature flags para rollout gradual
- Fronteiras de módulo enforçadas por **ArchUnit** nos testes

## Consequências

### Positivas
- Deploy simples: um artefato, uma pipeline, zero service discovery
- Transações ACID naturais entre domínios (ex: matrícula + plano + pagamento)
- Refatoração segura: módulos podem virar microsserviços depois (event-driven boundary)
- Debugging/trancing trivial (single process)
- Menor latência intra-processo

### Negativas
- Escala vertical apenas (single instance ou active-passive)
- Falha em um módulo pode derrubar todo o sistema (mitigado: circuit breakers internos, health checks por módulo)
- Time coupling no deploy (mitigado: feature flags, database migrations backward-compatible)
- Tecnologia única (Java) — OK para time atual

### Riscos
- Crescimento descontrolado de acoplamento → **Mitigação**: ArchUnit + code review + métricas de acoplamento
- Gargalo de banco → **Mitigação**: Read replicas, connection pooling, query optimization, futuro sharding por tenant

## Alternativas Consideradas
1. **Microsserviços desde o início**: Overhead operacional enorme (K8s, service mesh, distributed tracing, saga patterns) para escala que não existe. Rejeitado.
2. **Serverless (AWS Lambda + API Gateway)**: Cold starts, vendor lock-in, debugging difícil, custo imprevisível. Rejeitado.
3. **Modulith com banco por módulo**: Complexidade de transações distribuídas (Saga) sem necessidade. Rejeitado.

## Referências
- Spring Modulith reference: https://spring.io/projects/spring-modulith
- "Modulithic Architecture" - Eberhard Wolff
- ArchUnit: https://www.archunit.org/