"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Field, InfoItem, StatusBadge } from "@/components/ui/Field";
import { inputClass, panelClass, primaryButtonClass, secondaryButtonClass } from "@/components/ui/formStyles";
import { Spinner } from "@/components/ui/Loading";
import { formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import { MEMBERSHIP_STATUS_LABELS, MEMBERSHIP_STATUS_STYLES } from "@/features/memberships/model/types";
import { MembershipFormViewModel } from "@/features/memberships/viewmodel/MembershipFormViewModel";
import { describeBilling, describeDuration } from "@/features/plans/model/types";
import { STUDENT_STATUS_LABELS } from "@/features/students/model/types";

function NewMembershipContent() {
  const router = useRouter();
  const can = usePermission();
  const viewModel = useMemo(() => new MembershipFormViewModel(), []);
  const state = useViewModel(viewModel);
  const canDiscount = can("memberships:apply_discount");
  const { form, quote, fieldErrors } = state;

  useEffect(() => {
    void viewModel.init(new URLSearchParams(window.location.search).get("studentId"));
  }, [viewModel]);

  async function handleConfirm() {
    const id = await viewModel.confirm();
    if (id) router.push(`/academy/memberships/${id}`);
  }

  const selectedPlan = state.plans.find((p) => p.id === form.planId);
  const blockedStudent = state.selectedStudent && ["BLOCKED", "ARCHIVED"].includes(state.selectedStudent.status);

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Comercial</p>
          <h1 className="text-2xl font-semibold text-white">Nova matrícula</h1>
          <p className="text-sm text-slate-400">O preço é calculado pelo servidor a partir do preço vigente do plano.</p>
        </div>
        <Link href="/academy/memberships" className={secondaryButtonClass}>Voltar</Link>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}

      <div className="grid gap-6 lg:grid-cols-5">
        <section className={`${panelClass} space-y-5 p-5 lg:col-span-3`}>
          <div className="space-y-2">
            <h2 className="text-sm font-semibold text-white">1. Aluno</h2>
            {state.selectedStudent ? (
              <div className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-white/10 bg-surface-950/50 px-4 py-3">
                <div>
                  <p className="font-medium text-white">{state.selectedStudent.name}</p>
                  <p className="text-xs text-slate-400">
                    {state.selectedStudent.registrationNumber} · CPF {state.selectedStudent.cpfMasked ?? "—"} ·{" "}
                    {STUDENT_STATUS_LABELS[state.selectedStudent.status]}
                  </p>
                </div>
                <button type="button" onClick={viewModel.clearStudent} className="text-xs text-slate-400 underline hover:text-white">
                  Trocar
                </button>
              </div>
            ) : (
              <Field id="student-query" label="Buscar aluno *" error={fieldErrors.studentId} hint="Nome, nome social ou CPF.">
                <div className="relative">
                  <input
                    id="student-query"
                    className={inputClass}
                    autoComplete="off"
                    placeholder="Digite ao menos 2 caracteres"
                    value={state.studentQuery}
                    onChange={(e) => viewModel.setStudentQuery(e.target.value)}
                  />
                  {state.searchingStudents ? <Spinner className="absolute right-3 top-2.5 h-4 w-4" /> : null}
                  {state.studentResults.length > 0 ? (
                    <ul className="absolute z-10 mt-1 max-h-72 w-full overflow-y-auto rounded-xl border border-white/10 bg-surface-900 shadow-xl">
                      {state.studentResults.map((s) => (
                        <li key={s.id}>
                          <button
                            type="button"
                            onClick={() => viewModel.selectStudent(s)}
                            className="flex w-full flex-col items-start px-4 py-2 text-left hover:bg-white/5"
                          >
                            <span className="text-sm text-white">{s.name}</span>
                            <span className="text-xs text-slate-500">
                              {s.registrationNumber} · {s.cpfMasked ?? "sem CPF"} · {STUDENT_STATUS_LABELS[s.status]}
                            </span>
                          </button>
                        </li>
                      ))}
                    </ul>
                  ) : null}
                </div>
              </Field>
            )}
            {blockedStudent ? (
              <Alert>Aluno bloqueado ou arquivado não pode contratar planos.</Alert>
            ) : null}
          </div>

          <div className="space-y-3">
            <h2 className="text-sm font-semibold text-white">2. Plano e unidade</h2>
            <div className="grid gap-3 sm:grid-cols-2">
              <Field id="ms-plan" label="Plano *" error={fieldErrors.planId}>
                <select id="ms-plan" className={inputClass} value={form.planId}
                  onChange={(e) => viewModel.updateForm({ planId: e.target.value })}>
                  <option value="">Selecione…</option>
                  {state.plans.map((p) => (
                    <option key={p.id} value={p.id} disabled={!p.currentPrice}>
                      {p.name}
                      {p.currentPrice ? ` — ${formatMoney(p.currentPrice.amount, p.currentPrice.currency)}` : " (sem preço vigente)"}
                    </option>
                  ))}
                </select>
              </Field>
              <Field id="ms-branch" label="Unidade *" error={fieldErrors.branchId}>
                <select id="ms-branch" className={inputClass} value={form.branchId}
                  onChange={(e) => viewModel.updateForm({ branchId: e.target.value })}>
                  <option value="">Selecione…</option>
                  {state.branches.map((b) => (
                    <option key={b.id} value={b.id}>{b.name}</option>
                  ))}
                </select>
              </Field>
              <Field id="ms-start" label="Data de início" hint="Vazio = hoje. Data futura deixa a matrícula aguardando início.">
                <input id="ms-start" type="date" className={inputClass} value={form.startDate}
                  onChange={(e) => viewModel.updateForm({ startDate: e.target.value })} />
              </Field>
              {selectedPlan ? (
                <div className="self-end pb-2 text-xs text-slate-400">
                  Duração: {describeDuration(selectedPlan.durationType, selectedPlan.durationValue)}
                </div>
              ) : null}
            </div>
          </div>

          {canDiscount ? (
            <div className="space-y-3">
              <h2 className="text-sm font-semibold text-white">3. Desconto (opcional)</h2>
              <div className="grid gap-3 sm:grid-cols-3">
                <Field id="ms-discount" label="Valor (R$)" error={fieldErrors.discountAmount}>
                  <input id="ms-discount" inputMode="decimal" className={inputClass} placeholder="0,00"
                    value={form.discountAmount} onChange={(e) => viewModel.updateForm({ discountAmount: e.target.value })} />
                </Field>
                <Field id="ms-discount-reason" label="Motivo" error={fieldErrors.discountReason} className="sm:col-span-2">
                  <input id="ms-discount-reason" className={inputClass} value={form.discountReason}
                    onChange={(e) => viewModel.updateForm({ discountReason: e.target.value })} />
                </Field>
              </div>
            </div>
          ) : null}

          <Field id="ms-notes" label="Observações">
            <textarea id="ms-notes" rows={2} className={inputClass} value={form.notes}
              onChange={(e) => viewModel.updateForm({ notes: e.target.value })} />
          </Field>

          <div className="flex justify-end">
            <button
              type="button"
              disabled={state.quoting || !!blockedStudent}
              onClick={() => void viewModel.requestQuote()}
              className={quote ? secondaryButtonClass : primaryButtonClass}
            >
              {state.quoting ? "Calculando…" : quote ? "Recalcular resumo" : "Calcular resumo"}
            </button>
          </div>
        </section>

        <aside className={`${panelClass} space-y-4 p-5 lg:col-span-2`} aria-live="polite">
          <h2 className="text-sm font-semibold text-white">Resumo da contratação</h2>
          {!quote ? (
            <p className="text-sm text-slate-400">
              Preencha os dados e clique em “Calcular resumo”. Os valores e datas vêm do servidor e não podem ser editados aqui.
            </p>
          ) : (
            <>
              <dl className="grid grid-cols-2 gap-4">
                <InfoItem label="Aluno">{quote.studentName}</InfoItem>
                <InfoItem label="Plano">{quote.planName}</InfoItem>
                <InfoItem label="Unidade">{quote.branchName}</InfoItem>
                <InfoItem label="Status inicial">
                  <StatusBadge className={MEMBERSHIP_STATUS_STYLES[quote.initialStatus]}>
                    {MEMBERSHIP_STATUS_LABELS[quote.initialStatus]}
                  </StatusBadge>
                </InfoItem>
                <InfoItem label="Início">{formatLocalDate(quote.startDate)}</InfoItem>
                <InfoItem label="Fim">{formatLocalDate(quote.endDate)}</InfoItem>
                <InfoItem label="Duração">
                  {describeDuration(quote.conditions.durationType, quote.conditions.durationValue)}
                </InfoItem>
                <InfoItem label="Cobrança">
                  {describeBilling(quote.conditions.billingPeriod, quote.conditions.billingInterval)}
                </InfoItem>
              </dl>
              <div className="space-y-1 rounded-xl border border-white/10 bg-surface-950/50 p-4 text-sm">
                <div className="flex justify-between text-slate-400">
                  <span>Valor do plano</span>
                  <span>{formatMoney(quote.conditions.amount, quote.conditions.currency)}</span>
                </div>
                {Number(quote.conditions.discountAmount) > 0 ? (
                  <div className="flex justify-between text-amber-300">
                    <span>Desconto</span>
                    <span>− {formatMoney(quote.conditions.discountAmount, quote.conditions.currency)}</span>
                  </div>
                ) : null}
                <div className="flex justify-between border-t border-white/10 pt-2 font-semibold text-white">
                  <span>Valor final</span>
                  <span>{formatMoney(quote.conditions.finalAmount, quote.conditions.currency)}</span>
                </div>
                {Number(quote.conditions.setupFee) > 0 ? (
                  <div className="flex justify-between text-xs text-slate-400">
                    <span>Taxa de adesão</span>
                    <span>{formatMoney(quote.conditions.setupFee, quote.conditions.currency)}</span>
                  </div>
                ) : null}
              </div>
              <p className="text-xs text-slate-500">
                Ao confirmar, essas condições ficam gravadas na matrícula e não mudam se o plano for alterado depois.
              </p>
              <button
                type="button"
                disabled={state.saving}
                onClick={() => void handleConfirm()}
                className={`w-full ${primaryButtonClass}`}
              >
                {state.saving ? "Criando…" : "Confirmar matrícula"}
              </button>
            </>
          )}
        </aside>
      </div>
    </PageShell>
  );
}

export default function NewMembershipPage() {
  return (
    <RequireAuth>
      <NewMembershipContent />
    </RequireAuth>
  );
}
