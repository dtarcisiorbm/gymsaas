# ADR 0003: Gateway Local: Offline-First com Sync Periódico

## Status
Accepted

## Contexto
Gateway roda em hardware dedicado por unidade (Raspberry Pi / Mini PC) na LAN da academia.
Requisitos:
- Comunicação com equipamentos Control iD (iDFace, iDBlock) via HTTP/HTTPS LAN
- Sincronização com SaaS Cloud (usuários, faces, regras, comandos)
- Operação contínua mesmo se internet cair (dias)
- Baixa latência para decisões de acesso (< 200ms)

## Decisão
**Arquitetura Offline-First**:
1. **Cache local completo** de políticas de acesso (SQLite + memória)
2. **Identificação local** nos equipamentos (Modo Standalone) — decisão < 500ms
3. **Sync periódico** com SaaS (pull a cada 15min + push WebSocket para comandos urgentes)
4. **Fila persistente** de eventos (access_logs, alarm_logs) em SQLite → replay quando online
5. **Heartbeat** `device_is_alive` monitora conectividade SaaS

## Consequências

### Positivas
- **Resiliência**: Academia opera normal sem internet (catraca abre, facial funciona)
- **Latência**: Decisão de acesso no device (Standalone) = ~100-300ms
- **Simplicidade**: Sem necessidade de VPN, túnel, ou conexão permanente SaaS→Gateway
- **Custo**: Hardware barato (RPi 4/5 ~$100); não precisa cloud edge

### Negativas
- **Consistência eventual**: Mudança de regra/plano leva até 15min para propagar
- **Conflitos**: Enroll facial offline no device vs SaaS → resolução "last write wins" com timestamp
- **Storage local**: SQLite cresce se offline prolongado (limite 100k events ~ 50MB)

### Riscos
- **Firmware mismatch**: Device com firmware antigo não suporta API nova → **Mitigação**: Matriz compatibilidade; validação no sync; fallback graceful
- **Certificado expirado offline** → **Mitigação**: Renovação 30d antes; cache de CRL; alerta local (LED/buzzer)
- **Perda de eventos** (SD card failure) → **Mitigação**: SSD em vez de SD; backup diário config no SaaS; health check disk

## Alternativas Consideradas
1. **Online-only (Modo Pro/Enterprise)**: Decisão no SaaS. Latência LAN + Cloud > 500ms; falha internet = catraca parada. Rejeitado.
2. **VPN permanente (WireGuard/Tailscale)**: Complexidade operacional; single point of failure no servidor VPN. Rejeitado.
3. **Edge Computing (AWS Greengrass / Azure IoT Edge)**: Overkill; vendor lock-in; custo. Rejeitado.

## Referências
- Control iD Modos de Operação: https://www.controlid.com.br/docs/access-api-pt/modos-de-operacao/
- "Offline-First Architecture" - Martin Fowler
- SQLite WAL mode para concorrência: https://www.sqlite.org/wal.html