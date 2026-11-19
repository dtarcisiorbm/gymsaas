"use client";

import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
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
import { formatDateTime, formatLocalDate, formatMoney, todayIso } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import { CONTRACT_STATUS_LABELS, CONTRACT_STATUS_STYLES } from "@/features/contracts/model/types";
import {
  EVENT_LABELS,
  FREEZE_STATUS_LABELS,
  MEMBERSHIP_STATUS_LABELS,
  MEMBERSHIP_STATUS_STYLES,
  availableActions,
} from "@/features/memberships/model/types";
import {
  MembershipDetailViewModel,
  type MembershipTab,
  type ReasonAction,
} from "@/features/memberships/viewmodel/MembershipDetailViewModel";
import { describeBilling, describeDuration } from "@/features/plans/model/types";

const tabs: { id: MembershipTab; label: string }[] = [
  { id: "overview", label: "Condições" },
  { id: "freezes", label: "Congelamentos" },
  { id: "contracts", label: "Contratos" },
  { id: "history", label: "Histórico" },
];

const REASON_COPY: Record<ReasonAction, { title: string; description: string; button: string; required: boolean }> = {
  cancel: {
    title: "Cancelar matrícula",
    description: "O cancelamento é definitivo, encerra congelamento em andamento e não gera efeito financeiro.",
    button: "Cancelar matrícula",
    required: true,
  },
  suspend: {
    title: "Suspender matrícula",
    description: "Suspensão administrativa: o acesso é bloqueado e a vigência não é estendida.",
    button: "Suspender",
    required: true,
  },
  reactivate: {
    title: "Reativar matrícula",
    description: "A matrícula volta a ficar ativa com a vigência atual.",
    button: "Reativar",
    required: true,
  },
  unfreeze: {
    title: "Encerrar congelamento",
    description: "Os dias planejados e não usados são devolvidos, antecipando o fim da vigência.",
    button: "Descongelar",
    required: false,
  },
};

function daysBetween(fromIso: string, toIso: string): number {
  const from = Date.UTC(+fromIso.slice(0, 4), +fromIso.slice(5, 7) - 1, +fromIso.slice(8, 10));
  const to = Date.UTC(+toIso.slice(0, 4), +toIso.slice(5, 7) - 1, +toIso.slice(8, 10));
  return Math.round((to - from) / 86_400_000) + 1;
}

function MembershipDetailContent() {
  const params = useParams<{ id: string }>();
  const router = useRouter();
  const can = usePermission();
  const viewModel = useMemo(() => new MembershipDetailViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load(params.id);
  }, [viewModel, params.id]);

  const m = state.membership;
  const actions = m ? availableActions(m) : null;
  const policy = m?.conditions.freezePolicy;
  const modalError = state.modal !== "none" && state.error ? <div className="mb-4"><Alert>{state.error}</Alert></div> : null;
  const reasonAction =
    state.modal === "cancel" || state.modal === "suspend" || state.modal === "reactivate" || state.modal === "unfreeze"
      ? state.modal
      : null;
  const currentContract = state.contracts.find((c) => c.status !== "SUPERSEDED");

  async function handleRenew() {
    const id = await viewModel.submitRenew();
    if (id) router.push(`/academy/memberships/${id}`);
  }

  async function handleGenerate() {
    const id = await viewModel.generateContract();
    if (id) router.push(`/academy/contracts/${id}`);
  }

  return (
    <PageShell>
      {state.error && state.modal === "none" ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      {state.loading ? (
        <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando matrícula…</div>
      ) : !m || !actions ? (
        <Link href="/academy/memberships" className={secondaryButtonClass}>Voltar para matrículas</Link>
      ) : (
        <div className="space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Matrícula {m.registrationNumber}</p>
              <h1 className="text-2xl font-semibold text-white">
                <Link href={`/academy/students/${m.studentId}`} className="hover:text-accent-400">{m.studentName}</Link>
              </h1>
              <p className="flex flex-wrap items-center gap-2 text-sm text-slate-400">
                <StatusBadge className={MEMBERSHIP_STATUS_STYLES[m.status]}>{MEMBERSHIP_STATUS_LABELS[m.status]}</StatusBadge>
                <Link href={`/academy/plans/${m.planId}`} className="hover:text-white">{m.planName}</Link>
                <span>· {m.branchName}</span>
                <span>· {formatLocalDate(m.startDate)} → {formatLocalDate(m.endDate)}</span>
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              {actions.renew && can("memberships:renew") ? (
                <button type="button" onClick={() => viewModel.openModal("renew")} className={primaryButtonClass}>Renovar</button>
              ) : null}
              {actions.freeze && can("memberships:freeze") ? (
                <button type="button" onClick={() => viewModel.openModal("freeze")} className={secondaryButtonClass}>Congelar</button>
              ) : null}
              {actions.unfreeze && can("memberships:freeze") ? (
                <button type="button" onClick={() => viewModel.openModal("unfreeze")} className={secondaryButtonClass}>Descongelar</button>
              ) : null}
              {actions.suspend && can("memberships:suspend") ? (
                <button type="button" onClick={() => viewModel.openModal("suspend")} className={secondaryButtonClass}>Suspender</button>
              ) : null}
              {actions.reactivate && can("memberships:suspend") ? (
                <button type="button" onClick={() => viewModel.openModal("reactivate")} className={secondaryButtonClass}>Reativar</button>
              ) : null}
              {actions.edit && can("memberships:update") ? (
                <button type="button" onClick={() => viewModel.openModal("edit")} className={secondaryButtonClass}>Editar</button>
              ) : null}
              {actions.cancel && can("memberships:cancel") ? (
                <button type="button" onClick={() => viewModel.openModal("cancel")} className={dangerButtonClass}>Cancelar</button>
              ) : null}
              <Link href="/academy/memberships" className={secondaryButtonClass}>Voltar</Link>
            </div>
          </div>

          {m.renewedFromId || m.renewedById ? (
            <div className="flex flex-wrap gap-4 text-sm">
              {m.renewedFromId ? (
                <Link href={`/academy/memberships/${m.renewedFromId}`} className="text-accent-400 hover:underline">← Matrícula anterior</Link>
              ) : null}
              {m.renewedById ? (
                <Link href={`/academy/memberships/${m.renewedById}`} className="text-accent-400 hover:underline">Renovação →</Link>
              ) : null}
            </div>
          ) : null}
          {m.status === "CANCELLED" ? (
            <Alert kind="info">
              Cancelada em {formatDateTime(m.cancelledAt)}. Motivo: {m.cancellationReason ?? "—"}
            </Alert>
          ) : null}
          {currentContract?.status === "GENERATED" ? (
            <Alert kind="info">
              Contrato v{currentContract.contractVersion} aguardando aceite.{" "}
              <Link href={`/academy/contracts/${currentContract.id}`} className="underline">Abrir contrato</Link>
            </Alert>
          ) : null}

          <div className="flex flex-wrap gap-1 border-b border-white/10" role="tablist">
            {tabs.map((tab) => (
              <button
                key={tab.id}
                type="button"
                role="tab"
                aria-selected={state.tab === tab.id}
                onClick={() => viewModel.setTab(tab.id)}
                className={`-mb-px border-b-2 px-4 py-2 text-sm transition ${
                  state.tab === tab.id ? "border-accent-500 text-white" : "border-transparent text-slate-400 hover:text-white"
                }`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {state.tab === "overview" ? (
            <section className={`${panelClass} space-y-5 p-5`}>
              <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <InfoItem label="Valor contratado">{formatMoney(m.conditions.amount, m.conditions.currency)}</InfoItem>
                <InfoItem label="Desconto">
                  {Number(m.conditions.discountAmount) > 0
                    ? `${formatMoney(m.conditions.discountAmount, m.conditions.currency)} — ${m.conditions.discountReason ?? ""}`
                    : "—"}
                </InfoItem>
                <InfoItem label="Valor final">
                  <span className="font-semibold text-white">{formatMoney(m.conditions.finalAmount, m.conditions.currency)}</span>
                </InfoItem>
                <InfoItem label="Taxa de adesão">{formatMoney(m.conditions.setupFee, m.conditions.currency)}</InfoItem>
                <InfoItem label="Duração">{describeDuration(m.conditions.durationType, m.conditions.durationValue)}</InfoItem>
                <InfoItem label="Cobrança">{describeBilling(m.conditions.billingPeriod, m.conditions.billingInterval)}</InfoItem>
                <InfoItem label="Fim original">{formatLocalDate(m.originalEndDate)}</InfoItem>
                <InfoItem label="Ciclo">{m.termSequence}º · âncora {formatLocalDate(m.anchorDate)}</InfoItem>
                <InfoItem label="Congelamentos usados">
                  {m.freezesUsed}
                  {policy?.maxFreezes != null ? ` de ${policy.maxFreezes}` : ""}
                </InfoItem>
                <InfoItem label="Dias congelados">
                  {m.frozenDaysUsed}
                  {policy?.maxFreezeDays != null ? ` de ${policy.maxFreezeDays}` : ""}
                </InfoItem>
                <InfoItem label="Criada em">{formatDateTime(m.createdAt)}</InfoItem>
                <InfoItem label="Atualizada em">{formatDateTime(m.updatedAt)}</InfoItem>
              </dl>
              {m.notes ? (
                <div>
                  <p className="text-xs text-slate-500">Observações</p>
                  <p className="whitespace-pre-line text-sm text-slate-300">{m.notes}</p>
                </div>
              ) : null}
              <p className="text-xs text-slate-500">
                Estas condições foram gravadas na contratação e não mudam se o plano ou o preço forem alterados.
              </p>
            </section>
          ) : null}

          {state.tab === "freezes" ? (
            <section className={`overflow-hidden ${panelClass}`}>
              {m.freezes.length === 0 ? (
                <p className="px-5 py-8 text-center text-sm text-slate-400">Nenhum congelamento registrado.</p>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-medium">Período</th>
                      <th className="px-5 py-3 font-medium">Dias</th>
                      <th className="px-5 py-3 font-medium">Situação</th>
                      <th className="px-5 py-3 font-medium">Motivo</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-slate-300">
                    {m.freezes.map((f) => (
                      <tr key={f.id}>
                        <td className="px-5 py-3 text-xs">
                          {formatLocalDate(f.startDate)} → {formatLocalDate(f.actualEndDate ?? f.plannedEndDate)}
                          {f.actualEndDate && f.actualEndDate !== f.plannedEndDate ? (
                            <span className="block text-slate-500">planejado até {formatLocalDate(f.plannedEndDate)}</span>
                          ) : null}
                        </td>
                        <td className="px-5 py-3 text-xs">
                          {f.effectiveDays ?? f.plannedDays}
                          {f.effectiveDays != null && f.effectiveDays !== f.plannedDays ? ` de ${f.plannedDays}` : ""}
                        </td>
                        <td className="px-5 py-3"><span className={badgeClass}>{FREEZE_STATUS_LABELS[f.status]}</span></td>
                        <td className="px-5 py-3 text-xs text-slate-400">{f.reason}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          ) : null}

          {state.tab === "contracts" ? (
            <section className={`overflow-hidden ${panelClass}`}>
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-white/5 px-5 py-4">
                <p className="text-xs text-slate-500">
                  Gerar uma nova versão substitui o contrato ainda não aceito. Contrato aceito não é regenerado.
                </p>
                {can("contracts:generate") && m.status !== "CANCELLED" && currentContract?.status !== "ACCEPTED" ? (
                  <button type="button" disabled={state.saving} onClick={() => void handleGenerate()} className={primaryButtonClass}>
                    {state.saving ? "Gerando…" : currentContract ? "Gerar nova versão" : "Gerar contrato"}
                  </button>
                ) : null}
              </div>
              {state.contracts.length === 0 ? (
                <p className="px-5 py-8 text-center text-sm text-slate-400">Nenhum contrato gerado.</p>
              ) : (
                <table className="w-full text-left text-sm">
                  <thead className="text-xs uppercase tracking-wide text-slate-500">
                    <tr>
                      <th className="px-5 py-3 font-medium">Versão</th>
                      <th className="px-5 py-3 font-medium">Gerado em</th>
                      <th className="px-5 py-3 font-medium">Situação</th>
                      <th className="px-5 py-3 font-medium">Aceite</th>
                      <th className="px-5 py-3 text-right font-medium">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-white/5 text-slate-300">
                    {state.contracts.map((c) => (
                      <tr key={c.id}>
                        <td className="px-5 py-3">v{c.contractVersion}</td>
                        <td className="px-5 py-3 text-xs">{formatDateTime(c.generatedAt)}</td>
                        <td className="px-5 py-3">
                          <StatusBadge className={CONTRACT_STATUS_STYLES[c.status]}>{CONTRACT_STATUS_LABELS[c.status]}</StatusBadge>
                        </td>
                        <td className="px-5 py-3 text-xs">
                          {c.acceptance ? `${c.acceptance.acceptorName} · ${formatDateTime(c.acceptance.acceptedAt)}` : "—"}
                        </td>
                        <td className="px-5 py-3 text-right">
                          <Link href={`/academy/contracts/${c.id}`} className={smallButtonClass}>Abrir</Link>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </section>
          ) : null}

          {state.tab === "history" ? (
            <section className={`${panelClass} p-5`}>
              {state.history.length === 0 ? (
                <p className="text-center text-sm text-slate-400">Sem eventos registrados.</p>
              ) : (
                <ol className="space-y-4 border-l border-white/10 pl-5">
                  {state.history.map((h) => (
                    <li key={h.id} className="relative">
                      <span className="absolute -left-[25px] top-1.5 h-2 w-2 rounded-full bg-accent-500" aria-hidden />
                      <p className="text-sm text-white">
                        {EVENT_LABELS[h.type] ?? h.type}
                        {h.fromStatus && h.toStatus && h.fromStatus !== h.toStatus ? (
                          <span className="text-slate-400">
                            {" "}· {MEMBERSHIP_STATUS_LABELS[h.fromStatus]} → {MEMBERSHIP_STATUS_LABELS[h.toStatus]}
                          </span>
                        ) : null}
                      </p>
                      <p className="text-xs text-slate-500">{formatDateTime(h.changedAt)}</p>
                      {h.reason ? <p className="mt-1 text-sm text-slate-300">{h.reason}</p> : null}
                    </li>
                  ))}
                </ol>
              )}
            </section>
          ) : null}
        </div>
      )}

      {m && reasonAction ? (
        <Modal
          title={REASON_COPY[reasonAction].title}
          description={REASON_COPY[reasonAction].description}
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button
                type="button"
                disabled={state.saving}
                onClick={() => void viewModel.submitReason(reasonAction)}
                className={reasonAction === "cancel" ? dangerButtonClass : primaryButtonClass}
              >
                {state.saving ? "Salvando…" : REASON_COPY[reasonAction].button}
              </button>
            </div>
          }
        >
          {modalError}
          <Field id="reason" label={REASON_COPY[reasonAction].required ? "Motivo *" : "Motivo"} error={state.fieldErrors.reason}>
            <textarea id="reason" rows={3} className={inputClass} value={state.reason}
              onChange={(e) => viewModel.setReason(e.target.value)} />
          </Field>
        </Modal>
      ) : null}

      {m && state.modal === "freeze" ? (
        <Modal
          title="Congelar matrícula"
          description="O congelamento começa hoje e estende o fim da vigência pelos dias planejados."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.submitFreeze()} className={primaryButtonClass}>
                {state.saving ? "Salvando…" : "Congelar"}
              </button>
            </div>
          }
        >
          {modalError}
          <div className="space-y-4">
            <Field
              id="freeze-end"
              label="Congelar até *"
              error={state.fieldErrors.freezeEndDate}
              hint={state.freezeEndDate ? `${daysBetween(todayIso(), state.freezeEndDate)} dia(s) a partir de hoje.` : undefined}
            >
              <input id="freeze-end" type="date" min={todayIso()} className={inputClass} value={state.freezeEndDate}
                onChange={(e) => viewModel.setFreezeEndDate(e.target.value)} />
            </Field>
            <p className="text-xs text-slate-500">
              Limites do contrato: {policy?.maxFreezes ?? "∞"} congelamento(s), {policy?.maxFreezeDays ?? "∞"} dia(s) no total
              {policy?.minFreezeDays ? `, mínimo de ${policy.minFreezeDays} dia(s) por vez` : ""}. Já usados: {m.freezesUsed} /{" "}
              {m.frozenDaysUsed} dia(s).
            </p>
            <Field id="freeze-reason" label="Motivo *" error={state.fieldErrors.reason}>
              <textarea id="freeze-reason" rows={3} className={inputClass} value={state.reason}
                onChange={(e) => viewModel.setReason(e.target.value)} />
            </Field>
          </div>
        </Modal>
      ) : null}

      {m && state.modal === "renew" ? (
        <Modal
          title="Renovar matrícula"
          description="A renovação usa o preço vigente do plano na data de início. Desconto anterior não é herdado."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void handleRenew()} className={primaryButtonClass}>
                {state.saving ? "Renovando…" : "Renovar"}
              </button>
            </div>
          }
        >
          {modalError}
          <div className="grid gap-3 sm:grid-cols-2">
            <Field id="renew-start" label="Início" hint="Vazio = dia seguinte ao fim atual (ou hoje, se já expirou).">
              <input id="renew-start" type="date" className={inputClass} value={state.renewForm.startDate}
                onChange={(e) => viewModel.updateRenew({ startDate: e.target.value })} />
            </Field>
            <Field id="renew-branch" label="Unidade">
              <select id="renew-branch" className={inputClass} value={state.renewForm.branchId}
                onChange={(e) => viewModel.updateRenew({ branchId: e.target.value })}>
                {state.branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </Field>
            {can("memberships:apply_discount") ? (
              <>
                <Field id="renew-discount" label="Desconto (R$)" error={state.fieldErrors.discountAmount}>
                  <input id="renew-discount" inputMode="decimal" className={inputClass} value={state.renewForm.discountAmount}
                    onChange={(e) => viewModel.updateRenew({ discountAmount: e.target.value })} />
                </Field>
                <Field id="renew-discount-reason" label="Motivo do desconto" error={state.fieldErrors.discountReason}>
                  <input id="renew-discount-reason" className={inputClass} value={state.renewForm.discountReason}
                    onChange={(e) => viewModel.updateRenew({ discountReason: e.target.value })} />
                </Field>
              </>
            ) : null}
            <Field id="renew-notes" label="Observações" className="sm:col-span-2">
              <textarea id="renew-notes" rows={2} className={inputClass} value={state.renewForm.notes}
                onChange={(e) => viewModel.updateRenew({ notes: e.target.value })} />
            </Field>
          </div>
        </Modal>
      ) : null}

      {m && state.modal === "edit" ? (
        <Modal
          title="Editar matrícula"
          description="Somente unidade e observações podem ser alteradas. Condições contratadas são imutáveis."
          onClose={viewModel.closeModal}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeModal} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.submitEdit()} className={primaryButtonClass}>
                {state.saving ? "Salvando…" : "Salvar"}
              </button>
            </div>
          }
        >
          {modalError}
          <div className="space-y-3">
            <Field id="edit-branch" label="Unidade *" error={state.fieldErrors.editBranchId}>
              <select id="edit-branch" className={inputClass} value={state.editBranchId}
                onChange={(e) => viewModel.setEditBranch(e.target.value)}>
                {!state.branches.some((b) => b.id === m.branchId) ? (
                  <option value={m.branchId}>{m.branchName}</option>
                ) : null}
                {state.branches.map((b) => (
                  <option key={b.id} value={b.id}>{b.name}</option>
                ))}
              </select>
            </Field>
            <Field id="edit-notes" label="Observações">
              <textarea id="edit-notes" rows={3} className={inputClass} value={state.editNotes}
                onChange={(e) => viewModel.setEditNotes(e.target.value)} />
            </Field>
          </div>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function MembershipDetailPage() {
  return (
    <RequireAuth>
      <MembershipDetailContent />
    </RequireAuth>
  );
}
