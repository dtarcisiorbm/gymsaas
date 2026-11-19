"use client";

import { useEffect, useMemo } from "react";
import { ChargesWorkspace } from "@/components/billing/ChargesWorkspace";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Field } from "@/components/ui/Field";
import { inputClass, panelClass, primaryButtonClass, secondaryButtonClass } from "@/components/ui/formStyles";
import { formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import { PAYMENT_METHOD_LABELS, type AmountView } from "@/features/billing/model/types";
import { ChargesViewModel } from "@/features/billing/viewmodel/ChargesViewModel";

function SummaryCard({ label, amount, currency, tone, hint }: {
  label: string;
  amount: AmountView;
  currency: string;
  tone: string;
  hint: string;
}) {
  return (
    <div className={`${panelClass} space-y-1 p-4`}>
      <p className="text-xs text-slate-500">{label}</p>
      <p className={`text-xl font-semibold ${tone}`}>{formatMoney(amount.total, currency)}</p>
      <p className="text-xs text-slate-500">{amount.count} {amount.count === 1 ? "lançamento" : "lançamentos"} · {hint}</p>
    </div>
  );
}

function BillingContent() {
  const can = usePermission();
  const canReport = can("billing:reports");
  const viewModel = useMemo(() => new ChargesViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    // Read once on mount; avoids useSearchParams' Suspense requirement on static export.
    const params = new URLSearchParams(window.location.search);
    viewModel.init({
      showSummary: false,
      studentId: params.get("studentId"),
      membershipId: params.get("membershipId"),
    });
  }, [viewModel]);

  useEffect(() => {
    viewModel.enableSummary(canReport);
  }, [viewModel, canReport]);

  const summary = state.summary;

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Financeiro</p>
          <h1 className="text-2xl font-semibold text-white">Cobranças e recebimentos</h1>
          <p className="text-sm text-slate-400">
            Parcelas geradas pelas matrículas, cobranças avulsas e recebimentos registrados manualmente.
          </p>
        </div>
        {can("charges:create") ? (
          <button type="button" onClick={() => void viewModel.openCreate()} className={primaryButtonClass}>
            Cobrança avulsa
          </button>
        ) : null}
      </div>

      {state.studentFilter || state.membershipFilter ? (
        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-sky-500/30 bg-sky-500/10 px-4 py-3 text-sm text-sky-200">
          <span>
            Mostrando apenas as cobranças {state.membershipFilter ? "da matrícula" : "do aluno"}
            {state.items[0]
              ? ` ${state.membershipFilter ? state.items[0].membershipRegistrationNumber ?? "" : state.items[0].studentName ?? ""}`
              : ""}.
          </span>
          <button type="button" onClick={viewModel.clearScope} className="underline hover:text-white">Ver todas</button>
        </div>
      ) : null}

      {canReport ? (
        <section className="space-y-3">
          <form
            className="flex flex-wrap items-end gap-3"
            onSubmit={(e) => {
              e.preventDefault();
              void viewModel.loadSummary();
            }}
          >
            <Field id="sum-from" label="Período de">
              <input id="sum-from" type="date" className={inputClass} value={state.summaryFrom}
                onChange={(e) => viewModel.setSummaryPeriod("summaryFrom", e.target.value)} />
            </Field>
            <Field id="sum-to" label="até">
              <input id="sum-to" type="date" className={inputClass} value={state.summaryTo}
                onChange={(e) => viewModel.setSummaryPeriod("summaryTo", e.target.value)} />
            </Field>
            <button type="submit" className={secondaryButtonClass}>Atualizar resumo</button>
          </form>
          {state.summaryError ? <Alert>{state.summaryError}</Alert> : null}
          {summary ? (
            <>
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                <SummaryCard label="A receber no período" amount={summary.receivable} currency={summary.currency}
                  tone="text-sky-300" hint="em aberto, ainda não vencido" />
                <SummaryCard label="Vencido" amount={summary.overdue} currency={summary.currency}
                  tone="text-red-300" hint={`em aberto antes de ${formatLocalDate(summary.today)}`} />
                <SummaryCard label="Recebido no período" amount={summary.received} currency={summary.currency}
                  tone="text-emerald-300" hint="confirmados" />
                <SummaryCard label="Estornado no período" amount={summary.refunded} currency={summary.currency}
                  tone="text-slate-200" hint="estornos registrados" />
              </div>
              {summary.receivedByMethod.length > 0 ? (
                <div className="flex flex-wrap gap-2 text-xs text-slate-400">
                  <span className="text-slate-500">Recebido por forma:</span>
                  {summary.receivedByMethod.map((m) => (
                    <span key={m.method} className="rounded-full border border-white/10 bg-white/5 px-2.5 py-1">
                      {PAYMENT_METHOD_LABELS[m.method]}: {formatMoney(m.total, summary.currency)} ({m.count})
                    </span>
                  ))}
                </div>
              ) : null}
              <p className="text-xs text-slate-500">
                Período {formatLocalDate(summary.from)} → {formatLocalDate(summary.to)}
                {state.branchFilter ? " · unidade filtrada" : ""}. O filtro de unidade da lista também se aplica ao resumo.
              </p>
            </>
          ) : null}
        </section>
      ) : null}

      <ChargesWorkspace viewModel={viewModel} />
    </PageShell>
  );
}

export default function BillingPage() {
  return (
    <RequireAuth>
      <BillingContent />
    </RequireAuth>
  );
}
