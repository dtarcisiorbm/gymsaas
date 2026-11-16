"use client";

import { useEffect, useMemo } from "react";
import Link from "next/link";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Loading";
import { Modal } from "@/components/ui/Modal";
import { Pagination } from "@/components/ui/Pagination";
import { formatDate, formatDocument } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { authRepository } from "@/features/auth/data/AuthRepository";
import { TENANT_STATUSES, type TenantInput, type TenantStatus } from "@/features/tenants/model/types";
import { TenantsViewModel } from "@/features/tenants/viewmodel/TenantsViewModel";

const STATUS_STYLES: Record<TenantStatus, string> = {
  PENDING: "bg-amber-500/10 text-amber-300 border-amber-500/20",
  ACTIVE: "bg-emerald-500/10 text-emerald-300 border-emerald-500/20",
  SUSPENDED: "bg-orange-500/10 text-orange-300 border-orange-500/20",
  INACTIVE: "bg-slate-500/10 text-slate-300 border-slate-500/20",
};

function StatusBadge({ status }: { status: TenantStatus }) {
  return (
    <span className={`rounded-full border px-2 py-0.5 text-[11px] font-medium ${STATUS_STYLES[status] ?? STATUS_STYLES.INACTIVE}`}>
      {status}
    </span>
  );
}

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function TenantsContent() {
  const viewModel = useMemo(() => new TenantsViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load();
    void authRepository.me().catch(() => undefined);
  }, [viewModel]);

  function field(name: keyof TenantInput, label: string, options?: { type?: string; placeholder?: string; hint?: string }) {
    return (
      <div className="space-y-1.5">
        <label htmlFor={name} className={labelClass}>
          {label}
        </label>
        <input
          id={name}
          type={options?.type ?? "text"}
          className={inputClass}
          placeholder={options?.placeholder}
          value={state.form[name]}
          onChange={(event) => viewModel.setField(name, event.target.value)}
        />
        {state.formErrors[name] ? (
          <p className="text-[11px] text-red-300">{state.formErrors[name]}</p>
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
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Administração SaaS</p>
          <h1 className="text-2xl font-semibold text-white">Academias</h1>
          <p className="text-sm text-slate-400">
            Cadastre, edite e controle o ciclo de vida das academias da plataforma.
          </p>
        </div>
        <button
          type="button"
          onClick={viewModel.openCreate}
          className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Nova academia
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
            <label htmlFor="tenant-search" className={labelClass}>Buscar</label>
            <input
              id="tenant-search"
              className={inputClass}
              placeholder="Nome, razão social ou documento"
              value={state.search}
              onChange={(event) => viewModel.setSearch(event.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="tenant-status" className={labelClass}>Status</label>
            <select
              id="tenant-status"
              className={inputClass}
              value={state.statusFilter}
              onChange={(event) => viewModel.setStatusFilter(event.target.value)}
            >
              <option value="">Todos</option>
              {TENANT_STATUSES.map((status) => (
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
            <Spinner /> Carregando academias…
          </div>
        ) : state.items.length === 0 ? (
          <p className="px-5 py-10 text-sm text-slate-400">Nenhuma academia encontrada com os filtros atuais.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">Academia</th>
                  <th className="px-5 py-3 font-medium">Documento</th>
                  <th className="px-5 py-3 font-medium">Contato</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium">Criada em</th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((tenant) => (
                  <tr key={tenant.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <Link href={`/admin/tenants/${tenant.id}`} className="font-medium text-white hover:text-accent-300">
                        {tenant.name}
                      </Link>
                      <p className="font-mono text-[11px] text-slate-500">{tenant.id.slice(0, 8)}</p>
                    </td>
                    <td className="px-5 py-3 font-mono text-xs text-slate-400">{formatDocument(tenant.document)}</td>
                    <td className="px-5 py-3 text-slate-400">{tenant.email}</td>
                    <td className="px-5 py-3"><StatusBadge status={tenant.status} /></td>
                    <td className="px-5 py-3 text-xs text-slate-400">{formatDate(tenant.createdAt)}</td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => void viewModel.openEdit(tenant.id)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Editar
                        </button>
                        <select
                          aria-label={`Alterar status de ${tenant.name}`}
                          value={tenant.status}
                          disabled={state.statusUpdatingId === tenant.id}
                          onChange={(event) => void viewModel.changeStatus(tenant.id, event.target.value as TenantStatus)}
                          className="rounded-lg border border-white/10 bg-surface-950/70 px-2 py-1 text-xs text-slate-200 outline-none focus:border-accent-500/60 disabled:opacity-50"
                        >
                          {TENANT_STATUSES.map((status) => (
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
          title={state.editingId ? "Editar academia" : "Nova academia"}
          description={
            state.editingId
              ? "Alterações cadastrais não afetam usuários, unidades ou dados existentes."
              : "A academia é criada com status PENDING até a ativação."
          }
          onClose={viewModel.closeForm}
        >
          <form
            className="space-y-4"
            onSubmit={(event) => {
              event.preventDefault();
              void viewModel.save();
            }}
            noValidate
          >
            {field("name", "Nome fantasia", { placeholder: "Academia Central" })}
            {field("legalName", "Razão social", { placeholder: "Academia Central Ltda" })}
            {field("document", "CPF / CNPJ", { placeholder: "12.345.678/0001-99", hint: "Somente números ou formatado." })}
            {field("email", "E-mail", { type: "email", placeholder: "contato@academia.com" })}
            {field("phone", "Telefone", { placeholder: "(11) 4000-0000" })}

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
                {state.editingId ? "Salvar alterações" : "Cadastrar academia"}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function TenantsPage() {
  return (
    <RequireAuth requireTenant={false} requirePlatformAdmin>
      <TenantsContent />
    </RequireAuth>
  );
}
