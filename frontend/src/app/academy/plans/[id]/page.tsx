"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { PlanFormFields, PriceFields } from "@/components/plans/PlanFormFields";
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
import { formatDateTime, formatLocalDate, formatMoney } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import {
  FEATURE_LABELS,
  FEATURE_TYPES,
  FEATURES_WITH_INT,
  PLAN_STATUS_LABELS,
  PLAN_STATUS_STYLES,
  PLAN_STATUS_TRANSITIONS,
  WEEKDAYS,
  describeBilling,
  describeDuration,
  type PlanFeatureType,
} from "@/features/plans/model/types";
import { PlanDetailViewModel } from "@/features/plans/viewmodel/PlanDetailViewModel";

function policyText(value: number | null | undefined, suffix: string) {
  return value == null ? "Sem limite" : `${value} ${suffix}`;
}

function PlanDetailContent() {
  const params = useParams<{ id: string }>();
  const can = usePermission();
  const viewModel = useMemo(() => new PlanDetailViewModel(), []);
  const state = useViewModel(viewModel);
  const canUpdate = can("plans:update", "plan:manage");
  const canManagePrices = can("plans:manage_prices", "plan:manage");

  useEffect(() => {
    void viewModel.load(params.id);
  }, [viewModel, params.id]);

  const plan = state.plan;
  const modalError = state.modal !== "none" && state.error ? <div className="mb-4"><Alert>{state.error}</Alert></div> : null;

  return (
    <PageShell>
      {state.error && state.modal === "none" ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      {state.loading ? (
        <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando plano…</div>
      ) : !plan ? (
        <Link href="/academy/plans" className={secondaryButtonClass}>Voltar para planos</Link>
      ) : (
        <div className="space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Plano</p>
              <h1 className="text-2xl font-semibold text-white">{plan.name}</h1>
              <p className="flex flex-wrap items-center gap-2 text-sm text-slate-400">
                <span className="font-mono text-slate-300">{plan.code}</span>
                <StatusBadge className={PLAN_STATUS_STYLES[plan.status]}>{PLAN_STATUS_LABELS[plan.status]}</StatusBadge>
                <span>versão {plan.version}</span>
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {canUpdate ? (
                <>
                  <button type="button" onClick={viewModel.openEdit} className={secondaryButtonClass}>Editar</button>
                  {PLAN_STATUS_TRANSITIONS[plan.status].map((target) => (
                    <button
                      key={target}
                      type="button"
                      disabled={state.saving}
                      onClick={() => {
                        if (target !== "ARCHIVED" || window.confirm("Arquivar este plano?")) void viewModel.changeStatus(target);
                      }}
                      className={target === "ARCHIVED" ? dangerButtonClass : secondaryButtonClass}
                    >
                      {target === "ACTIVE" ? "Ativar" : target === "INACTIVE" ? "Inativar" : "Arquivar"}
                    </button>
                  ))}
                </>
              ) : null}
              <Link href="/academy/plans" className={secondaryButtonClass}>Voltar</Link>
            </div>
          </div>

          {plan.status !== "ACTIVE" ? (
            <Alert kind="info">Planos que não estão ativos não aceitam novas matrículas nem renovações.</Alert>
          ) : !plan.currentPrice ? (
            <Alert kind="info">Este plano não tem preço vigente hoje e não aceita matrículas até que um preço entre em vigor.</Alert>
          ) : null}

          <section className={`${panelClass} p-5`}>
            <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <InfoItem label="Duração">{describeDuration(plan.durationType, plan.durationValue)}</InfoItem>
              <InfoItem label="Preço vigente">
                {plan.currentPrice
                  ? `${formatMoney(plan.currentPrice.amount, plan.currentPrice.currency)} · ${describeBilling(plan.currentPrice.billingPeriod, plan.currentPrice.billingInterval)}`
                  : "—"}
              </InfoItem>
              <InfoItem label="Congelamentos">{policyText(plan.freezePolicy?.maxFreezes, "por contrato")}</InfoItem>
              <InfoItem label="Dias de congelamento">
                {policyText(plan.freezePolicy?.maxFreezeDays, "no total")}
                {plan.freezePolicy?.minFreezeDays ? ` · mín. ${plan.freezePolicy.minFreezeDays}` : ""}
              </InfoItem>
            </dl>
            {plan.description ? <p className="mt-4 whitespace-pre-line text-sm text-slate-300">{plan.description}</p> : null}
            {plan.contractTerms ? (
              <details className="mt-4 text-sm">
                <summary className="cursor-pointer text-slate-400 hover:text-white">Cláusulas específicas do contrato</summary>
                <p className="mt-2 whitespace-pre-line text-slate-300">{plan.contractTerms}</p>
              </details>
            ) : null}
          </section>

          <section className={`overflow-hidden ${panelClass}`}>
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/5 px-5 py-4">
              <div>
                <h2 className="text-base font-semibold text-white">Histórico de preços</h2>
                <p className="text-xs text-slate-500">Preços são imutáveis e nunca retroativos. Matrículas existentes não mudam.</p>
              </div>
              {canManagePrices ? (
                <button type="button" onClick={viewModel.openPrice} className={primaryButtonClass}>Programar novo preço</button>
              ) : null}
            </div>
            {state.prices.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-slate-400">Nenhum preço cadastrado.</p>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-medium">Valor</th>
                      <th className="px-5 py-3 font-medium">Cobrança</th>
                      <th className="px-5 py-3 font-medium">Adesão</th>
                      <th className="px-5 py-3 font-medium">Vigência</th>
                      <th className="px-5 py-3 font-medium">Situação</th>
                      <th className="px-5 py-3 text-right font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5">
                    {state.prices.map((price) => (
                      <tr key={price.id} className={price.status === "CANCELLED" ? "text-slate-500" : "text-slate-300"}>
                        <td className="px-5 py-3 font-medium text-white">{formatMoney(price.amount, price.currency)}</td>
                        <td className="px-5 py-3 text-xs">{describeBilling(price.billingPeriod, price.billingInterval)}</td>
                        <td className="px-5 py-3 text-xs">{formatMoney(price.setupFee, price.currency)}</td>
                        <td className="px-5 py-3 text-xs">
                          {formatLocalDate(price.validFrom)} → {price.validUntil ? formatLocalDate(price.validUntil) : "sem fim"}
                        </td>
                        <td className="px-5 py-3">
                          {price.status === "CANCELLED" ? (
                            <span className={badgeClass}>Cancelado</span>
                          ) : price.current ? (
                            <StatusBadge className="border-emerald-500/30 bg-emerald-500/10 text-emerald-300">Vigente</StatusBadge>
                          ) : price.scheduled ? (
                            <StatusBadge className="border-sky-500/30 bg-sky-500/10 text-sky-300">Programado</StatusBadge>
                          ) : (
                            <span className={badgeClass}>Encerrado</span>
                          )}
                          {price.notes ? <p className="mt-1 text-xs text-slate-500">{price.notes}</p> : null}
                        </td>
                        <td className="px-5 py-3 text-right">
                          {canManagePrices && price.scheduled && price.status === "ACTIVE" ? (
                            <button type="button" onClick={() => viewModel.openCancelPrice(price.id)} className={smallButtonClass}>
                              Cancelar
                            </button>
                          ) : (
                            <span className="text-xs text-slate-600">{formatDateTime(price.createdAt)}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </section>

          <section className={`${panelClass}`}>
            <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/5 px-5 py-4">
              <div>
                <h2 className="text-base font-semibold text-white">Benefícios</h2>
                <p className="text-xs text-slate-500">Copiados para a matrícula no momento da contratação.</p>
              </div>
              {canUpdate ? (
                <div className="flex gap-2">
                  {state.featuresDirty ? (
                    <button type="button" onClick={viewModel.resetFeatures} className={secondaryButtonClass}>Descartar</button>
                  ) : null}
                  <button type="button" onClick={viewModel.addFeature} className={secondaryButtonClass}>Adicionar</button>
                  <button
                    type="button"
                    disabled={!state.featuresDirty || state.saving}
                    onClick={() => void viewModel.saveFeatures()}
                    className={primaryButtonClass}
                  >
                    Salvar benefícios
                  </button>
                </div>
              ) : null}
            </div>
            {state.features.length === 0 ? (
              <p className="px-5 py-8 text-center text-sm text-slate-400">Nenhum benefício configurado.</p>
            ) : (
              <ul className="divide-y divide-white/5">
                {state.features.map((feature, index) => (
                  <li key={index} className="grid gap-3 px-5 py-4 md:grid-cols-12">
                    <Field id={`feature-type-${index}`} label="Tipo" className="md:col-span-4">
                      <select
                        id={`feature-type-${index}`}
                        className={inputClass}
                        disabled={!canUpdate}
                        value={feature.type}
                        onChange={(e) => viewModel.updateFeature(index, { type: e.target.value as PlanFeatureType })}
                      >
                        {FEATURE_TYPES.map((t) => (
                          <option key={t} value={t}>{FEATURE_LABELS[t]}</option>
                        ))}
                      </select>
                    </Field>
                    <div className="md:col-span-7">
                      {feature.type === "BRANCH_ACCESS" ? (
                        <Field id={`feature-branch-${index}`} label="Unidade">
                          <select
                            id={`feature-branch-${index}`}
                            className={inputClass}
                            disabled={!canUpdate}
                            value={feature.branchId}
                            onChange={(e) => viewModel.updateFeature(index, { branchId: e.target.value })}
                          >
                            <option value="">Selecione…</option>
                            {state.branches.map((b) => (
                              <option key={b.id} value={b.id}>{b.name}</option>
                            ))}
                          </select>
                        </Field>
                      ) : feature.type === "TIME_WINDOW" ? (
                        <div className="flex flex-wrap items-end gap-3">
                          <Field id={`feature-start-${index}`} label="Início">
                            <input id={`feature-start-${index}`} type="time" className={inputClass} disabled={!canUpdate}
                              value={feature.startTime} onChange={(e) => viewModel.updateFeature(index, { startTime: e.target.value })} />
                          </Field>
                          <Field id={`feature-end-${index}`} label="Fim">
                            <input id={`feature-end-${index}`} type="time" className={inputClass} disabled={!canUpdate}
                              value={feature.endTime} onChange={(e) => viewModel.updateFeature(index, { endTime: e.target.value })} />
                          </Field>
                          <div className="flex flex-wrap gap-1" role="group" aria-label="Dias da semana">
                            {WEEKDAYS.map((d) => {
                              const on = (feature.daysOfWeek & d.bit) !== 0;
                              return (
                                <button
                                  key={d.bit}
                                  type="button"
                                  aria-pressed={on}
                                  disabled={!canUpdate}
                                  onClick={() => viewModel.toggleWeekday(index, d.bit)}
                                  className={`rounded-lg border px-2 py-1.5 text-xs transition ${
                                    on ? "border-accent-500/50 bg-accent-500/15 text-accent-400" : "border-white/10 text-slate-500"
                                  }`}
                                >
                                  {d.short}
                                </button>
                              );
                            })}
                          </div>
                        </div>
                      ) : FEATURES_WITH_INT.includes(feature.type) ? (
                        <Field id={`feature-int-${index}`} label="Quantidade">
                          <input id={`feature-int-${index}`} type="number" min={1} className={inputClass} disabled={!canUpdate}
                            value={feature.intValue} onChange={(e) => viewModel.updateFeature(index, { intValue: e.target.value })} />
                        </Field>
                      ) : feature.type === "CUSTOM" ? (
                        <div className="grid gap-3 sm:grid-cols-2">
                          <Field id={`feature-label-${index}`} label="Rótulo">
                            <input id={`feature-label-${index}`} className={inputClass} disabled={!canUpdate}
                              value={feature.label} onChange={(e) => viewModel.updateFeature(index, { label: e.target.value })} />
                          </Field>
                          <Field id={`feature-desc-${index}`} label="Descrição">
                            <input id={`feature-desc-${index}`} className={inputClass} disabled={!canUpdate}
                              value={feature.description} onChange={(e) => viewModel.updateFeature(index, { description: e.target.value })} />
                          </Field>
                        </div>
                      ) : (
                        <p className="pt-7 text-xs text-slate-500">Acesso liberado em todas as unidades da academia.</p>
                      )}
                    </div>
                    {canUpdate ? (
                      <div className="flex items-end justify-end md:col-span-1">
                        <button
                          type="button"
                          aria-label={`Remover benefício ${index + 1}`}
                          onClick={() => viewModel.removeFeature(index)}
                          className="rounded-lg border border-red-500/30 px-2.5 py-2 text-xs text-red-300 transition hover:bg-red-500/10"
                        >
                          ✕
                        </button>
                      </div>
                    ) : null}
                  </li>
                ))}
              </ul>
            )}
          </section>
        </div>
      )}

      {state.modal === "edit" && plan ? (
        <Modal
          title="Editar plano"
          onClose={viewModel.closeModal}
          wide
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Cancelar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.saveEdit()} className={primaryButtonClass}>
                {state.saving ? "Salvando…" : "Salvar"}
              </button>
            </div>
          }
        >
          {modalError}
          <PlanFormFields value={state.form} errors={state.formErrors} creating={false} onChange={viewModel.updateForm} />
        </Modal>
      ) : null}

      {state.modal === "price" ? (
        <Modal
          title="Programar novo preço"
          description="O preço vigente será encerrado na véspera da data de início. Matrículas já feitas mantêm o valor contratado."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Cancelar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.savePrice()} className={primaryButtonClass}>
                {state.saving ? "Salvando…" : "Programar preço"}
              </button>
            </div>
          }
        >
          {modalError}
          <PriceFields value={state.priceForm} errors={state.formErrors} onChange={viewModel.updatePriceForm} />
        </Modal>
      ) : null}

      {state.modal === "cancelPrice" ? (
        <Modal
          title="Cancelar preço programado"
          description="O preço anterior volta a valer sem data de término."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.cancelPrice()} className={dangerButtonClass}>
                Cancelar preço
              </button>
            </div>
          }
        >
          {modalError}
          <Field id="cancel-price-reason" label="Motivo *">
            <textarea
              id="cancel-price-reason"
              className={inputClass}
              rows={3}
              value={state.cancelReason}
              onChange={(e) => viewModel.setCancelReason(e.target.value)}
            />
          </Field>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function PlanDetailPage() {
  return (
    <RequireAuth>
      <PlanDetailContent />
    </RequireAuth>
  );
}
