# ADR 0005: UUID v7 como Chave Primária Global

## Status
Accepted

## Contexto
Sistema distribuído (SaaS + Gateways + Devices) exige IDs globais únicos, ordenáveis e não-sequenciais (segurança).
Opções:
- UUID v4 (random): Não ordenável; fragmentação de índice B-tree
- UUID v1 (timestamp + MAC): Ordenável, mas expõe MAC; não criptograficamente seguro
- UUID v6/v7 (timestamp-ordered): Ordenável, melhor localidade de índice
- ULID: Similar a UUID v7, mas 26 chars vs 36
- Sequence/BigInt: Sequencial, previsível, não global

## Decisão
**UUID v7 (RFC 9562)** como PK padrão em todas as tabelas:
- Geração: `gen_random_uuid_v7()` (extensão `uuid-ossp` ou implementação custom)
- Formato: `018f8e4a-7c3b-7cc0-8c4a-7c3b7cc08c4a` (timestamp 48 bits + random 74 bits)
- Bibliotecas: `com.fasterxml.uuid:java-uuid-generator` ou `io.github.f4b:uuid-creator`

## Consequências

### Positivas
- **Ordenação natural**: `ORDER BY id` ≈ `ORDER BY created_at` — evita coluna extra em muitos casos
- **Index locality**: Inserções append-only no B-tree (menos page splits, melhor cache)
- **Global uniqueness**: Sem coordenação central (gateways offline geram IDs válidos)
- **Não sequencial**: Não expõe volume de dados (vs bigint auto-increment)
- **Compatibilidade**: `uuid` nativo no PostgreSQL; JSON serializável padrão

### Negativas
- **Tamanho**: 16 bytes (vs 8 bytes bigint) — 2x storage em PK/FK
- **Geração**: Ligeiramente mais lento que `gen_random_uuid()` (v4) — negligenciável
- **Debugging**: Menos legível que bigint sequencial

### Riscos
- **Clock skew** entre nodes → **Mitigação**: UUID v7 usa timestamp Unix ms + contador monotônico por gerador; tolera skew < 1ms
- **Collision** (teórico) → **Mitigação**: 74 bits random = 2^74 espaço; risco zero prático

## Alternativas Consideradas
1. **UUID v4**: Simples, mas fragmenta índices (page splits 50%+ em alta carga). Rejeitado.
2. **ULID**: 26 chars, case-insensitive, ordenável. Mas não é UUID padrão; menos suporte nativo PG. Rejeitado.
3. **BigInt + Shard ID**: `id = (shard_id << 56) | sequence`. Requer coordenação de shards; complexo. Rejeitado.
4. **TSID (Twitter Snowflake variant)**: Similar a ULID. Mesmo problema de padronização. Rejeitado.

## Referências
- RFC 9562: https://www.rfc-editor.org/rfc/rfc9562.html
- "UUIDs are not as bad as you think" - Percona Blog
- PostgreSQL `uuid` type: https://www.postgresql.org/docs/current/datatype-uuid.html