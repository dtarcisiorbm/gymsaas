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
import { USER_STATUSES, type UserStatus } from "@/features/users/model/types";
import { UsersViewModel } from "@/features/users/viewmodel/UsersViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function UsersContent() {
  const viewModel = useMemo(() => new UsersViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load();
    void viewModel.loadOptions();
    void authRepository.me().catch(() => undefined);
  }, [viewModel]);

  const editing = state.editingId !== null;

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Administração da academia</p>
          <h1 className="text-2xl font-semibold text-white">Usuários</h1>
          <p className="text-sm text-slate-400">
            Cadastre colaboradores, atribua papéis e vincule cada usuário às unidades permitidas.
          </p>
        </div>
        <button
          type="button"
          onClick={viewModel.openCreate}
          className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Novo usuário
        </button>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}
      {state.optionsError ? <Alert kind="info">{state.optionsError}</Alert> : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
        <form
          className="flex flex-wrap items-end gap-3 border-b border-white/5 px-5 py-4"
          onSubmit={(event) => {
            event.preventDefault();
            viewModel.applyFilters();
          }}
        >
          <div className="min-w-[220px] flex-1 space-y-1.5">
            <label htmlFor="user-search" className={labelClass}>Buscar</label>
            <input
              id="user-search"
              className={inputClass}
              placeholder="Nome ou e-mail"
              value={state.search}
              onChange={(event) => viewModel.setSearch(event.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="user-status" className={labelClass}>Status</label>
            <select
              id="user-status"
              className={inputClass}
              value={state.statusFilter}
              onChange={(event) => viewModel.setStatusFilter(event.target.value)}
            >
              <option value="">Todos</option>
              {USER_STATUSES.map((status) => (
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
            <Spinner /> Carregando usuários…
          </div>
        ) : state.items.length === 0 ? (
          <p className="px-5 py-10 text-sm text-slate-400">Nenhum usuário encontrado com os filtros atuais.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">Usuário</th>
                  <th className="px-5 py-3 font-medium">Telefone</th>
                  <th className="px-5 py-3 font-medium">Papéis</th>
                  <th className="px-5 py-3 font-medium">Unidades</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((user) => (
                  <tr key={user.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{user.fullName}</p>
                      <p className="text-xs text-slate-400">{user.email}</p>
                    </td>
                    <td className="px-5 py-3 text-slate-400">{user.phone ?? "—"}</td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap gap-1">
                        {user.roles.length === 0 ? (
                          <span className="text-xs text-slate-500">—</span>
                        ) : (
                          user.roles.map((role) => (
                            <span key={role} className="rounded bg-accent-500/10 px-1.5 py-0.5 text-[11px] text-accent-400">
                              {role}
                            </span>
                          ))
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3 text-xs text-slate-400">{user.branchIds.length}</td>
                    <td className="px-5 py-3">
                      <span
                        className={`rounded-full border px-2 py-0.5 text-[11px] font-medium ${
                          user.status === "ACTIVE"
                            ? "border-emerald-500/20 bg-emerald-500/10 text-emerald-300"
                            : user.status === "LOCKED"
                              ? "border-amber-500/20 bg-amber-500/10 text-amber-300"
                              : "border-slate-500/20 bg-slate-500/10 text-slate-300"
                        }`}
                      >
                        {user.status}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <button
                          type="button"
                          onClick={() => viewModel.openEdit(user.id)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Editar
                        </button>
                        <button
                          type="button"
                          onClick={() => viewModel.openRoles(user.id)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Papéis
                        </button>
                        <button
                          type="button"
                          onClick={() => viewModel.openBranches(user.id)}
                          className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
                        >
                          Unidades
                        </button>
                        <select
                          aria-label={`Alterar status de ${user.fullName}`}
                          value={user.status}
                          disabled={state.statusUpdatingId === user.id}
                          onChange={(event) => void viewModel.changeStatus(user.id, event.target.value as UserStatus)}
                          className="rounded-lg border border-white/10 bg-surface-950/70 px-2 py-1 text-xs text-slate-200 outline-none focus:border-accent-500/60 disabled:opacity-50"
                        >
                          {USER_STATUSES.map((status) => (
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
          title={editing ? "Editar usuário" : "Novo usuário"}
          description={
            editing
              ? "Atualize os dados cadastrais. Papéis e unidades são gerenciados em ações próprias."
              : "O usuário é criado e vinculado à academia ativa com os papéis e unidades selecionados."
          }
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
              <div className="space-y-1.5">
                <label htmlFor="user-email" className={labelClass}>E-mail</label>
                <input
                  id="user-email"
                  type="email"
                  className={inputClass}
                  value={state.form.email}
                  readOnly={editing}
                  disabled={editing}
                  onChange={(event) => viewModel.setField("email", event.target.value)}
                  placeholder="colaborador@academia.com"
                />
                {state.formErrors.email ? <p className="text-[11px] text-red-300">{state.formErrors.email}</p> : null}
              </div>
              <div className="space-y-1.5">
                <label htmlFor="user-name" className={labelClass}>Nome completo</label>
                <input
                  id="user-name"
                  className={inputClass}
                  value={state.form.fullName}
                  onChange={(event) => viewModel.setField("fullName", event.target.value)}
                  placeholder="Maria Silva"
                />
                {state.formErrors.fullName ? <p className="text-[11px] text-red-300">{state.formErrors.fullName}</p> : null}
              </div>
              {!editing ? (
                <div className="space-y-1.5">
                  <label htmlFor="user-password" className={labelClass}>Senha inicial</label>
                  <input
                    id="user-password"
                    type="password"
                    className={inputClass}
                    value={state.form.password}
                    onChange={(event) => viewModel.setField("password", event.target.value)}
                    placeholder="Mínimo de 10 caracteres"
                  />
                  {state.formErrors.password ? <p className="text-[11px] text-red-300">{state.formErrors.password}</p> : null}
                </div>
              ) : null}
              <div className="space-y-1.5">
                <label htmlFor="user-phone" className={labelClass}>Telefone</label>
                <input
                  id="user-phone"
                  className={inputClass}
                  value={state.form.phone}
                  onChange={(event) => viewModel.setField("phone", event.target.value)}
                  placeholder="(11) 90000-0000"
                />
                {state.formErrors.phone ? <p className="text-[11px] text-red-300">{state.formErrors.phone}</p> : null}
              </div>
            </div>

            {!editing ? (
              <div className="grid gap-4 sm:grid-cols-2">
                <fieldset className="space-y-2 rounded-xl border border-white/10 p-4">
                  <legend className="px-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Papéis</legend>
                  {state.roles.length === 0 ? (
                    <p className="text-xs text-slate-500">Nenhum papel disponível.</p>
                  ) : (
                    state.roles.map((role) => (
                      <label key={role.id} className="flex items-start gap-2 text-sm text-slate-300">
                        <input
                          type="checkbox"
                          checked={state.form.roleIds.includes(role.id)}
                          onChange={(event) => viewModel.toggleFormRole(role.id, event.target.checked)}
                          className="mt-1 h-4 w-4 rounded border-white/20 bg-surface-950"
                        />
                        <span>
                          <span className="block">{role.name}</span>
                          <span className="block font-mono text-[10px] text-slate-500">{role.code}</span>
                        </span>
                      </label>
                    ))
                  )}
                </fieldset>

                <fieldset className="space-y-2 rounded-xl border border-white/10 p-4">
                  <legend className="px-1 text-xs font-semibold uppercase tracking-wide text-slate-400">Unidades</legend>
                  {state.branches.length === 0 ? (
                    <p className="text-xs text-slate-500">Nenhuma unidade disponível.</p>
                  ) : (
                    state.branches.map((branch) => (
                      <label key={branch.id} className="flex items-start gap-2 text-sm text-slate-300">
                        <input
                          type="checkbox"
                          checked={state.form.branchIds.includes(branch.id)}
                          onChange={(event) => viewModel.toggleFormBranch(branch.id, event.target.checked)}
                          className="mt-1 h-4 w-4 rounded border-white/20 bg-surface-950"
                        />
                        <span>
                          <span className="block">{branch.name}</span>
                          <span className="block font-mono text-[10px] text-slate-500">
                            {branch.code} · {branch.status}
                          </span>
                        </span>
                      </label>
                    ))
                  )}
                </fieldset>
              </div>
            ) : null}

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
                {editing ? "Salvar alterações" : "Criar usuário"}
              </button>
            </div>
          </form>
        </Modal>
      ) : null}

      {state.rolesOpen ? (
        <Modal
          title="Papéis do usuário"
          description={state.rolesUserName}
          onClose={viewModel.closeRoles}
        >
          <div className="space-y-3">
            {state.rolesError ? <Alert>{state.rolesError}</Alert> : null}
            {state.roles.length === 0 ? (
              <p className="text-sm text-slate-400">Nenhum papel disponível para atribuição.</p>
            ) : (
              <div className="space-y-2">
                {state.roles.map((role) => (
                  <label key={role.id} className="flex items-start gap-3 rounded-xl border border-white/5 bg-white/[0.02] p-3 text-sm text-slate-300">
                    <input
                      type="checkbox"
                      checked={state.selectedRoleIds.includes(role.id)}
                      onChange={(event) => viewModel.toggleRole(role.id, event.target.checked)}
                      className="mt-1 h-4 w-4 rounded border-white/20 bg-surface-950"
                    />
                    <span>
                      <span className="block font-medium text-white">{role.name}</span>
                      <span className="block text-xs text-slate-400">{role.description ?? role.code}</span>
                      <span className="mt-1 block font-mono text-[10px] text-slate-500">
                        {role.permissions.length} permissão(ões)
                      </span>
                    </span>
                  </label>
                ))}
              </div>
            )}
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={viewModel.closeRoles}
                className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => void viewModel.saveRoles()}
                disabled={state.rolesSaving}
                className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-60"
              >
                {state.rolesSaving ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
                Salvar papéis
              </button>
            </div>
          </div>
        </Modal>
      ) : null}

      {state.branchesOpen ? (
        <Modal
          title="Unidades do usuário"
          description={state.branchesUserName}
          onClose={viewModel.closeBranches}
        >
          <div className="space-y-3">
            {state.branchesError ? <Alert>{state.branchesError}</Alert> : null}
            {state.branches.length === 0 ? (
              <p className="text-sm text-slate-400">Nenhuma unidade disponível.</p>
            ) : (
              <div className="space-y-2">
                {state.branches.map((branch) => (
                  <label key={branch.id} className="flex items-start gap-3 rounded-xl border border-white/5 bg-white/[0.02] p-3 text-sm text-slate-300">
                    <input
                      type="checkbox"
                      checked={state.selectedBranchIds.includes(branch.id)}
                      onChange={(event) => viewModel.toggleBranch(branch.id, event.target.checked)}
                      className="mt-1 h-4 w-4 rounded border-white/20 bg-surface-950"
                    />
                    <span>
                      <span className="block font-medium text-white">{branch.name}</span>
                      <span className="block font-mono text-[10px] text-slate-500">
                        {branch.code} · {branch.status}
                      </span>
                    </span>
                  </label>
                ))}
              </div>
            )}
            <p className="text-xs text-slate-500">
              Apenas unidades ativas podem ser vinculadas. Unidades inativas são rejeitadas pelo servidor.
            </p>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={viewModel.closeBranches}
                className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:border-white/20 hover:text-white"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => void viewModel.saveBranches()}
                disabled={state.branchesSaving}
                className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-60"
              >
                {state.branchesSaving ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
                Salvar unidades
              </button>
            </div>
          </div>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function UsersPage() {
  return (
    <RequireAuth requireTenant>
      <UsersContent />
    </RequireAuth>
  );
}
