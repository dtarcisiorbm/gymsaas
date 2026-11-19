# Relatório final — Fase 6: Financeiro (cobranças e recebimentos manuais)

Data: 2026-11-19 · Plano: `docs/specs/2026-11-17-phase6-plan.md`

## 1. Resultado

| Item | Estado |
|---|---|
| Migration V13 (contadores, `charges`, `payments`, triggers, RLS, permissões) | Aplicada (testes + Docker) |
| Módulo `com.gymsaas.billing` (domínio, casos de uso, JDBC, controllers) | Implementado |
| Integração com matrículas (geração na criação/renovação, cancelamento) | Implementada |
| Frontend `features/billing` + `/academy/billing` + aba "Cobranças" + atalho no aluno + menu | Implementado |
| Coleção Postman (pasta "Financeiro") | Atualizada |
| `./mvnw verify` | BUILD SUCCESS — **128 unitários / 50 integração** (antes 112 / 40) |
| `tsc --noEmit` e `next build` | OK (17 rotas) |
| Smoke Docker Compose | Flyway em V13; 21/21 verificações via API; fluxo na UI verificado |

## 2. Decisões aplicadas

F1–F9 do plano, sem alterações de regra:
- Parcelas: `ciclo = meses(periodicidade) × intervalo`, `parcelas = ⌈meses do termo ÷ ciclo⌉`; ONE_TIME e planos em dias
  = 1 parcela. Vencimento *i* = `início + i × ciclo` (sempre a partir do início; dia 31 ajusta ao fim do mês). Competência
  de cada parcela = `[vencimento, próximo vencimento − 1]`, a última limitada ao `originalEndDate` da matrícula.
  Congelamentos não alteram cobranças.
- Valor da parcela = `finalAmount` do snapshot. Matrícula com valor final zero (desconto de 100%) não gera parcelas.
- Taxa de adesão: só em matrícula nova e se > 0; vence no início.
- Cancelamento de matrícula: cobranças `OPEN` com vencimento ≥ hoje (fuso do tenant) viram `CANCELLED` com motivo
  "Matrícula M… cancelada: <motivo>"; vencidas permanecem `OPEN`.
- Recebimento integral (valor exatamente igual), `paidAt` opcional e não pode estar no futuro (tolerância de 5 min).
- Estorno: recebimento `REFUNDED` + cobrança volta a `OPEN`; pode ser paga novamente (histórico mantém ambos).
- Resumo (`/api/billing/summary`, padrão mês corrente): **a receber** = aberto, não vencido, com vencimento no período;
  **vencido** = todo o aberto com vencimento antes de hoje (dívida total, independe do período); **recebido** =
  recebimentos confirmados no período (por forma); **estornado** = estornos no período. Período máximo 366 dias.

## 3. Desvios do plano

- `GenerateMembershipChargesUseCase`/`CancelMembershipChargesUseCase` viraram um único adapter,
  `billing.application.MembershipBillingAdapter`, que implementa a porta `membership.application.MembershipBillingPort`.
  Assim o módulo de matrículas não depende de `billing` (sem ciclo entre módulos); as chamadas rodam na mesma transação
  da matrícula (falha no financeiro desfaz a matrícula).
- Persistência do financeiro com `JdbcTemplate` (adapters `JdbcChargeRepository`/`JdbcPaymentRepository`) em vez de JPA:
  SQL explícito para `SELECT … FOR UPDATE`, filtros dinâmicos e agregações. Usa o mesmo DataSource com RLS e participa da
  transação JPA (mesmo padrão dos geradores de número).
- Correção transversal: `GlobalExceptionHandler` passou a responder **400** para corpo JSON ilegível
  (`MALFORMED_REQUEST`, ex.: `"25,00"` em campo decimal) e parâmetro de tipo inválido (`INVALID_PARAMETER`, ex.: UUID
  malformado na query). Antes respondia 500 em qualquer endpoint. Encontrado no smoke.
- `RlsIsolationIT` ajustado para 13 migrations e RLS nas 3 tabelas novas.

## 4. API

| Método | Endpoint | Permissão |
|---|---|---|
| GET | `/api/charges` (status `OPEN`/`PAID`/`CANCELLED`/`OVERDUE`, `studentId`, `membershipId`, `branchId`, `dueFrom`, `dueTo`, `search`, `sort`=`dueDate`\|`createdAt`\|`amount`\|`chargeNumber`\|`status`) | `charges:read` \| `payment:read` |
| POST | `/api/charges` | `charges:create` |
| GET | `/api/charges/{id}` → `{ charge, payments }` | `charges:read` \| `payment:read` |
| POST | `/api/charges/{id}/cancel` | `charges:cancel` |
| POST | `/api/charges/{id}/payments` | `payments:register` |
| POST | `/api/payments/{id}/refund` | `payments:refund` |
| GET | `/api/billing/summary?from&to&branchId` | `billing:reports` |

Códigos de erro: `CHARGE_NOT_FOUND`, `PAYMENT_NOT_FOUND`, `CHARGE_NOT_OPEN` (409), `PAYMENT_NOT_REFUNDABLE` (409),
`AMOUNT_MISMATCH`, `INVALID_METHOD`, `INVALID_AMOUNT`, `PAID_AT_IN_FUTURE`, `INVALID_STATUS`, `INVALID_PERIOD`,
`MEMBERSHIP_STUDENT_MISMATCH`, `STUDENT_NOT_ELIGIBLE` (aluno arquivado).

Auditoria (`audit_logs`): `BILLING_CHARGES_GENERATED`, `BILLING_CHARGES_CANCELLED` (recurso = matrícula),
`CHARGE_CREATED`, `CHARGE_CANCELLED`, `PAYMENT_REGISTERED`, `PAYMENT_REFUNDED` (recurso = cobrança). Sem dados pessoais.

## 5. Testes

- Unitários: `BillingScheduleTest` (mensal, anual mensal, anual trimestral, intervalo com arredondamento, ONE_TIME,
  dias, dia 31, bissexto, meses customizados, termo inválido) e `ChargeRulesTest` (vencida derivada, transições,
  valor exato, estorno único, numeração).
- Integração: `BillingManagementIT` (geração com adesão; renovação sem adesão; desconto aplicado por ciclo; cancelamento
  mantém vencidas; recebimento, valor divergente, forma inválida, data futura, baixa repetida, estorno, novo
  recebimento, auditoria; avulsa com validações, filtro OVERDUE, busca, cancelamento; resumo por deltas; JSON
  malformado/UUID inválido = 400) e `BillingSecurityIT` (isolamento multi-tenant via API e RLS real não vazio, FK
  composta, triggers de imutabilidade/exclusão, índice único de recebimento confirmado, 5 recebimentos concorrentes =
  1×201 + 4×409, permissões da recepção 403/401).

## 6. Frontend

- `features/billing` (model/data/viewmodel `ChargesViewModel`) e componente compartilhado
  `components/billing/ChargesWorkspace` (lista + modais de detalhe, recebimento, cancelamento, estorno e avulsa).
- `/academy/billing`: resumo (só com `billing:reports`), filtros (situação incl. vencida, unidade, vencimento, busca),
  `?studentId=` / `?membershipId=` para escopo.
- Detalhe da matrícula: aba "Cobranças" (com `charges:read`/`payment:read`); texto do cancelamento atualizado para o
  efeito financeiro. Página do aluno: botão "Financeiro". Menu: item "Financeiro".
- Verificado no navegador (admin): resumo, lista, lançar avulsa com vírgula decimal, registrar recebimento, detalhe com
  recebimento e botão de estorno; console sem erros/avisos. Aba "Cobranças" da matrícula não foi aberta no navegador.

## 7. Pendências

- Pagamento parcial, multa/juros, descontos por antecipação e boleto/PIX automático (gateway) — fora do escopo.
- Recibo em PDF e exportação (CSV) do financeiro.
- Escopo por unidade do usuário (`user_branch_access`) não é aplicado nas listagens — igual às matrículas.
- Dados de teste do smoke ficaram no banco local de demonstração (alunos "Aluno Smoke Fin *", planos `SMOKE-F*`).
