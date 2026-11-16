"use client";

import Link from "next/link";
import { useEffect, useMemo } from "react";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Loading";
import { Pagination } from "@/components/ui/Pagination";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { STUDENT_STATUS_LABELS, STUDENT_STATUSES, type StudentStatus } from "@/features/students/model/types";
import { StudentsViewModel } from "@/features/students/viewmodel/StudentsViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function sortIndicator(active: boolean, direction: "asc" | "desc") {
  if (!active) return "";
  return direction === "asc" ? " ↑" : " ↓";
}

export default function StudentsPage() {
  const viewModel = useMemo(() => new StudentsViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load();
    void viewModel.loadBranches();
  }, [viewModel]);

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Gestão de alunos</p>
          <h1 className="text-2xl font-semibold text-white">Alunos</h1>
          <p className="text-sm text-slate-400">
            Cadastre, consulte e gerencie alunos, documentos, vínculos e histórico.
          </p>
        </div>
        <Link
          href="/academy/students/new"
          className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Novo aluno
        </Link>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
        <form
          className="grid gap-3 border-b border-white/5 px-5 py-4 md:grid-cols-6"
          onSubmit={(event) => {
            event.preventDefault();
            viewModel.applyFilters();
          }}
        >
          <div className="space-y-1.5 md:col-span-2">
            <label htmlFor="student-search" className={labelClass}>Buscar por nome</label>
            <input
              id="student-search"
              className={inputClass}
              placeholder="Nome ou nome social"
              value={state.search}
              onChange={(event) => viewModel.setSearch(event.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="student-document" className={labelClass}>Documento</label>
            <input
              id="student-document"
              className={inputClass}
              placeholder="CPF ou nº documento"
              value={state.document}
              onChange={(event) => viewModel.setDocument(event.target.value)}
            />
          </div>
          <div className="space-y-1.5">
            <label htmlFor="student-status" className={labelClass}>Status</label>
            <select
              id="student-status"
              className={inputClass}
              value={state.statusFilter}
              onChange={(event) => viewModel.setStatusFilter(event.target.value)}
            >
              <option value="">Todos</option>
              {STUDENT_STATUSES.map((status) => (
                <option key={status} value={status}>{STUDENT_STATUS_LABELS[status]}</option>
              ))}
            </select>
          </div>
          <div className="space-y-1.5">
            <label htmlFor="student-branch" className={labelClass}>Unidade</label>
            <select
              id="student-branch"
              className={inputClass}
              value={state.branchFilter}
              onChange={(event) => viewModel.setBranchFilter(event.target.value)}
            >
              <option value="">Todas</option>
              {state.branches.map((branch) => (
                <option key={branch.id} value={branch.id}>{branch.name}</option>
              ))}
            </select>
          </div>
          <div className="flex items-end">
            <button
              type="submit"
              className="w-full rounded-xl border border-accent-500/30 bg-accent-500/10 px-4 py-2 text-sm font-semibold text-accent-400 transition hover:bg-accent-500/20"
            >
              Filtrar
            </button>
          </div>
        </form>

        {state.loading ? (
          <div className="space-y-3 px-5 py-8">
            <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando alunos…</div>
            <div className="space-y-2">
              {[1, 2, 3, 4].map((i) => (
                <div key={i} className="h-12 animate-pulse rounded-xl bg-white/5" />
              ))}
            </div>
          </div>
        ) : state.items.length === 0 ? (
          <div className="px-5 py-12 text-center">
            <p className="text-sm font-medium text-white">Nenhum aluno encontrado</p>
            <p className="mt-1 text-sm text-slate-400">Ajuste os filtros ou cadastre um novo aluno.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("name")} className="hover:text-white">
                      Aluno{sortIndicator(state.sort === "name", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("registrationNumber")} className="hover:text-white">
                      Matrícula{sortIndicator(state.sort === "registrationNumber", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 font-medium">CPF</th>
                  <th className="px-5 py-3 font-medium">Unidade</th>
                  <th className="px-5 py-3 font-medium">
                    <button type="button" onClick={() => viewModel.toggleSort("status")} className="hover:text-white">
                      Status{sortIndicator(state.sort === "status", state.direction)}
                    </button>
                  </th>
                  <th className="px-5 py-3 text-right font-medium">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.items.map((student) => (
                  <tr key={student.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{student.name}</p>
                      {student.socialName ? <p className="text-xs text-slate-400">{student.socialName}</p> : null}
                    </td>
                    <td className="px-5 py-3 font-mono text-xs">{student.registrationNumber}</td>
                    <td className="px-5 py-3 text-xs text-slate-400">{student.cpfMasked ?? "—"}</td>
                    <td className="px-5 py-3 text-xs text-slate-400">{student.primaryBranchName ?? "—"}</td>
                    <td className="px-5 py-3">
                      <span className="rounded-full border border-white/10 bg-white/5 px-2 py-0.5 text-[11px]">
                        {STUDENT_STATUS_LABELS[student.status]}
                      </span>
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex flex-wrap items-center justify-end gap-2">
                        <Link href={`/academy/students/${student.id}`} className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white">Detalhes</Link>
                        <Link href={`/academy/students/${student.id}/edit`} className="rounded-lg border border-white/10 px-2.5 py-1 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white">Editar</Link>
                        <select
                          aria-label={`Alterar status de ${student.name}`}
                          value={student.status}
                          disabled={state.busyId === student.id}
                          onChange={(e) => void viewModel.changeStatus(student.id, e.target.value as StudentStatus)}
                          className="rounded-lg border border-white/10 bg-surface-950/70 px-2 py-1 text-xs text-slate-200 outline-none focus:border-accent-500/60 disabled:opacity-50"
                        >
                          {STUDENT_STATUSES.map((status) => (
                            <option key={status} value={status}>{STUDENT_STATUS_LABELS[status]}</option>
                          ))}
                        </select>
                        <button
                          type="button"
                          disabled={state.busyId === student.id}
                          onClick={() => {
                            if (window.confirm(`Excluir o aluno ${student.name}?`)) void viewModel.remove(student.id);
                          }}
                          className="rounded-lg border border-red-500/30 px-2.5 py-1 text-xs text-red-300 transition hover:bg-red-500/10 disabled:opacity-50"
                        >
                          Excluir
                        </button>
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
    </PageShell>
  );
}
