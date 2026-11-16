# ADR 0002: Estratégia Multi-Tenant: Shared Schema + RLS

## Status
Accepted

## Contexto
SaaS multi-tenant com isolamento forte de dados (LGPD, segurança, compliance). Opções:
1. **Database per tenant**: Isolamento total, mas overhead operacional (migrations, backups, conexões)
2. **Schema per tenant**: Melhor que acima, mas ainda N schemas para gerenciar
3. **Shared schema + discriminator column (`tenant_id`)**: Um schema, RLS no PostgreSQL

Escala inicial: 1-10 tenants, <5k alunos/tenant. Crescimento esperado moderado.

## Decisão
**Shared Schema + Row Level Security (RLS) no PostgreSQL 16**:
- Todas as tabelas multi-tenant têm coluna `tenant_id UUID NOT NULL`
- RLS policies enforçam `tenant_id = current_setting('app.current_tenant')::uuid`
- Contexto de tenant injetado via `SET LOCAL app.current_tenant = 'uuid'` no início da transação
- `saas_plans`, `roles`, `permissions` são globais (read-only para tenants)
- Admins SaaS (`SAAS_ADMIN`) têm policy `USING (current_setting('app.current_role') = 'SAAS_ADMIN')`

## Consequências

### Positivas
- Operação simples: 1 DB, 1 pool de conexões, 1 migration path
- Isolamento garantido pelo **banco** (defesa em profundidade) — não depende só da aplicação
- Performance: índices compostos `(tenant_id, ...)` cobrem queries típicas
- Escalabilidade: read replicas, connection pooling (PgBouncer) funcionam nativamente
- Custo: 1 instância PostgreSQL vs N

### Negativas
- Vazamento de dados entre tenants só possível via bug no RLS ou `SAAS_ADMIN` malicioso
- Queries "global" (relatórios cross-tenant) requerem role especial ou `SET app.current_tenant = '0000...'`
- `tenant_id` em todas FKs/indexes aumenta storage (~16 bytes/linha)

### Riscos
- **RLS bypass** via `SECURITY DEFINER` functions → **Mitigação**: Auditar functions; evitar `SECURITY DEFINER` exceto helpers internos
- **Connection leak** com tenant errado → **Mitigação**: Pool valida `current_setting` no checkout; testes de integração multi-tenant
- **Performance** com muitos tenants grandes → **Mitigação**: Particionamento nativo futuro (`PARTITION BY LIST (tenant_id)`); sharding lógico

## Alternativas Consideradas
1. **Database per tenant**: Isolamento perfeito, mas 10x+ overhead operacional. Para 100+ tenants maybe. Rejeitado para Fase 1.
2. **Schema per tenant**: Melhor que DB/tenant, mas `search_path` manipulation é frágil; migrações complexas. Rejeitado.
3. **Application-level only (sem RLS)**: Confia só no código. Risco alto (bug = vazamento). Rejeitado.

## Referências
- PostgreSQL RLS: https://www.postgresql.org/docs/current/ddl-rowsecurity.html
- "Multi-Tenancy Patterns" - Microsoft Azure Architecture
- Spring Data RLS integration: `TransactionSynchronizationManager` + `SET LOCAL`