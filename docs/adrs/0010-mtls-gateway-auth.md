# ADR 0010: Autenticação Gateway ↔ SaaS: mTLS + JWT Híbrido

## Status
Accepted

## Contexto
Gateway roda em hardware cliente (academia), rede não confiável.
SaaS multi-tenant em cloud.
Requisitos:
- Autenticação mútua forte (zero trust)
- Revogação imediata de gateway comprometido
- Baixa latência para comandos (WebSocket)
- Suporte a rotação de credenciais
- Auditoria de todas chamadas

## Decisão
**mTLS 1.3 (Mutual TLS) + JWT de Curta Duração para Autorização**:

### 1. PKI Própria (Private CA)
```
Root CA (offline, HSM)
  └── Intermediate CA (online, Vault PKI)
        ├── SaaS Server Cert (api.gymsaas.com)
        ├── Gateway Client Certs (um por gateway)
        └── Device Certs (opcional, futuro)
```
- Root CA: 10 anos, armazenada offline (HSM/YubiHSM)
- Intermediate: 2 anos, gerenciada no **HashiCorp Vault PKI Engine**
- Certificados: ECDSA P-384 (menor, mais rápido que RSA 2048)

### 2. Bootstrap do Gateway
```bash
# 1. Gateway gera keypair + CSR (primeira execução)
openssl ecparam -genkey -name secp384r1 -out gateway.key
openssl req -new -key gateway.key -out gateway.csr -subj "/CN=gw-<serial>/O=GymSaaS"

# 2. Envia CSR para SaaS (endpoint público, auth via token de instalação)
curl -X POST https://api.gymsaas.com/gateways/bootstrap \
  -H "X-Install-Token: <token>" \
  --data-binary @gateway.csr

# 3. SaaS valida token, assina via Vault PKI, retorna cert + CA chain
# 4. Gateway instala: keystore.p12 (key + cert) + truststore.p12 (Root CA)
```

### 3. Comunicação Runtime
```
┌─────────────────────────────────────────────────────────────┐
│ TLS 1.3 Handshake (mTLS)                                    │
│   - Server: valida cert cliente (gateway)                   │
│   - Client: valida cert server (api.gymsaas.com)            │
│   - ALPN: h2 (HTTP/2)                                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ HTTP/2 sobre TLS                                             │
│   - Header: X-Gateway-ID: <gateway_id>                      │
│   - Cookie: __Host-gw_session=<JWT>; Secure; HttpOnly       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│ JWT (RS256, 15min)                                          │
│   - sub: gateway_id                                         │
│   - tenant_id: (para multi-tenant gateway futuro)           │
│   - scopes: ["sync:read", "sync:write", "events:write",     │
│              "commands:read", "commands:ack"]               │
│   - iat, exp, jti                                           │
└─────────────────────────────────────────────────────────────┘
```

### 4. Renovação de JWT (Sem Round-trip Adicional)
- `refresh_token` armazenado em `gateways.refresh_token_hash` (Argon2id)
- Gateway faz `POST /auth/refresh` (mTLS) → novo JWT + novo refresh_token (rotation)
- Refresh token: 30 dias, rotação a cada uso (detecta replay)

### 5. WebSocket para Comandos Imediatos
- Upgrade na **mesma conexão mTLS** (HTTP/2 CONNECT ou `:protocol: websocket`)
- `Authorization: Bearer <JWT>` no handshake WS
- Heartbeat: `ping`/`pong` a cada 30s
- Reconexão: Exponential backoff (1s, 2s, 4s, 8s, 16s, 32s, max 60s)

### 6. Revogação e Comprometimento
| Cenário | Ação |
|---------|------|
| Gateway roubado/comprometido | SaaS: `POST /gateways/{id}/revoke` → CRL update + cert revogado no Vault + desconecta WS ativo |
| Certificado expirando | Vault PKI: auto-renovação 30d antes; gateway detecta `NotAfter < 14d` → re-bootstrap |
| Suspeita de chave privada vazada | Rotação forçada: revoga cert antigo + emite novo (mesmo `gateway_id`) |

## Consequências

### Positivas
- **Zero Trust**: Nenhum segredo compartilhado (password/api-key); autenticação por posse de chave privada
- **Forward Secrecy**: TLS 1.3 + ECDHE garante PFS
- **Revogação Imediata**: CRL/OCSP stapling + disconnect WS ativo
- **Auditoria**: `gateway_id` em todo request (cert + JWT + header)
- **Escalabilidade**: Stateless JWT para autorização; mTLS apenas no handshake

### Negativas
- **Complexidade PKI**: Vault, CRL, OCSP, bootstrap flow
- **Clock Sync Crítico**: TLS + JWT `exp` requerem NTP sincronizado (gateway + SaaS)
- **Debugging**: Wireshark não descriptografa sem chaves; logs estruturados essenciais

### Riscos
- **Vault indisponível** → bootstrap falha → **Mitigação**: Vault HA (3 nodes); cache de CRL/OCSP no SaaS; gateway usa cert antigo até renovar
- **Gateway sem NTP** (offline prolongado) → cert validation falha → **Mitigação**: RTC battery no hardware; `systemd-timesyncd` + fallback NTP pool; grace period 24h para `NotAfter`
- **Man-in-the-Middle na LAN** → **Mitigação**: mTLS valida cert server; VLAN isolada; Device certs futuro

## Alternativas Consideradas
1. **API Key + HMAC**: Simples, mas segredo compartilhado; rotação complexa; sem forward secrecy. Rejeitado.
2. **OAuth 2.0 Client Credentials (JWT Bearer)**: Boa, mas requer token endpoint; sem autenticação mútua de transporte. Rejeitado.
3. **WireGuard / Tailscale VPN**: Camada 3; adiciona complexidade; não resolve autenticação aplicação. Rejeitado.
4. **mTLS only (sem JWT)**: Autorização baseada em cert (CN/SAN); revogação só via CRL; menos granular. Rejeitado.

## Referências
- RFC 8446 (TLS 1.3): https://www.rfc-editor.org/rfc/rfc8446.html
- RFC 7519 (JWT): https://www.rfc-editor.org/rfc/rfc7519.html
- HashiCorp Vault PKI: https://www.vaultproject.io/docs/secrets/pki
- "Zero Trust Networks" - Evan Gilman / Doug Barth
- Spring Security mTLS: https://docs.spring.io/spring-security/reference/servlet/authentication/mtls.html