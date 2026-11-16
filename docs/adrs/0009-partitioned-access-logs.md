# ADR 0009: Access Logs: Particionamento Mensal + Arquivamento

## Status
Accepted

## Contexto
`access_logs` é tabela de maior volume:
- Escala: 10 academias × 5 unidades × 1.000 alunos × 4 check-ins/dia = ~200k logs/dia
- Crescimento: ~6M/mês, ~72M/ano
- Queries típicas: "últimos 30 dias da unidade X", "histórico do aluno Y", "dashboard tempo real"
- Retenção por plano: Starter 90d, Pro 1 ano, Enterprise 5 anos
- LGPD: Direito à exclusão/anonymização

## Decisão
**Particionamento Nativo PostgreSQL (Range Partitioning) por `occurred_at` mensal**:

### 1. Estrutura
```sql
CREATE TABLE access_logs (
    id uuid NOT NULL DEFAULT gen_random_uuid_v7(),
    tenant_id uuid NOT NULL,
    branch_id uuid NOT NULL,
    device_id uuid NOT NULL,
    student_id uuid,
    -- ... demais colunas ...
    occurred_at timestamptz NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    created_at timestamptz NOT NULL DEFAULT now()
) PARTITION BY RANGE (occurred_at);

-- Partições automáticas via pg_partman ou job mensal
CREATE TABLE access_logs_2026_11 PARTITION OF access_logs
    FOR VALUES FROM ('2026-11-01') TO ('2026-12-01');
CREATE TABLE access_logs_2026_12 PARTITION OF access_logs
    FOR VALUES FROM ('2026-12-01') TO ('2027-01-01');
-- ...
```

### 2. Índices (Locais por Partição)
```sql
-- Cada partição tem seus índices locais
CREATE INDEX ON access_logs_2026_11 (tenant_id, occurred_at DESC);
CREATE INDEX ON access_logs_2026_11 (branch_id, occurred_at DESC);
CREATE INDEX ON access_logs_2026_11 (student_id, occurred_at DESC);
CREATE INDEX ON access_logs_2026_11 (device_id, occurred_at DESC);
CREATE INDEX ON access_logs_2026_11 (event_type);
```

### 3. Retenção e Arquivamento
| Plano | Retenção Online | Arquivamento |
|-------|-----------------|--------------|
| Starter | 90 dias (3 partições) | `pg_dump` partições antigas → S3 Glacier (Parquet) |
| Professional | 1 ano (12 partições) | Same + Athena/Trino para queries históricas |
| Enterprise | 5 anos (60 partições) | Same + tabela `access_logs_archive` particionada anual |

**Job Mensal (pg_cron)**:
```sql
-- 1. Detach partição antiga
ALTER TABLE access_logs DETACH PARTITION access_logs_2025_11;
-- 2. Export para S3 (COPY TO PROGRAM 'aws s3 cp - s3://bucket/archive/...')
-- 3. Drop ou mover para schema archive
DROP TABLE access_logs_2025_11; -- ou ALTER TABLE ... SET SCHEMA archive;
```

### 4. Queries Cross-Partition (Raras)
- Dashboard "todos tenants" → `SET enable_partition_pruning = off;` + índice global (se criado)
- Admin SaaS apenas; uso controlado

## Consequências

### Positivas
- **Performance**: Partition pruning = scans apenas partições relevantes (ex: últimos 30 dias = 1 partição)
- **Manutenção**: `DROP PARTITION` instantâneo vs `DELETE` lento + vacuum
- **Storage**: Partições antigas em storage barato (S3 Glacier ~$1/TB/mês)
- **Backup**: Backup incremental por partição; restore point-in-time granular

### Negativas
- **PK não única globalmente** (uuid v7 é, mas PG exige PK inclua partition key) → **Solução**: PK `(id, occurred_at)`; `id` único via `EXCLUDE` constraint ou trigger
- **Foreign Keys** para tabela particionada não suportadas nativamente → **Solução**: FKs lógicas (aplicação) + triggers de validação se crítico
- **Complexidade**: pg_partman ou job custom para criar partições antecipadamente

### Riscos
- **Partição faltando** (insert em data futura/sem partição) → **Mitigação**: Job cria partições 3 meses à frente; trigger `ON INSERT` cria sob demanda (com lock)
- **Índices globais** não suportados → **Mitigação**: Índices locais cobrem 99% queries; queries admin usam `enable_partition_pruning=off`
- **pg_upgrade** com partições → **Mitigação**: Testado em staging; pg_partman suporta

## Alternativas Consideradas
1. **Tabela única + BRIN index**: `CREATE INDEX ON access_logs USING BRIN (occurred_at)` — bom para append-only, mas `DELETE` antigo não libera espaço; vacuum pesado. Rejeitado.
2. **TimescaleDB (hypertables)**: Excelente para time-series, mas adiciona dependência; PG nativo suficiente. Rejeitado.
3. **Sharding lógico por tenant**: `access_logs_tenant_{id}` — complexidade de routing; cross-tenant queries impossíveis. Rejeitado.
4. **MongoDB/ClickHouse para logs**: Polyglot persistence overhead; PG atende bem. Rejeitado.

## Referências
- PostgreSQL Partitioning: https://www.postgresql.org/docs/current/ddl-partitioning.html
- pg_partman: https://github.com/pgpartman/pg_partman
- "Time-Series Data in PostgreSQL" - Timescale Blog (concepts apply to native partitioning)