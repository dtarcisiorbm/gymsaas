"use client";

import Link from "next/link";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Field, StatusBadge } from "@/components/ui/Field";
import { inputClass, panelClass, primaryButtonClass, secondaryButtonClass, smallButtonClass } from "@/components/ui/formStyles";
import { Spinner } from "@/components/ui/Loading";
import { Pagination } from "@/components/ui/Pagination";
import { formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import {
  MEMBERSHIP_STATUS_LABELS,
  MEMBERSHIP_STATUS_STYLES,
  MEMBERSHIP_STATUSES,
} from "@/features/memberships/model/types";
import { BILLING_LABELS } from "@/features/plans/model/types";
import { MembershipsViewModel } from "@/features/memberships/viewmodel/MembershipsViewModel";

function sortIndicator(active: boolean, direction: "asc" | "desc") {
  if (!active) return "";
  return direction === "asc" ? " ↑" : " ↓";
}

function MembershipsContent() {
  const can = usePermission();
  const viewModel = useMemo(() => new MembershipsViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    // Read once on mount; avoids useSearchParams' Suspense requirement on static export.
    viewModel.init(new URLSearchParams(window.location.search).get("studentId"));
  }, [viewModel]);

  const newHref = state.studentFilter
    ? `/academy/memberships/new?studentId=${state.studentFilter}`
    : "/academy/memberships/new";

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Comercial</p>
          <h1 className="text-2xl font-semibold text-white">Matrículas</h1>
          <p className="text-sm text-slate-400">Contratações de planos, vigência, congelamentos, renovações e contratos.</p>
        </div>
        {can("memberships:create") ? (
          <Link href={newHref} className={primaryButtonClass}>Nova matrícula</Link>
        ) : null}
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.studentFilter ? (
        <div className="flex flex-wrap items-center gap-3 rounded-xl border border-sky-500/30 bg-sky-500/10 px-4 py-3 text-sm text-sky-200">
          <span>
            Mostrando apenas as matrículas do aluno
            {state.items[0] ? ` ${state.items[0].studentName}` : ""}.
          </span>
          <button type="button" onClick={viewModel.clearStudentFilter} className="underline hover:text-white">
            Ver todas
          </button>
        </div>
      ) : null}

      <section className={`overflow-hidden ${panelClass}`}>
        <form
          className="grid gap-3 border-b border-white/5 px-5 py-4 md:grid-cols-6"
          onSubmit={(e) => {
            e.preventDefault();
            viewModel.applyFilters();
          }}
        >
          <Field id="ms-search" label="Buscar" className="md:col-span-2">
            <input
              id="ms-search"
              className={inputClass}
              placeholder="Aluno ou nº da matrícula"
              value={state.search}
              onChange={(e) => viewModel.setFilter("search", e.target.value)}
            />
          </Field>
          <Field id="ms-status" label="Status">
            <select id="ms-status" className={inputClass} value={state.statusFilter}
              onChange={(e) => viewModel.setFilter("statusFilter", e.target.value)}>
              <option value="">Todos</option>
              {MEMBERSHIP_STATUSES.map((s) => (
                <option key={s} value={s}>{MEMBERSHIP_STATUS_LABELS[s]}</option>
              ))}
            </select>
          </Field>
          <Field id="ms-plan" label="Plano">
            <select id="ms-plan" className={inputClass} value={state.planFilter}
              onChange={(e) => viewModel.setFilter("planFilter", e.target.value)}>
              <option value="">Todos</option>
              {state.plans.map((p) => (
                <option key={p.id} value={p.id}>{p.name}</option>
              ))}
            </select>
          </Field>
          <Field id="ms-branch" label="Unidade" className="md:col-span-2">
            <select id="ms-branch" className={inputClass} value={state.branchFilter}
              onChange={(e) => viewModel.setFilter("branchFilter", e.target.value)}>
              <option value="">Todas</option>
              {state.branches.map((b) => (
                <option key={b.id} value={b.id}>{b.name}</option>
              ))}
            </select>
          </Field>
          <Field id="ms-ending-from" label="Vence de">
            <input id="ms-ending-from" type="date" className={inputClass} value={state.endingFrom}
              onChange={(e) => viewModel.setFilter("endingFrom", e.target.value)} />
          </Field>
          <Field id="ms-ending-to" label="Vence até">
            <input id="ms-ending-to" type="date" className={inputClass} value={state.endingTo}
              onChange={(e) => viewModel.setFilter("endingTo", e.target.value)} />
          </Field>
          <div className="flex items-end md:col-start-6">
            <button type="submit" className={`w-full ${secondaryButtonClass}`}>Filtrar</button>
          </div>
        </form>

        {state.loading ? (
          <div className="flex items-center gap-3 px-5 py-8 text-sm text-slate-400"><Spinner /> Carregando matrículas…</div>
        ) : state.items.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-white">Nenhuma matrícula encontrada</p>
            <p className="mt-1 text-sm text-slate-400">Ajuste os filtros ou crie uma nova matrícula.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("registrationNumber")} className="hover:text-white">
                      Matrícula{sortIndicator(state.sort === "registrationNumber", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">Aluno</th>
                  <th className="px-5 py-3 font-medium">Plano / unidade</th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("endDate")} className="hover:text-white">
                      Vigência{sortIndicator(state.sort === "endDate", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">Valor</th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("status")} className="hover:text-white">
                      Status{sortIndicator(state.sort === "status", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((m) => (
                  <tr key={m.id} className="text-slate-300">
                    <td className="px-5 py-3 font-mono text-xs">{m.registrationNumber}</td>
                    <td className="px-5 py-3">
                      <Link href={`/academy/students/${m.studentId}`} className="font-medium text-white hover:text-accent-400">
                        {m.studentName}
                      </Link>
                      <p className="font-mono text-xs text-slate-500">{m.studentRegistration}</p>
                    </td>
                    <td className="px-5 py-3">
                      <p>{m.planName}</p>
                      <p className="text-xs text-slate-500">{m.branchName}</p>
                    </td>
                    <td className="px-5 py-3 text-xs">
                      {formatLocalDate(m.startDate)} → {formatLocalDate(m.endDate)}
                    </td>
                    <td className="px-5 py-3">
                      <p className="text-white">{formatMoney(m.finalAmount, m.currency)}</p>
                      <p className="text-xs text-slate-500">{BILLING_LABELS[m.billingPeriod] ?? m.billingPeriod}</p>
                    </td>
                    <td className="px-5 py-3">
                      <StatusBadge className={MEMBERSHIP_STATUS_STYLES[m.status]}>{MEMBERSHIP_STATUS_LABELS[m.status]}</StatusBadge>
                    </td>
                    <td className="px-5 py-3 text-right">
                      <Link href={`/academy/memberships/${m.id}`} className={smallButtonClass}>Detalhes</Link>
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
    </PageShell>
  );
}

export default function MembershipsPage() {
  return (
    <RequireAuth>
      <MembershipsContent />
    </RequireAuth>
  );
}
