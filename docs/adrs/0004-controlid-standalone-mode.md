# ADR 0004: Control iD: Modo Standalone + Monitor + Push Híbrido

## Status
Accepted

## Contexto
Equipamentos: iDFace (terminal facial) + iDBlock (catraca).
API Control iD oferece 3 modos:
- **Standalone**: Identificação + autorização local no terminal
- **Pro**: Identificação local, autorização no servidor (eventos `new_user_identified`)
- **Enterprise**: Identificação + autorização no servidor (eventos biométricos brutos)

Limitações críticas:
- iDFace suporta ~3.000 templates faciais
- Academia pode ter 5.000+ alunos ativos
- Rede LAN instável em alguns clientes

## Decisão
**Modo Standalone (primário) + Monitor (eventos) + Push (comandos)**:

| Função | Mecanismo |
|--------|-----------|
| Identificação facial | Local no iDFace (Standalone) |
| Autorização de acesso | Regras sincronizadas no device (access_rules + time_zones) |
| Cadastro facial | Foto enviada via `user_set_image_list` → device gera template |
| Eventos (acesso, alarme, catraca) | **Monitor** → Gateway HTTP endpoints (`/dao`, `/catra_event`, etc) |
| Comandos imediatos (abrir, msg) | **Push** (polling `/push` a cada 5s) + API direta fallback |
| Sync usuários/regras | `create_or_modify_objects` batch (500/req) |
| Contingência | `device_is_alive` detecta SaaS offline → device usa cache local |

## Consequências

### Positivas
- **Escala**: Não limitado por 3k templates (device gerencia; sync seletivo por validade/plano)
- **Performance**: Identificação + decisão 100% local (~200ms)
- **Confiabilidade**: Funciona sem SaaS, sem internet
- **Simplicidade**: Não precisa manter sessão WebSocket SaaS→Device; HTTP polling simples

### Negativas
- **Propagação de mudanças**: Revogação de acesso leva até próximo sync (15min)
- **Auditoria de enrolment**: `change_logs` limitado a 10k ops (iDFace only)
- **Firmware dependency**: APIs Monitor/Push variam entre versões

### Riscos
- **Sync drift**: Device vs SaaS dessincronizado → **Mitigação**: Checksum por device; full sync semanal forçado; reconciliação `change_logs`
- **Batch photo limit**: 100 fotos/req, 2MB → **Mitigação**: Chunking + prioridade (ativos primeiro)
- **Catraca iDBlock eventos**: `TURN_LEFT/RIGHT` vs `GIVE_UP` mapeamento → **Mitigação**: Mapeamento configurável por instalação

## Alternativas Consideradas
1. **Modo Pro**: Autorização no SaaS. Exige conexão constante; latência > 500ms; falha rede = acesso negado. Rejeitado.
2. **Modo Enterprise**: Templates no SaaS. Excede capacidade device; complexidade enorme; latência biométrica rede. Rejeitado.
3. **Standalone puro (sem Monitor/Push)**: Sem eventos em tempo real; sem comandos imediatos. Rejeitado.

## Referências
- Control iD API: https://www.controlid.com.br/docs/access-api-pt/
- Monitor endpoints: `/dao`, `/catra_event`, `/operation_mode`, `/device_is_alive`, `/door`, `/secbox`
- Push: `GET /push` + `POST /result` + `transactions` batch