# ADR 0008: Sincronização: Event-Driven com Idempotência

## Status
Accepted

## Contexto
Sincronização bidirecional SaaS ↔ Gateway ↔ Devices:
- SaaS → Gateway: Usuários, faces, regras, comandos
- Gateway → SaaS: Eventos (access_logs, alarmas, catraca), status, resultados
- Gateway ↔ Devices: CRUD objects, Monitor events, Push commands

Requisitos:
- Confiabilidade em rede instável
- Exactly-once semantics (sem duplicatas)
- Ordenação causal (comando → resultado → evento)
- Auditoria completa

## Decisão
**Event-Driven Architecture com Idempotência Baseada em Chaves Naturais**:

### 1. Idempotency Keys (Todas Operações)
| Fluxo | Chave | Onde |
|-------|-------|------|
| SaaS → Gateway Sync | `syncId` (UUID v7) | `sync_state.last_checksum` |
| Gateway → Device Command | `idempotency_key` (UUID v7) | `device_commands.idempotency_key` UNIQUE |
| Device → Gateway Event | `eventId` (UUID v7 do device) | `device_events.id` PK |
| Gateway → SaaS Event | `eventId` (preservado) | `audit_logs.resource_id` |

### 2. Event Sourcing Leve (Gateway)
- `device_events` = log imutável de tudo que device envia
- `device_commands` = log de comandos enviados + status machine
- Processamento: `ProcessDeviceEventUseCase` lê `WHERE processed=false ORDER BY received_at`
- Após sucesso: `UPDATE ... SET processed=true`

### 3. Outbox Pattern (SaaS → Gateway)
```java
// SaaS: Transação única
@Transactional
public void publishSync(SyncPayload payload) {
    syncRepository.save(new SyncOutbox(syncId, payload)); // Tabela outbox
    // Commit → TransactionListener publica no RabbitMQ/Kafka
}

// Gateway: Consumer idempotente
@RabbitListener(queues = "gateway.sync")
public void handleSync(SyncMessage msg) {
    if (syncStateRepository.existsByChecksum(msg.checksum)) return; // Duplicate
    syncOrchestrator.execute(msg.payload);
    syncStateRepository.updateChecksum(msg.checksum);
}
```

### 4. Reconciliação Periódica
- **Daily**: SaaS compara `access_logs` count por device/gateway vs Gateway reportado
- **Weekly**: Full checksum comparison (usuários, faces, regras) → force full sync se drift
- **On-demand**: Admin trigger "Reconciliar" no portal

### 5. Dead Letter Queue (DLQ)
- Falha processamento evento → retry exponencial (1m, 5m, 15m, 1h, 6h, max 24h)
- Após 5 falhas → `device_events.processing_error` + alerta + DLQ table
- Admin pode: "Reprocessar", "Ignorar", "Corrigir payload"

## Consequências

### Positivas
- **Exactly-once** garantido por chaves naturais + constraints UNIQUE
- **Resiliência**: Retry automático + DLQ + reconciliação
- **Auditoria**: Rastro completo `eventId → commandId → syncId`
- **Ordenação**: `received_at` + `occurred_at` preservam causalidade

### Negativas
- **Complexidade**: Outbox, DLQ, reconciliação = mais código
- **Latência**: Eventual consistency (ms a minutos dependendo do caminho)
- **Storage**: `device_events` cresce (mitigado: particionamento + TTL)

### Riscos
- **Duplicate eventId** (device bug) → **Mitigação**: PK em `device_events.id`; dedup no SaaS por `(device_serial, eventId)`
- **Clock skew** device vs gateway → **Mitigação**: Usar `occurred_at` do device; gateway adiciona `received_at`; NTP obrigatório
- **Outbox table growth** → **Mitigação**: TTL 7 dias; purge job após publish confirmado

## Alternativas Consideradas
1. **Request-Response sincronizado** (REST blocking): Frágil; timeout; sem retry automático. Rejeitado.
2. **CDC (Debezium) no PostgreSQL**: Captura changes automaticamente. Overhead operacional; não cobre device→gateway. Rejeitado.
3. **gRPC streaming**: Baixa latência, mas complexo em gateway offline; HTTP/2 nem sempre em device. Rejeitado.

## Referências
- "Enterprise Integration Patterns" - Hohpe/Woolf (Outbox, Idempotent Receiver, DLQ)
- "Event-Driven Architecture" - Martin Fowler
- Transactional Outbox: https://microservices.io/patterns/data/transactional-outbox.html