# ADR 0006: Biometria: Privacy by Design + LGPD Compliance

## Status
Accepted

## Contexto
Reconhecimento facial processa dados biométricos sensíveis (LGPD Art. 5º, II, 11).
Riscos: vazamento, uso indevido, discriminação, irreversibilidade.
Requisitos legais: consentimento livre, informado, específico; finalidade; minimização; segurança; transparência; retenção limitada.

## Decisão
**Arquitetura Privacy-by-Design para Biometria**:

### 1. Minimização de Dados
- **Device gera template**; SaaS **nunca recebe template bruto**
- SaaS armazena apenas **hash SHA-256 do template** (para deduplicação cross-device)
- **Foto original**: Opcional; se armazenada → criptografada (AES-256-GCM, DEK por tenant); `keep_user_image=0` no device (apaga após template)

### 2. Consentimento Explícito Versionado
- Tabela `biometric_consents` com: `version`, `purpose`, `legal_basis`, `granted_at`, `revoked_at`, `signature_hash`
- UI: Termo digital assinado (canvas/touch) + hash da assinatura
- Revogação a qualquer momento → cascade delete em device + SaaS

### 3. Finalidade Restrita
- `purpose` enum: `ACCESS_CONTROL`, `ATTENDANCE`, `SECURITY`
- Código valida: `biometric_profile` só usado para `access_rules` evaluation

### 4. Retenção Limitada
- `biometric_profiles.expires_at` = `consent.granted_at + 2 anos` (configurável)
- Job diário: revoga expirados → comando gateway → device `user_destroy_image`

### 5. Segurança Reforçada
- Criptografia em repouso (DEK/KEK no Vault)
- TLS 1.3 em todo fluxo
- Acesso: apenas `RECEPTIONIST` + `BRANCH_MANAGER` com `biometric:enroll` permission
- Auditoria imutável: `audit_logs` append-only (BIOMETRIC_ENROLLED, REVOKED, EXPORTED)

### 6. Direitos do Titular (Automatizados)
- `GET /lgpd/my-biometric-data` → status, consentimento, devices enrolados
- `POST /biometric-consents/{id}/revoke` → cascade imediato
- `GET /lgpd/export-biometric` → JSON + foto criptografada (se consentimento ativo)

## Consequências

### Positivas
- **Conformidade LGPD** nativa na arquitetura (não retrofit)
- **Risk reduction**: Template nunca sai do device; superfície de ataque mínima
- **Trust**: Aluno vê controle total (revogação 1-clique)
- **Auditabilidade**: Rastro completo para ANPD

### Negativas
- **Complexidade**: Fluxo consentimento → enrolment → sync → revogação multi-hop
- **Deduplicação cross-device** limitada (apenas hash; não comparação biométrica)
- **Performance**: Criptografia + Vault access no caminho crítico de enrolment

### Riscos
- **Device compromised** → templates expostos → **Mitigação**: Device em VLAN isolada; secure boot; TPM; alerta violação física
- **Consentimento viciado** (coação na recepção) → **Mitigação**: UI clara; opção "não quero" visível; fallback cartão/PIN; auditoria de padrão de consentimento
- **Foto vazada em backup** → **Mitigação**: Backup criptografado com chave separada; `keep_user_image=0` default

## Alternativas Consideradas
1. **Templates no SaaS (Enterprise mode)**: Centraliza biométrico; viola minimização; risco crítico vazamento. Rejeitado.
2. **Sem consentimento explícito (legítimo interesse)**: Juridicamente frágil para biométrico (Art. 11 LGPD). Rejeitado.
3. **Biometria only local (sem SaaS sync)**: Perde gestão central, revogação, auditoria. Rejeitado.

## Referências
- LGPD (Lei 13.709/2018): Art. 5º, 7º, 11, 12, 18, 37, 38
- ANPD Guia de Boas Práticas Biometria
- ISO/IEC 24745 (Biometric information protection)
- Control iD `keep_user_image` config