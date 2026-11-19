"use client";

import Link from "next/link";
import { Alert } from "@/components/ui/Alert";
import { Field, InfoItem, StatusBadge } from "@/components/ui/Field";
import {
  badgeClass,
  dangerButtonClass,
  inputClass,
  panelClass,
  primaryButtonClass,
  secondaryButtonClass,
  smallButtonClass,
} from "@/components/ui/formStyles";
import { Spinner } from "@/components/ui/Loading";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { formatDateTime, formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import {
  CHARGE_KIND_LABELS,
  CHARGE_STATUS_FILTERS,
  CHARGE_STATUS_LABELS,
  CHARGE_STATUS_STYLES,
  PAYMENT_METHOD_LABELS,
  PAYMENT_METHODS,
  PAYMENT_STATUS_LABELS,
  displayStatus,
  type Charge,
  type PaymentMethod,
} from "@/features/billing/model/types";
import type { ChargesViewModel } from "@/features/billing/viewmodel/ChargesViewModel";

function sortIndicator(active: boolean, direction: "asc" | "desc") {
  if (!active) return "";
  return direction === "asc" ? " ↑" : " ↓";
}

function ChargeStatusBadge({ charge }: { charge: Charge }) {
  const status = displayStatus(charge);
  return <StatusBadge className={CHARGE_STATUS_STYLES[status]}>{CHARGE_STATUS_LABELS[status]}</StatusBadge>;
}

interface ChargesWorkspaceProps {
  viewModel: ChargesViewModel;
  /** Compact mode (membership tab): no filter bar and no student column. */
  compact?: boolean;
}

/**
 * Charge list + detail/payment/cancel/refund/manual-charge modals. Shared by
 * the billing page and the membership "Cobranças" tab. Buttons follow the
 * session permissions; the backend enforces them on every request.
 */
export function ChargesWorkspace({ viewModel, compact = false }: ChargesWorkspaceProps) {
  const can = usePermission();
  const state = useViewModel(viewModel);
  const selected = state.selected;
  const charge = selected?.charge ?? null;
  const modalError = state.modal !== "none" && state.error ? <div className="mb-4"><Alert>{state.error}</Alert></div> : null;

  return (
    <>
      {state.error && state.modal === "none" ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      <section className={`overflow-hidden ${panelClass}`}>
        {compact ? (
          <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/5 px-5 py-4">
            <p className="text-xs text-slate-500">
              Parcelas geradas a partir das condições da matrícula. Vencidas permanecem como dívida mesmo após cancelamento.
            </p>
            {can("charges:create") ? (
              <button type="button" onClick={() => void viewModel.openCreate()} className={secondaryButtonClass}>
                Cobrança avulsa
              </button>
            ) : null}
          </div>
        ) : (
          <form
            className="grid gap-3 border-b border-white/5 px-5 py-4 md:grid-cols-6"
            onSubmit={(e) => {
              e.preventDefault();
              viewModel.applyFilters();
            }}
          >
            <Field id="ch-search" label="Buscar" className="md:col-span-2">
              <input
                id="ch-search"
                className={inputClass}
                placeholder="Aluno, nº da cobrança ou descrição"
                value={state.search}
                onChange={(e) => viewModel.setFilter("search", e.target.value)}
              />
            </Field>
            <Field id="ch-status" label="Situação">
              <select id="ch-status" className={inputClass} value={state.statusFilter}
                onChange={(e) => viewModel.setFilter("statusFilter", e.target.value)}>
                <option value="">Todas</option>
                {CHARGE_STATUS_FILTERS.map((s) => (
                  <option key={s} value={s}>{CHARGE_STATUS_LABELS[s]}</option>
                ))}
              </select>
            </Field>
            <Field id="ch-branch" label="Unidade">
              <select id="ch-branch" className={inputClass} value={state.branchFilter}
                onChange={(e) => viewModel.setFilter("branchFilter", e.target.value)}>
                <option value="">Todas</option>
                {state.branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </Field>
            <Field id="ch-due-from" label="Vencimento de">
              <input id="ch-due-from" type="date" className={inputClass} value={state.dueFrom}
                onChange={(e) => viewModel.setFilter("dueFrom", e.target.value)} />
            </Field>
            <Field id="ch-due-to" label="Vencimento até">
              <input id="ch-due-to" type="date" className={inputClass} value={state.dueTo}
                onChange={(e) => viewModel.setFilter("dueTo", e.target.value)} />
            </Field>
            <div className="flex items-end md:col-start-6">
              <button type="submit" className={`w-full ${secondaryButtonClass}`}>Filtrar</button>
            </div>
          </form>
        )}

        {state.loading ? (
          <div className="flex items-center gap-3 px-5 py-8 text-sm text-slate-400"><Spinner /> Carregando cobranças…</div>
        ) : state.items.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-white">Nenhuma cobrança encontrada</p>
            <p className="mt-1 text-sm text-slate-400">
              {compact ? "Esta matrícula não gerou cobranças." : "Ajuste os filtros ou lance uma cobrança avulsa."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("chargeNumber")} className="hover:text-white">
                      Cobrança{sortIndicator(state.sort === "chargeNumber", state.direction)}
                    </button>
                  </th>
                  {compact ? null : <th className="px-5 py-3 font-medium">Aluno</th>}
                  <th className="px-5 py-3 font-medium">Descrição</th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("dueDate")} className="hover:text-white">
                      Vencimento{sortIndicator(state.sort === "dueDate", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("amount")} className="hover:text-white">
                      Valor{sortIndicator(state.sort === "amount", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">Situação</th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((c) => (
                  <tr key={c.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <p className="font-mono text-xs">{c.chargeNumber}</p>
                      <span className={badgeClass}>{CHARGE_KIND_LABELS[c.kind]}</span>
                    </td>
                    {compact ? null : (
                      <td className="px-5 py-3">
                        <Link href={`/academy/students/${c.studentId}`} className="font-medium text-white hover:text-accent-400">
                          {c.studentName ?? "—"}
                        </Link>
                        <p className="font-mono text-xs text-slate-500">{c.studentRegistration}</p>
                      </td>
                    )}
                    <td className="px-5 py-3">
                      <p>{c.description}</p>
                      {c.periodStart ? (
                        <p className="text-xs text-slate-500">
                          Competência {formatLocalDate(c.periodStart)} → {formatLocalDate(c.periodEnd)}
                        </p>
                      ) : null}
                    </td>
                    <td className="px-5 py-3 text-xs">{formatLocalDate(c.dueDate)}</td>
                    <td className="px-5 py-3 text-white">{formatMoney(c.amount, c.currency)}</td>
                    <td className="px-5 py-3"><ChargeStatusBadge charge={c} /></td>
                    <td className="px-5 py-3">
                      <div className="flex justify-end gap-2">
                        {c.status === "OPEN" && can("payments:register") ? (
                          <button type="button" onClick={() => void viewModel.openPay(c)} className={smallButtonClass}>Receber</button>
                        ) : null}
                        <button type="button" onClick={() => void viewModel.openDetail(c.id)} className={smallButtonClass}>Detalhes</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <Pagination
          page={state.page}
          size={state.size}
          totalElements={state.totalElements}
          onPrevious={() => viewModel.changePage(-1)}
          onNext={() => viewModel.changePage(1)}
          onSizeChange={viewModel.changeSize}
        />
      </section>

      {state.modal === "detail" ? (
        <Modal title={charge ? `Cobrança ${charge.chargeNumber}` : "Cobrança"} onClose={viewModel.closeModal} wide
          footer={charge ? (
            <div className="flex flex-wrap justify-end gap-2">
              {charge.status === "OPEN" && can("charges:cancel") ? (
                <button type="button" onClick={() => void viewModel.openCancel(charge)} className={dangerButtonClass}>Cancelar cobrança</button>
              ) : null}
              {charge.status === "OPEN" && can("payments:register") ? (
                <button type="button" onClick={() => void viewModel.openPay(charge)} className={primaryButtonClass}>Registrar recebimento</button>
              ) : null}
            </div>
          ) : undefined}>
          {modalError}
          {state.loadingDetail || !selected || !charge ? (
            <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando…</div>
          ) : (
            <div className="space-y-5">
              <div className="flex flex-wrap items-center gap-2">
                <ChargeStatusBadge charge={charge} />
                <span className={badgeClass}>{CHARGE_KIND_LABELS[charge.kind]}</span>
                {charge.installmentNumber ? (
                  <span className={badgeClass}>Parcela {charge.installmentNumber}/{charge.installmentCount}</span>
                ) : null}
              </div>
              <dl className="grid gap-4 sm:grid-cols-3">
                <InfoItem label="Aluno">
                  <Link href={`/academy/students/${charge.studentId}`} className="hover:text-accent-400">{charge.studentName ?? "—"}</Link>
                </InfoItem>
                <InfoItem label="Matrícula">
                  {charge.membershipId ? (
                    <Link href={`/academy/memberships/${charge.membershipId}`} className="hover:text-accent-400">
                      {charge.membershipRegistrationNumber ?? "Abrir"}
                    </Link>
                  ) : "—"}
                </InfoItem>
                <InfoItem label="Unidade">{charge.branchName ?? "—"}</InfoItem>
                <InfoItem label="Valor"><span className="font-semibold text-white">{formatMoney(charge.amount, charge.currency)}</span></InfoItem>
                <InfoItem label="Vencimento">{formatLocalDate(charge.dueDate)}</InfoItem>
                <InfoItem label="Competência">
                  {charge.periodStart ? `${formatLocalDate(charge.periodStart)} → ${formatLocalDate(charge.periodEnd)}` : "—"}
                </InfoItem>
                <InfoItem label="Pago em">{formatDateTime(charge.paidAt)}</InfoItem>
                <InfoItem label="Lançada em">{formatDateTime(charge.createdAt)}</InfoItem>
                <InfoItem label="Descrição">{charge.description}</InfoItem>
              </dl>
              {charge.notes ? <p className="whitespace-pre-line text-sm text-slate-400">{charge.notes}</p> : null}
              {charge.status === "CANCELLED" ? (
                <Alert kind="info">Cancelada em {formatDateTime(charge.cancelledAt)}. Motivo: {charge.cancellationReason}</Alert>
              ) : null}
              <div className="space-y-2">
                <h3 className="text-sm font-semibold text-white">Recebimentos</h3>
                {selected.payments.length === 0 ? (
                  <p className="text-sm text-slate-400">Nenhum recebimento registrado.</p>
                ) : (
                  <ul className="divide-y divide-white/5 rounded-xl border border-white/10">
                    {selected.payments.map((p) => (
                      <li key={p.id} className="flex flex-wrap items-start justify-between gap-3 px-4 py-3 text-sm">
                        <div className="space-y-0.5">
                          <p className="text-white">
                            {formatMoney(p.amount, p.currency)} · {PAYMENT_METHOD_LABELS[p.method]}
                            <span className="ml-2 font-mono text-xs text-slate-500">{p.paymentNumber}</span>
                          </p>
                          <p className="text-xs text-slate-500">
                            Recebido em {formatDateTime(p.paidAt)}
                            {p.reference ? ` · ref. ${p.reference}` : ""}
                          </p>
                          {p.status === "REFUNDED" ? (
                            <p className="text-xs text-red-300">
                              Estornado em {formatDateTime(p.refundedAt)} — {p.refundReason}
                            </p>
                          ) : null}
                        </div>
                        <div className="flex items-center gap-2">
                          <span className={badgeClass}>{PAYMENT_STATUS_LABELS[p.status]}</span>
                          {p.status === "CONFIRMED" && can("payments:refund") ? (
                            <button type="button" onClick={() => viewModel.openRefund(p.id)} className={smallButtonClass}>Estornar</button>
                          ) : null}
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          )}
        </Modal>
      ) : null}

      {state.modal === "pay" && charge ? (
        <Modal title="Registrar recebimento"
          description={`${charge.chargeNumber} · ${charge.description}. O valor deve ser o valor integral da cobrança.`}
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.submitPayment()} className={primaryButtonClass}>
                {state.saving ? "Registrando…" : "Confirmar recebimento"}
              </button>
            </div>
          }>
          {modalError}
          <div className="grid gap-3 sm:grid-cols-2">
            <Field id="pay-amount" label="Valor recebido *" error={state.fieldErrors.amount}
              hint={`Valor da cobrança: ${formatMoney(charge.amount, charge.currency)}`}>
              <input id="pay-amount" inputMode="decimal" className={inputClass} value={state.payment.amount}
                onChange={(e) => viewModel.updatePayment({ amount: e.target.value })} />
            </Field>
            <Field id="pay-method" label="Forma de pagamento *">
              <select id="pay-method" className={inputClass} value={state.payment.method}
                onChange={(e) => viewModel.updatePayment({ method: e.target.value as PaymentMethod })}>
                {PAYMENT_METHODS.map((m) => (
                  <option key={m} value={m}>{PAYMENT_METHOD_LABELS[m]}</option>
                ))}
              </select>
            </Field>
            <Field id="pay-at" label="Data e hora do recebimento" error={state.fieldErrors.paidAt} hint="Em branco = agora.">
              <input id="pay-at" type="datetime-local" className={inputClass} value={state.payment.paidAt}
                onChange={(e) => viewModel.updatePayment({ paidAt: e.target.value })} />
            </Field>
            <Field id="pay-ref" label="Referência" hint="Ex.: ID do PIX, NSU da maquininha.">
              <input id="pay-ref" maxLength={200} className={inputClass} value={state.payment.reference}
                onChange={(e) => viewModel.updatePayment({ reference: e.target.value })} />
            </Field>
            <Field id="pay-notes" label="Observações" className="sm:col-span-2">
              <textarea id="pay-notes" maxLength={1000} rows={2} className={inputClass} value={state.payment.notes}
                onChange={(e) => viewModel.updatePayment({ notes: e.target.value })} />
            </Field>
          </div>
        </Modal>
      ) : null}

      {(state.modal === "cancel" || state.modal === "refund") ? (
        <Modal
          title={state.modal === "cancel" ? "Cancelar cobrança" : "Estornar recebimento"}
          description={state.modal === "cancel"
            ? "A cobrança é mantida no histórico como cancelada."
            : "O recebimento fica marcado como estornado e a cobrança volta a ficar em aberto."}
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving}
                onClick={() => void (state.modal === "cancel" ? viewModel.submitCancel() : viewModel.submitRefund())}
                className={dangerButtonClass}>
                {state.saving ? "Salvando…" : state.modal === "cancel" ? "Cancelar cobrança" : "Estornar"}
              </button>
            </div>
          }>
          {modalError}
          <Field id="billing-reason" label="Motivo *" error={state.fieldErrors.reason}>
            <textarea id="billing-reason" maxLength={1000} rows={3} className={inputClass} value={state.reason}
              onChange={(e) => viewModel.setReason(e.target.value)} />
          </Field>
        </Modal>
      ) : null}

      {state.modal === "create" ? (
        <Modal title="Lançar cobrança avulsa" description="Produto, diária ou outro valor fora das parcelas da matrícula."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.submitCreate()} className={primaryButtonClass}>
                {state.saving ? "Lançando…" : "Lançar cobrança"}
              </button>
            </div>
          }>
          {modalError}
          <div className="grid gap-3 sm:grid-cols-2">
            {state.createStudent ? (
              <div className="flex items-center justify-between gap-3 rounded-xl border border-white/10 px-4 py-3 sm:col-span-2">
                <div>
                  <p className="text-sm text-white">{state.createStudent.name}</p>
                  <p className="font-mono text-xs text-slate-500">{state.createStudent.registrationNumber}</p>
                </div>
                {state.studentFilter ? null : (
                  <button type="button" onClick={viewModel.clearStudent} className="text-xs text-slate-400 underline hover:text-white">Trocar</button>
                )}
              </div>
            ) : (
              <Field id="create-student" label="Aluno *" error={state.fieldErrors.studentId} className="sm:col-span-2">
                <div className="relative">
                  <input id="create-student" autoComplete="off" className={inputClass} placeholder="Digite ao menos 2 caracteres"
                    value={state.studentQuery} onChange={(e) => viewModel.setStudentQuery(e.target.value)} />
                  {state.studentResults.length > 0 ? (
                    <ul className="absolute z-10 mt-1 max-h-60 w-full overflow-y-auto rounded-xl border border-white/10 bg-surface-900 shadow-xl">
                      {state.studentResults.map((s) => (
                        <li key={s.id}>
                          <button type="button" onClick={() => viewModel.selectStudent(s)}
                            className="flex w-full flex-col items-start px-4 py-2 text-left hover:bg-white/5">
                            <span className="text-sm text-white">{s.name}</span>
                            <span className="text-xs text-slate-500">{s.registrationNumber}</span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </Field>
            )}
            <Field id="create-branch" label="Unidade *" error={state.fieldErrors.branchId}>
              <select id="create-branch" className={inputClass} value={state.create.branchId}
                onChange={(e) => viewModel.updateCreate({ branchId: e.target.value })}>
                <option value="">Selecione</option>
                {state.branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </Field>
            <Field id="create-due" label="Vencimento *" error={state.fieldErrors.dueDate}>
              <input id="create-due" type="date" className={inputClass} value={state.create.dueDate}
                onChange={(e) => viewModel.updateCreate({ dueDate: e.target.value })} />
            </Field>
            <Field id="create-description" label="Descrição *" error={state.fieldErrors.description} className="sm:col-span-2">
              <input id="create-description" maxLength={300} className={inputClass} value={state.create.description}
                onChange={(e) => viewModel.updateCreate({ description: e.target.value })} />
            </Field>
            <Field id="create-amount" label="Valor *" error={state.fieldErrors.amount}>
              <input id="create-amount" inputMode="decimal" placeholder="0,00" className={inputClass} value={state.create.amount}
                onChange={(e) => viewModel.updateCreate({ amount: e.target.value })} />
            </Field>
            {state.membershipFilter ? (
              <div className="flex items-end text-xs text-slate-500">Vinculada a esta matrícula.</div>
            ) : null}
            <Field id="create-notes" label="Observações" className="sm:col-span-2">
              <textarea id="create-notes" maxLength={1000} rows={2} className={inputClass} value={state.create.notes}
                onChange={(e) => viewModel.updateCreate({ notes: e.target.value })} />
            </Field>
          </div>
        </Modal>
      ) : null}
    </>
  );
}
