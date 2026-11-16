"use client";

import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Loading";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { authRepository } from "@/features/auth/data/AuthRepository";
import { BRANCH_STATUSES, type BranchInput, type BranchStatus } from "@/features/branches/model/types";
import { BranchesViewModel } from "@/features/branches/viewmodel/BranchesViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function BranchesContent() {
  const viewModel = useMemo(() => new BranchesViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load();
    void authRepository.me().catch(() => undefined);
  }, [viewModel]);

  function field(
    name: keyof BranchInput,
    label: string,
    options?: { type?: string; placeholder?: string; hint?: string; maxLength?: number },
  ) {
    const error = (state.formErrors as Record<string, string | undefined>)[name];
    return (
      <div className="space-y-1.5">
        <label htmlFor={name} className={labelClass}>{label}</label>
        <input
          id={name}
          type={options?.type ?? "text"}
          maxLength={options?.maxLength}
          className={inputClass}
          placeholder={options?.placeholder}
          value={state.form[name]}
          onChange={(event) => viewModel.setField(name, event.target.value)}
        />
        {error ? (
          <p className="text-[11px] text-red-300">{error}</p>
        ) : options?.hint ? (
          <p className="text-[11px] text-slate-500">{options.hint}</p>
        ) : null}
      </div>
    );
  }

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Administração da academia</p>
          <h1 className="text-2xl font-semibold text-white">Unidades</h1>
          <p className="text-sm text-slate-400">
            Gerencie as unidades da academia ativa, seus endereços e configurações operacionais.
          </p>
        </div>
        <button
          type="button"
          onClick={viewModel.openCreate}
          className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Nova unidade
        </button>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
        <form
          className="flex flex-wrap items-end gap-3 border-b border-white/5 px-5 py-4"
          onSubmit={(event) => {
            event.preventDefault();
            viewModel.applyFilters();
          }}
        >
          <div className="min-w-[220px] flex-1 space-y-1.5">
            <label htmlFor="branch-search" className={labelClass}>Buscar</label>
            <input
              id="branch-search"
              className={inputClass}
              placeholder="Nome, código ou cidade"
              value={state.search}
              onChange={(event) => viewModel.setSearch(event.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="branch-status" className={labelClass}>Status</label>
            <select
              id="branch-status"
              className={inputClass}
              value={state.statusFilter}
              onChange={(event) => viewModel.setStatusFilter(event.target.value)}
            >
              <option value="">Todos</option>
              {BRANCH_STATUSES.map((status) => (
                <option key={status} value={status}>{status}</option>
              ))}
            </select>
          </div>
          <button
            type="submit"
            className="rounded-xl border border-accent-500/30 bg-accent-500/10 px-4 py-2 text-sm font-semibold text-accent-400 transition hover:bg-accent-500/20"
          >
            Filtrar
          </button>
        </form>

        {state.loading ? (
          <div className="flex items-center gap-3 px-5 py-10 text-sm text-slate-400">
            <Spinner /> Carregando unidades…
          </div>
        ) : state.items.length === 0 ? (
          <p className="px-5 py-10 text-sm text-slate-400">Nenhuma unidade encontrada com os filtros atuais.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">Unidade</th>
                  <th className="px-5 py-3 font-medium">Cidade / UF</th>
                  <th className="px-5 py-3 font-medium">Telefone</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((branch) => (
                  <tr key={branch.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{branch.name}</p>
                      <p className="font-mono text-[11px] text-slate-500">{branch.code}</p>
                    </td>
                    <td className="px-5 py-3 text-slate-400">
                      {branch.city ? `${branch.city}${branch.state ? ` / ${branch.state}` : ""}` : "—"}
                    </td>
                    <td className="px-5 py-3 text-slate-400">{branch.phone ?? "—"}</td>
                    <td className="px-5 py-3">
                      <span
                        className={`rounded-full border px-2 py-0.5 text-[11px] font-medium ${
                          branch.status === "ACTIVE"
                            ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                            : "border-slate-500/20 bg-slate-500/10 text-slate-300"
                        }`}
                      >
                        {branch.status}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => void viewModel.openEdit(branch.id)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => void viewModel.openSettings(branch.id, branch.name)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Configurações
                        </button>
                        <select
                          aria-label={`Alterar status de ${branch.name}`}
                          value={branch.status}
                          disabled={state.statusUpdatingId === branch.id}
                          onChange={(event) => void viewModel.changeStatus(branch.id, event.target.value as BranchStatus)}
                          className="rounded-lg border border-white/10 bg-surface-950/70 px-2 py-1 text-xs text-slate-200 outline-none focus:border-accent-500/60 disabled:opacity-50"
                        >
                          {BRANCH_STATUSES.map((status) => (
                            <option key={status} value={status}>{status}</option>
                          ))}
                        </select>
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

      {state.formOpen ? (
        <Modal
          title={state.editingId ? "Editar unidade" : "Nova unidade"}
          description="Endereço e contato são opcionais; nome e código são obrigatórios."
          onClose={viewModel.closeForm}
          wide
        >
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              void viewModel.save();
            }}
            noValidate
          >
            <div className="grid gap-4 sm:grid-cols-2">
              {field("name", "Nome", { placeholder: "Unidade Centro" })}
              {field("code", "Código", { placeholder: "CENTRO", hint: "Letras, números, - ou _ (2 a 30)." })}
              {field("document", "CPF / CNPJ", { placeholder: "Somente números" })}
              {field("email", "E-mail", { type: "email", placeholder: "unidade@academia.com" })}
              {field("phone", "Telefone", { placeholder: "(11) 4000-0000" })}
              {field("zipCode", "CEP", { placeholder: "01001-000" })}
              {field("street", "Logradouro", { placeholder: "Praça da Sé" })}
              {field("number", "Número", { placeholder: "1" })}
              {field("complement", "Complemento", { placeholder: "Térreo" })}
              {field("neighborhood", "Bairro", { placeholder: "Sé" })}
              {field("city", "Cidade", { placeholder: "São Paulo" })}
              {field("state", "UF", { placeholder: "SP", maxLength: 2 })}
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={viewModel.closeForm}
                className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={state.saving}
                className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-60"
              >
                {state.saving ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
                {state.editingId ? "Salvar alterações" : "Cadastrar unidade"}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {state.settingsOpen ? (
        <Modal
          title="Configurações da unidade"
          description={state.settingsBranchName}
          onClose={viewModel.closeSettings}
        >
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              void viewModel.saveSettings();
            }}
            noValidate
          >
            {state.settingsError ? <Alert>{state.settingsError}</Alert> : null}

            <div className="space-y-1.5">
              <label htmlFor="timezone" className={labelClass}>Fuso horário</label>
              <input
                id="timezone"
                className={inputClass}
                value={state.settingsForm.timezone}
                onChange={(event) => viewModel.setSettingsField("timezone", event.target.value)}
                placeholder="America/Sao_Paulo"
              />
            </div>

            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-1.5">
                <label htmlFor="openingTime" className={labelClass}>Abertura</label>
                <input
                  id="openingTime"
                  type="time"
                  className={inputClass}
                  value={state.settingsForm.openingTime}
                  onChange={(event) => viewModel.setSettingsField("openingTime", event.target.value)}
                />
              </div>
              <div className="space-y-1.5">
                <label htmlFor="closingTime" className={labelClass}>Fechamento</label>
                <input
                  id="closingTime"
                  type="time"
                  className={inputClass}
                  value={state.settingsForm.closingTime}
                  onChange={(event) => viewModel.setSettingsField("closingTime", event.target.value)}
                />
              </div>
            </div>

            <div className="space-y-1.5">
              <label htmlFor="maxCapacity" className={labelClass}>Capacidade máxima</label>
              <input
                id="maxCapacity"
                type="number"
                min={1}
                className={inputClass}
                value={state.settingsForm.maxCapacity}
                onChange={(event) => viewModel.setSettingsField("maxCapacity", event.target.value)}
                placeholder="300"
              />
            </div>

            <label className="flex items-center gap-3 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={state.settingsForm.allow24hAccess}
                onChange={(event) => viewModel.setSettingsField("allow24hAccess", event.target.checked)}
                className="h-4 w-4 rounded border-white/20 bg-surface-950"
              />
              Permitir acesso 24 horas
            </label>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={viewModel.closeSettings}
                className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={state.settingsSaving}
                className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-60"
              >
                {state.settingsSaving ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
                Salvar configurações
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function BranchesPage() {
  return (
    <RequireAuth requireTenant>
      <BranchesContent />
    </RequireAuth>
  );
}
