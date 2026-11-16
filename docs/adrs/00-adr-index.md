# Architecture Decision Records (ADRs) — Índice

| ADR | Título | Status | Data |
|-----|--------|--------|------|
| [0001](0001-monolithic-modular-backend.md) | Backend Monólito Modular vs Microsserviços | Accepted | 2026-11-12 |
| [0002](0002-multi-tenant-strategy.md) | Estratégia Multi-Tenant: Shared Schema + RLS | Accepted | 2026-11-12 |
| [0003](0003-gateway-offline-first.md) | Gateway Local: Offline-First com Sync Periódico | Accepted | 2026-11-12 |
| [0004](0004-controlid-standalone-mode.md) | Control iD: Modo Standalone + Monitor + Push Híbrido | Accepted | 2026-11-12 |
| [0005](0005-uuid-v7-primary-keys.md) | UUID v7 como Chave Primária Global | Accepted | 2026-11-12 |
| [0006](0006-biometric-privacy-lgpd.md) | Biometria: Privacy by Design + LGPD Compliance | Accepted | 2026-11-12 |
| [0007](0007-frontend-mvvm-nextjs.md) | Frontend: MVVM com Next.js 14 App Router | Accepted | 2026-11-12 |
| [0008](0008-event-driven-sync.md) | Sincronização: Event-Driven com Idempotência | Accepted | 2026-11-12 |
| [0009](0009-partitioned-access-logs.md) | Access Logs: Particionamento Mensal + Arquivamento | Accepted | 2026-11-12 |
| [0010](0010-mtls-gateway-auth.md) | Autenticação Gateway ↔ SaaS: mTLS + JWT Híbrido | Accepted | 2026-11-12 |

---

## Template para Novas ADRs

```markdown
# ADR {NNNN}: {Título Curto}

## Status
{Proposed | Accepted | Rejected | Superseded by ADR-NNNN}

## Contexto
{Descrever a situação, problema, restrições, forças em jogo}

## Decisão
{Descrever a escolha feita, com suficiente detalhe para implementação}

## Consequências
### Positivas
- ...
### Negativas
- ...
### Riscos
- ...

## Alternativas Consideradas
1. **{Alternativa 1}**: {Por que não escolhida}
2. **{Alternativa 2}**: {Por que não escolhida}

## Referências
- Links para docs, issues, discussões
```

---

## Processo de Governança

1. **Proposta**: Qualquer arquiteto/senior dev abre PR com ADR em `Proposed`
2. **Discussão**: 5 dias úteis para comentários (async no PR)
3. **Decisão**: Tech Lead + Arquiteto aprovam → `Accepted`
4. **Implementação**: Referenciar ADR em código, docs, PRs
5. **Revisão**: Anual ou quando contexto mudar significativamente