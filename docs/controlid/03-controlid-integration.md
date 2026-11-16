# Mapeamento da API Control iD — iDFace + iDBlock

Baseado na documentação oficial: https://www.controlid.com.br/docs/access-api-pt/

---

## Visão Geral dos Modos de Operação

| Modo | Identificação | Autorização | Uso Recomendado |
|------|---------------|-------------|-----------------|
| **Standalone** | Local (terminal) | Local (terminal) | **Default** — Offline-first, sync periódica |
| **Pro** | Local (terminal) | Servidor (SaaS) | Quando regras complexas/dinâmicas |
| **Enterprise** | Servidor (SaaS) | Servidor (SaaS) | Alta segurança, templates no server |

**Decisão Arquitetural**: **Standalone + Monitor + Push Híbrido**
- Identificação facial **local** no iDFace (performance, offline)
- Regras de acesso **cacheadas** no terminal via sync
- Eventos (access_logs, catraca, alarme) via **Monitor** (push assíncrono)
- Comandos imediatos (abrir catraca, msg) via **Push** (polling HTTP)
- Fallback: `device_is_alive` detecta contingência

---

## Autenticação e Sessão

### Endpoints

| Operação | Endpoint | Método | Content-Type |
|----------|----------|--------|--------------|
| Login | `/login.fcgi` | POST | `application/json` |
| Logout | `/logout.fcgi` | POST | `application/json` |
| Validar Sessão | `/session_is_valid.fcgi` | POST | `application/json` |
| Alterar Credenciais | `/change_login.fcgi` | POST | `application/json` |

### Fluxo

```java
// Login
POST /login.fcgi
{ "login": "admin", "password": "senha" }
// Response: { "session": "apx7NM2CErTcvXpuvExuzaZ" }

// Todas as requisições subsequentes:
?session=apx7NM2CErTcvXpuvExuzaZ

// Sessão expira em ~30min de inatividade (configurável)
```

### Gestão no Gateway

- **Pool de sessões** por dispositivo (reutilização)
- **Renovação proativa** a cada 20min
- **Circuit breaker** se 3 falhas consecutivas → re-login
- **Credenciais criptografadas** no `device_credentials.password_hash`

---

## Objetos Principais (Mapeamento SaaS ↔ Control iD)

| Objeto Control iD | Entidade SaaS | Sync Direction | Notas |
|-------------------|---------------|----------------|-------|
| `users` | `students` + `users` (staff) | SaaS → Device | `registration` = matrícula; `name` = nome |
| `templates` (digital) | `biometric_enrollments` (fingerprint) | Bidirecional | Apenas se usar digital |
| `face_templates` / fotos | `biometric_enrollments` (face) | SaaS → Device | Foto → template local no device |
| `cards` | `biometric_enrollments` (card) | Bidirecional | Wiegand/Proximidade |
| `qrcodes` | `biometric_enrollments` (qr) | Bidirecional | Modo alfanumérico |
| `pins` | `biometric_enrollments` (pin) | Bidirecional | |
| `access_rules` | `access_rules` + `access_rule_time_zones` | SaaS → Device | Regras + horários |
| `time_zones` / `time_spans` | `time_zones` + `time_spans` | SaaS → Device | |
| `portals` / `areas` | Configuração fixa por device | SaaS → Device | Mapeamento físico |
| `groups` / `user_groups` | Não usado (RBAC no SaaS) | — | |
| `holidays` | Feriados nacionais/locais | SaaS → Device | 3 tipos |
| `access_logs` | `access_logs` | Device → SaaS | Via Monitor |
| `alarm_logs` | `audit_logs` (tipo ALARM) | Device → SaaS | Via Monitor |
| `catra_infos` | `turnstiles` (contadores) | Device → SaaS | Read-only |
| `change_logs` | Auditoria sync | Device → SaaS | iDFace only, 10k limit |

---

## Operações CRUD de Objetos

### Endpoints Genéricos

| Operação | Endpoint | Método | Uso |
|----------|----------|--------|-----|
| Criar | `/create_objects.fcgi` | POST | Insert |
| Carregar | `/load_objects.fcgi` | POST | Select (com filtros) |
| Modificar | `/modify_objects.fcgi` | POST | Update |
| Criar/Modificar | `/create_or_modify_objects.fcgi` | POST | Upsert |
| Destruir | `/destroy_objects.fcgi` | POST | Delete |
| Exportar Relatório | `/export_report.fcgi` | POST | CSV/JSON logs |

### Formato Padrão (create_objects)

```json
POST /create_objects.fcgi?session=xxx
{
  "object": "users",
  "values": [
    { "id": 12345, "registration": "ALU-001", "name": "João Silva", "password": "hash", "user_type_id": 0 }
  ]
}
```

### Formato Padrão (load_objects)

```json
POST /load_objects.fcgi?session=xxx
{
  "object": "users",
  "fields": ["id", "registration", "name", "user_type_id"],
  "where": { "user_type_id": 0 },
  "limit": 1000,
  "offset": 0
}
```

### Estratégia de Sync (SaaS → Device)

```java
// 1. Diff: comparar hash (SHA-256) do payload agregado por device
// 2. Se diff: enviar create_or_modify_objects em lotes de 500
// 3. Para fotos: user_set_image_list.fcgi (batch base64, max 2MB)
// 4. Verificar change_logs após sync para confirmar
// 5. Marcar biometric_enrollments.sync_status = SYNCED
```

---

## Reconhecimento Facial — Cadastro e Configuração

### Cadastro por Foto (Preferido para iDFace)

| Operação | Endpoint | Content-Type | Limite |
|----------|----------|--------------|--------|
| Foto única | `/user_set_image.fcgi` | `application/octet-stream` | < 2MB |
| Lote (batch) | `/user_set_image_list.fcgi` | `application/json` | 100 fotos, < 2MB total |
| Obter foto | `/user_get_image.fcgi` | GET → `image/jpeg` ou JSON+base64 | |
| Listar com foto | `/user_list_images.fcgi` | GET | `get_timestamp=1` |
| Testar foto | `/user_test_image.fcgi` | `application/octet-stream` | Não persiste |
| Excluir foto | `/user_destroy_image.fcgi` | POST JSON | Remove template facial |

### Parâmetros de Qualidade (Retorno `scores`)

| Campo | Ideal | Rejeição se |
|-------|-------|-------------|
| `bounds_width` | 100-400 | < 60 (longe) ou > 800 (perto) |
| `horizontal_center_offset` | ±50 | > 1000 |
| `vertical_center_offset` | ±50 | > 1000 |
| `center_pose_quality` | < 200 | > 400 (rosto torto) |
| `sharpness_quality` | > 700 | < 450 (borrado) |

### Configurações Faciais Relevantes (`set_configuration.fcgi`)

```json
{
  "face_id": {
    "mask_detection_enabled": "2",           // 0=off, 1=obrigatório, 2=recomendado
    "vehicle_mode": "0",                     // 0=pedestre, 1=veicular
    "max_identified_duration": "30000",      // ms (anti-tailgating)
    "limit_identification_to_display_region": "1",
    "liveness_mode": "1",                    // 0=normal, 1=rigoroso
    "min_detect_bounds_width": "0.29"        // ~40cm (11.6/distance_cm)
  },
  "camera_overlay": {
    "zoom": "1.5",
    "vertical_crop": "-0.06"
  },
  "face_module": {
    "light_threshold_led_activation": "1"    // 0=baixa, 1=média, 2=alta, 3=sempre
  },
  "led_white": {
    "brightness": "70"
  },
  "general": {
    "keep_user_image": "0"                   // 0=apagar foto após template (LGPD)
  },
  "enroller": {
    "return_face_template": "0"              // 0=foto, 1=template no remote_enroll
  }
}
```

### Thresholds Personalizados (Faces Similares)

```json
POST /create_objects.fcgi
{
  "object": "custom_thresholds",
  "values": [{ "user_id": 12345, "threshold": 1200 }]  // Default ~900-1000
}
// Aumenta rigidez para usuário específico
// ⚠️ Fazer apenas com suporte Control iD
```

---

## Modo Online — Eventos de Identificação (Pro/Enterprise)

> **Não usado no modo Standalone** (identificação local).
> Documentado para referência se migração futura.

### Eventos Enviados pelo Equipamento → Servidor

| Evento | Endpoint | Identificação |
|--------|----------|---------------|
| Imagem biométrica | `/new_biometric_image.fcgi` | Digital (imagem) |
| Template biométrica | `/new_biometric_template.fcgi` | Digital (template Innovatrics) |
| Cartão proximidade | `/new_card.fcgi` | RFID/Wiegand |
| QR Code | `/new_qrcode.fcgi` | QR alfanumérico |
| UHF Tag | `/new_uhf_tag.fcgi` | UHF |
| ID + Senha | `/new_user_id_and_password.fcgi` | Teclado |
| **Usuário identificado (Pro)** | `/new_user_identified.fcgi` | **Local + user_id** |
| Acesso por botoeira | `/new_rex_log.fcgi` | Botão saída |

### Resposta do Servidor (Autorização)

```json
// iDFace / iDAccess / iDBox
{
  "result": {
    "event": 7,                    // 7 = Acesso concedido
    "user_id": 12345,
    "user_name": "João Silva",
    "user_image": false,
    "portal_id": 1,
    "actions": [
      { "action": "door", "parameters": "door=1" },
      { "action": "door", "parameters": "door=2" }
    ],
    "message": "Bem-vindo!",
    "duress": 0
  }
}

// iDBlock (Catraca)
{
  "result": {
    "event": 7,
    "user_id": 12345,
    "user_name": "João Silva",
    "user_image": false,
    "portal_id": 1,
    "actions": [
      { "action": "catra", "parameters": "allow=clockwise" }
    ]    // allow: clockwise, anticlockwise, both
  }
}
```

### Códigos de Evento (Resposta)

| Código | Significado |
|--------|-------------|
| 1 | Equipamento inválido |
| 2 | Parâmetros inválidos |
| 3 | Não identificado |
| 4 | Identificação pendente |
| 5 | Timeout identificação |
| 6 | **Acesso negado** |
| 7 | **Acesso concedido** |
| 8 | Acesso pendente (múltipla validação) |
| 9 | Usuário não é admin |
| 10 | Acesso não identificado (API) |
| 11 | Acesso por botoeira |
| 12 | Acesso pela interface web |
| 13 | Desistência (iDBlock) |
| 14 | Sem resposta |
| 15 | Acesso por interfonia (iDFace) |

---

## Monitor — Eventos Assíncronos (Todos os Modos)

Configuração no device:
```json
POST /set_configuration.fcgi
{
  "monitor": {
    "hostname": "gateway.local",
    "port": "8080",
    "path": "api/notifications",
    "request_timeout": "5000",
    "alive_interval": "30000",
    "inform_access_event_id": 1,
    "enable_photo_upload": 0
  }
}
```

### Endpoints que o Gateway deve implementar

| Evento | Endpoint | Payload Principal |
|--------|----------|-------------------|
| **Logs (access, template, card, alarm)** | `POST /api/notifications/dao` | `object_changes[]` |
| **Cadastro remoto (template)** | `POST /api/notifications/template` | Template biométrico |
| **Cadastro remoto (face/foto)** | `POST /api/notifications/user_image` | Foto base64 |
| **Cadastro remoto (face template)** | `POST /api/notifications/face_template` | Template facial |
| **Cadastro remoto (cartão)** | `POST /api/notifications/card` | Card value |
| **Cadastro remoto (PIN)** | `POST /api/notifications/pin` | PIN value |
| **Cadastro remoto (senha)** | `POST /api/notifications/password` | Senha hash |
| **Eventos catraca (iDBlock)** | `POST /api/notifications/catra_event` | `TURN_LEFT`, `TURN_RIGHT`, `GIVE_UP` |
| **Mudança modo operação** | `POST /api/notifications/operation_mode` | `mode`, `exception_mode` |
| **Heartbeat (standalone)** | `POST /api/notifications/device_is_alive` | `access_logs` count |
| **Porta/Relé (SecBox)** | `POST /api/notifications/door` / `secbox` | `id`, `open` boolean |
| **Foto no acesso** | `POST /api/notifications/access_photo` | `access_photo` (base64 jpg) |

### Exemplo `dao` (Data Access Object Changes)

```json
{
  "object_changes": [
    {
      "object": "access_logs",
      "type": "inserted",
      "values": {
        "id": "519",
        "time": "1532977090",
        "event": "7",
        "device_id": "478435",
        "identifier_id": "0",
        "user_id": "12345",
        "portal_id": "1",
        "card_value": "0",
        "confidence": "1650",
        "mask": "0"
      }
    }
  ],
  "device_id": 478435
}
```

---

## Push — Comandos do Servidor → Equipamento

Configuração no device:
```json
POST /set_configuration.fcgi
{
  "push_server": {
    "hostname": "gateway.local",
    "port": "8080",
    "path": "push",
    "interval": "5000"
  }
}
```

### Fluxo

1. Device faz `GET /push?deviceId=XXX&uuid=YYY` a cada `interval` ms
2. Gateway responde:
   - **Vazio** (200 OK, body vazio) = sem comandos
   - **Comando** = JSON com `verb`, `endpoint`, `body`, `contentType`, `queryString`
3. Device executa comando no próprio API local
4. Device envia `POST /result` com resultado
5. Gateway responde vazio (ack)

### Comandos Úteis via Push

| Comando | Endpoint | Uso |
|---------|----------|-----|
| Abrir porta | `door.fcgi` | `{"door": 1, "timeout": 5}` |
| Abrir catraca | `catra.fcgi` | `{"allow": "clockwise"}` |
| Mensagem display | `message_to_screen.fcgi` | `{"message": "Bem-vindo!", "timeout": 3000}` |
| Buzzer | `buzzer.fcgi` | `{"times": 2, "interval": 200}` |
| Sync usuários | `create_or_modify_objects.fcgi` | Batch users |
| Sync config | `set_configuration.fcgi` | Parâmetros |
| Reiniciar | `reboot.fcgi` | `{}` |
| Capturar foto | `capture_image.fcgi` | Para enrolment remoto |

### Batch Transactions (Múltiplos comandos)

```json
{
  "transactions": [
    { "transactionid": 1, "verb": "POST", "endpoint": "set_configuration", "body": {...} },
    { "transactionid": 2, "verb": "POST", "endpoint": "message_to_screen", "body": {...} }
  ]
}
```

---

## Ações Remotas (API Direta — Não Push/Monitor)

| Ação | Endpoint | Descrição |
|------|----------|-----------|
| Autorização remota | `/remote_authorize.fcgi` | Abre porta/catraca via ID usuário |
| Abertura remota porta | `/open_door.fcgi` | `door=1` |
| Abertura remota catraca | `/open_catra.fcgi` | `allow=clockwise` |
| Cadastro remoto bio/face/cartão/PIN | `/remote_enroll.fcgi` | Enroll sem foto local |
| Mensagem na tela | `/message_to_screen.fcgi` | Display custom |
| Buzzer remoto | `/buzzer.fcgi` | Feedback sonoro |
| Personalizar msg eventos | `/customize_event_message.fcgi` | Msg por evento |

### Cadastro Remoto (`remote_enroll.fcgi`)

```json
POST /remote_enroll.fcgi?session=xxx
{
  "user_id": 12345,
  "template": "base64_template",        // Ou "image": "base64_jpg"
  "biometry_type": "face",              // face, fingerprint, card, pin, password
  "save": true,                         // Salvar no device
  "sync": false,                        // Não propagar para outros devices
  "finger_position": 0,                 // Se digital
  "card_value": 123456789,              // Se cartão
  "pin_value": "1234",                  // Se PIN
  "password": "hash"                    // Se senha
}
```

---

## Particularidades iDFace e iDBlock

### iDFace (Terminal Facial Principal)

| Característica | Detalhe |
|----------------|---------|
| Capacidade templates faciais | ~3.000 (varia por firmware) |
| Capacidade usuários total | ~10.000 |
| Modos: Standalone, Pro, Enterprise | Todos suportados |
| Liveness detection | Sim (configurável normal/rigoroso) |
| Máscara detection | Sim (0/1/2) |
| Foto armazenada | Configurável (`keep_user_image`) |
| Streaming vídeo | Sim (RTSP) |
| Interfonia SIP | Sim |
| QR Code | Modo legacy/alfanumérico |
| Catraca integrada | Não (usa iDBlock via Wiegand/TCP) |

### iDBlock / iDBlock Next (Catraca)

| Característica | Detalhe |
|----------------|---------|
| Tipos | Tripé, Torniquete, PNE, Vidro |
| Sentidos | Horário (entry), Anti-horário (exit), Ambos |
| Eventos catraca | `TURN_LEFT`, `TURN_RIGHT`, `GIVE_UP` |
| Contadores | `left_turns`, `right_turns`, `entrance_turns`, `exit_turns` |
| Intertravamento | Via rede (network_interlocking_rules) |
| Identificação | Via iDFace externo (Wiegand/TCP) ou leitor próprio |
| Modo Next | Eventos identificação online próprios |

### Limitações Críticas

| Limitação | Impacto | Mitigação |
|-----------|---------|-----------|
| **~3.000 templates faciais/iDFace** | Não cabe todos alunos se > 3k/unidade | **Standalone mode** (identificação local); sync seletivo por plano/validade; Enterprise mode só se necessário |
| **change_logs: 10k operações** | Auditoria de sync limitada | Polling frequente + persistência no gateway |
| **access_events: 10k eventos** | Buffer catraca | Monitor contínuo + persistência |
| **Batch foto: 100 fotos / 2MB** | Enrollment massa lento | Chunking + prioridade ativos |
| **user_set_image: 1 foto/req** | Lento para muitos | Usar `user_set_image_list` |
| **Firmware variations** | APIs mudam entre versões | Matriz compatibilidade; version detection no gateway |

---

## Mapeamento de IDs (SaaS ↔ Device)

| Contexto | SaaS (UUID) | Device (int64) | Estratégia |
|----------|-------------|----------------|------------|
| Student/User | `uuid` | `users.id` (int64) | **Mapping table** `biometric_enrollments.device_user_id` |
| Device | `uuid` | `devices.id` (int64) | `devices.serial_number` como chave natural |
| Gateway | `uuid` | N/A | Gateway não exposto ao device |
| Access Rule | `uuid` | `access_rules.id` (int64) | Sync 1:1, IDs device = sequence local |
| Time Zone | `uuid` | `time_zones.id` (int64) | Idem |
| Portal | `uuid` | `portals.id` (int64) | Fixado no provisionamento |

> **Regra**: Nunca expor UUID SaaS ao equipamento. Gateway mantém mapeamento bidirecional.

---

## Contratos de Integração (Gateway ↔ SaaS)

### Sync Request (SaaS → Gateway)

```json
{
  "syncId": "uuid-v7",
  "tenantId": "uuid",
  "branchId": "uuid",
  "gatewayId": "uuid",
  "timestamp": "2026-11-12T10:00:00Z",
  "fullSync": false,
  "payload": {
    "users": [
      { "deviceUserId": 12345, "registration": "ALU-001", "name": "João", "accessRuleIds": [1,2], "validFrom": 1731400000, "validTo": 1734000000 }
    ],
    "faces": [
      { "deviceUserId": 12345, "imageBase64": "...", "timestamp": 1731400000, "match": false }
    ],
    "cards": [
      { "deviceUserId": 12345, "value": 123456789 }
    ],
    "accessRules": [
      { "id": 1, "name": "Horário Comercial", "type": 1, "timeZoneIds": [10, 11] }
    ],
    "timeZones": [
      { "id": 10, "name": "Seg-Sex 06-22", "spans": [{ "start": 21600, "end": 79200, "mon": true, "tue": true, "wed": true, "thu": true, "fri": true }] }
    ],
    "devices": [
      { "serial": "IDF-12345", "ip": "192.168.1.100", "mode": "STANDALONE", "config": {...} }
    ]
  },
  "checksum": "sha256-hex"
}
```

### Event Notification (Gateway → SaaS)

```json
{
  "eventId": "uuid-v7",
  "eventType": "ACCESS_LOG",
  "gatewayId": "uuid",
  "deviceSerial": "IDF-12345",
  "occurredAt": "2026-11-12T10:00:05Z",
  "receivedAt": "2026-11-12T10:00:05.100Z",
  "payload": {
    "deviceUserId": 12345,
    "eventCode": 7,
    "eventLabel": "GRANTED",
    "portalId": 1,
    "identifierId": 0,
    "credentialType": "FACE",
    "confidence": 1650,
    "faceMask": false,
    "direction": "ENTRY"
  }
}
```

---

## Checklist Integração Control iD

- [ ] Auth: login, session pool, renovação, circuit breaker
- [ ] CRUD Objects: create/load/modify/destroy/create_or_modify
- [ ] Facial: user_set_image_list, quality validation, keep_user_image=0
- [ ] Config: face_id, camera_overlay, general, enroller, monitor, push_server
- [ ] Monitor: dao, catra_event, operation_mode, device_is_alive, door/secbox
- [ ] Push: GET /push, POST /result, transactions batch
- [ ] Ações: open_door, open_catra, message_to_screen, buzzer, remote_authorize
- [ ] Mapeamento IDs: UUID ↔ int64 bidirecional persistido
- [ ] Sync: diff por hash, batch 500, retry exponencial, idempotency
- [ ] Offline: fila local SQLite, replay ordenado, dedup por eventId
- [ ] Limitações: 3k faces, 10k logs, 100 fotos/batch, firmware matrix
- [ ] Testes: integração com device real (Testcontainers + mock server)