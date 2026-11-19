"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { PlanFormFields } from "@/components/plans/PlanFormFields";
import { Alert } from "@/components/ui/Alert";
import { Field, StatusBadge } from "@/components/ui/Field";
import { inputClass, panelClass, primaryButtonClass, secondaryButtonClass, smallButtonClass } from "@/components/ui/formStyles";
import { Spinner } from "@/components/ui/Loading";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import {
  PLAN_STATUS_LABELS,
  PLAN_STATUS_STYLES,
  PLAN_STATUS_TRANSITIONS,
  PLAN_STATUSES,
  describeBilling,
  describeDuration,
} from "@/features/plans/model/types";
import { PlansViewModel } from "@/features/plans/viewmodel/PlansViewModel";

function sortIndicator(active: boolean, direction: "asc" | "desc") {
  if (!active) return "";
  return direction === "asc" ? " ↑" : " ↓";
}

function PlansContent() {
  const router = useRouter();
  const can = usePermission();
  const viewModel = useMemo(() => new PlansViewModel(), []);
  const state = useViewModel(viewModel);
  const canCreate = can("plans:create", "plan:manage");
  const canUpdate = can("plans:update", "plan:manage");

  useEffect(() => {
    void viewModel.load(0);
  }, [viewModel]);

  async function handleCreate() {
    const id = await viewModel.create();
    if (id) router.push(`/academy/plans/${id}`);
  }

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Comercial</p>
          <h1 className="text-2xl font-semibold text-white">Planos</h1>
          <p className="text-sm text-slate-400">Planos, histórico de preços e benefícios oferecidos pela academia.</p>
        </div>
        {canCreate ? (
          <button type="button" onClick={viewModel.openCreate} className={primaryButtonClass}>
            Novo plano
          </button>
        ) : null}
      </div>

      {state.error && !state.createOpen ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      <section className={`overflow-hidden ${panelClass}`}>
        <form
          className="grid gap-3 border-b border-white/5 px-5 py-4 md:grid-cols-4"
          onSubmit={(e) => {
            e.preventDefault();
            viewModel.applyFilters();
          }}
        >
          <Field id="plan-search" label="Buscar" className="md:col-span-2">
            <input
              id="plan-search"
              className={inputClass}
              placeholder="Nome ou código"
              value={state.search}
              onChange={(e) => viewModel.setSearch(e.target.value)}
            />
          </Field>
          <Field id="plan-status-filter" label="Status">
            <select
              id="plan-status-filter"
              className={inputClass}
              value={state.statusFilter}
              onChange={(e) => viewModel.setStatusFilter(e.target.value)}
            >
              <option value="">Todos</option>
              {PLAN_STATUSES.map((s) => (
                <option key={s} value={s}>{PLAN_STATUS_LABELS[s]}</option>
              ))}
            </select>
          </Field>
          <div className="flex items-end">
            <button type="submit" className={`w-full ${secondaryButtonClass}`}>Filtrar</button>
          </div>
        </form>

        {state.loading ? (
          <div className="flex items-center gap-3 px-5 py-8 text-sm text-slate-400"><Spinner /> Carregando planos…</div>
        ) : state.items.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-white">Nenhum plano encontrado</p>
            <p className="mt-1 text-sm text-slate-400">
              {canCreate ? "Cadastre o primeiro plano para começar a matricular alunos." : "Ajuste os filtros."}
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("name")} className="hover:text-white">
                      Plano{sortIndicator(state.sort === "name", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">Duração</th>
                  <th className="px-5 py-3 font-medium">Preço vigente</th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("status")} className="hover:text-white">
                      Status{sortIndicator(state.sort === "status", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((plan) => (
                  <tr key={plan.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <Link href={`/academy/plans/${plan.id}`} className="font-medium text-white hover:text-accent-400">
                        {plan.name}
                      </Link>
                      <p className="font-mono text-xs text-slate-500">{plan.code}</p>
                    </td>
                    <td className="px-5 py-3 text-xs">{describeDuration(plan.durationType, plan.durationValue)}</td>
                    <td className="px-5 py-3">
                      {plan.currentPrice ? (
                        <>
                          <p className="text-white">{formatMoney(plan.currentPrice.amount, plan.currentPrice.currency)}</p>
                          <p className="text-xs text-slate-500">
                            {describeBilling(plan.currentPrice.billingPeriod, plan.currentPrice.billingInterval)} · desde{" "}
                            {formatLocalDate(plan.currentPrice.validFrom)}
                          </p>
                        </>
                      ) : (
                        <span className="text-xs text-amber-300">Sem preço vigente</span>
                      )}
                    </td>
                    <td className="px-5 py-3">
                      <StatusBadge className={PLAN_STATUS_STYLES[plan.status]}>{PLAN_STATUS_LABELS[plan.status]}</StatusBadge>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <Link href={`/academy/plans/${plan.id}`} className={smallButtonClass}>Detalhes</Link>
                        {canUpdate
                          ? PLAN_STATUS_TRANSITIONS[plan.status].map((target) => (
                              <button
                                key={target}
                                type="button"
                                disabled={state.busyId === plan.id}
                                onClick={() => {
                                  if (target !== "ARCHIVED" || window.confirm(`Arquivar o plano ${plan.name}?`)) {
                                    void viewModel.changeStatus(plan.id, target);
                                  }
                                }}
                                className={smallButtonClass}
                              >
                                {target === "ACTIVE" ? "Ativar" : target === "INACTIVE" ? "Inativar" : "Arquivar"}
                              </button>
                            ))
                          : null}
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

      {state.createOpen ? (
        <Modal
          title="Novo plano"
          description="O preço é guardado em histórico imutável; matrículas gravam as condições no momento da contratação."
          onClose={viewModel.closeCreate}
          wide
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeCreate} className={secondaryButtonClass}>Cancelar</button>
              <button type="button" disabled={state.saving} onClick={() => void handleCreate()} className={primaryButtonClass}>
                {state.saving ? "Salvando…" : "Cadastrar plano"}
              </button>
            </div>
          }
        >
          {state.error ? <div className="mb-4"><Alert>{state.error}</Alert></div> : null}
          <PlanFormFields
            value={state.form}
            errors={state.formErrors}
            creating
            onChange={viewModel.updateForm}
            onPriceChange={viewModel.updatePrice}
          />
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function PlansPage() {
  return (
    <RequireAuth>
      <PlansContent />
    </RequireAuth>
  );
}
