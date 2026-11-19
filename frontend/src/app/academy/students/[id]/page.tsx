"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo } from "react";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import {
  EMPTY_DOCUMENT_FORM,
  STUDENT_STATUS_LABELS,
  STUDENT_STATUSES,
  type DocumentType,
  type StudentStatus,
} from "@/features/students/model/types";
import { StudentDetailViewModel, type DetailTab } from "@/features/students/viewmodel/StudentDetailViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";
const tabs: { id: DetailTab; label: string }[] = [
  { id: "overview", label: "Visão geral" },
  { id: "documents", label: "Documentos" },
  { id: "contacts", label: "Contatos" },
  { id: "address", label: "Endereço" },
  { id: "branches", label: "Unidades" },
  { id: "history", label: "Histórico" },
  { id: "notes", label: "Observações" },
];

function formatDate(value: string | null | undefined) {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleDateString("pt-BR");
}

function formatDateTime(value: string | null | undefined) {
  if (!value) return "—";
  const d = new Date(value);
  return Number.isNaN(d.getTime()) ? value : d.toLocaleString("pt-BR");
}

export default function StudentDetailPage() {
  const params = useParams<{ id: string }>();
  const id = params.id;
  const viewModel = useMemo(() => new StudentDetailViewModel(), []);
  const state = useViewModel(viewModel);
  const can = usePermission();

  useEffect(() => { void viewModel.load(id); }, [viewModel, id]);

  const student = state.student;

  return (
    <PageShell>
      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      {state.loading || !student ? (
        <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando aluno…</div>
      ) : (
        <div className="space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Aluno</p>
              <h1 className="text-2xl font-semibold text-white">{student.name}</h1>
              <p className="text-sm text-slate-400">
                Matrícula <span className="font-mono text-slate-200">{student.registrationNumber}</span> · CPF {student.cpfMasked ?? "—"} · {STUDENT_STATUS_LABELS[student.status]}
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <Link href={`/academy/memberships?studentId=${id}`} className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:text-white">Matrículas</Link>
              {can("charges:read", "payment:read") ? (
                <Link href={`/academy/billing?studentId=${id}`} className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:text-white">Financeiro</Link>
              ) : null}
              <Link href={`/academy/memberships/new?studentId=${id}`} className="rounded-xl border border-accent-500/30 bg-accent-500/10 px-4 py-2 text-sm font-semibold text-accent-400 transition hover:bg-accent-500/20">Nova matrícula</Link>
              <Link href={`/academy/students/${id}/edit`} className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:text-white">Editar</Link>
              <Link href="/academy/students" className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:text-white">Voltar</Link>
            </div>
          </div>

          <div className="flex flex-wrap gap-3 rounded-2xl border border-white/10 bg-surface-900/60 p-4">
            <div className="space-y-1">
              <label htmlFor="status-change" className={labelClass}>Alterar status</label>
              <select
                id="status-change"
                className={inputClass}
                value={student.status}
                disabled={state.saving}
                onChange={(e) => void viewModel.changeStatus(e.target.value as StudentStatus)}
              >
                {STUDENT_STATUSES.map((s) => <option key={s} value={s}>{STUDENT_STATUS_LABELS[s]}</option>)}
              </select>
            </div>
            <div className="flex items-end">
              <button type="button" onClick={() => void viewModel.toggleReveal()} disabled={state.saving} className="rounded-xl border border-accent-500/30 bg-accent-500/10 px-4 py-2 text-sm font-semibold text-accent-400 transition hover:bg-accent-500/20 disabled:opacity-50">
                {state.revealSensitive ? "Ocultar documentos" : "Revelar documentos"}
              </button>
            </div>
          </div>

          <div className="flex flex-wrap gap-2 border-b border-white/10 pb-2">
            {tabs.map((tab) => (
              <button key={tab.id} type="button" onClick={() => viewModel.setTab(tab.id)} className={`rounded-lg px-3 py-1.5 text-sm transition ${state.tab === tab.id ? "bg-accent-500/15 text-accent-400" : "text-slate-400 hover:text-white"}`}>
                {tab.label}
              </button>
            ))}
          </div>

          {state.tab === "overview" ? (
            <section className="grid gap-4 md:grid-cols-2">
              <div className="rounded-2xl border border-white/10 bg-surface-900/60 p-5 space-y-2 text-sm">
                <h2 className="text-base font-semibold text-white">Dados pessoais</h2>
                <p><span className="text-slate-400">Nome social:</span> {student.socialName ?? "—"}</p>
                <p><span className="text-slate-400">Nascimento:</span> {formatDate(student.birthDate)}</p>
                <p><span className="text-slate-400">E-mail:</span> {student.email ?? "—"}</p>
                <p><span className="text-slate-400">Telefone:</span> {student.phone ?? "—"}</p>
                <p><span className="text-slate-400">Gênero:</span> {student.gender ?? "—"}</p>
                <p><span className="text-slate-400">Criado em:</span> {formatDateTime(student.createdAt)}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-surface-900/60 p-5 space-y-2 text-sm">
                <h2 className="text-base font-semibold text-white">Observações cadastrais</h2>
                <p className="whitespace-pre-wrap text-slate-300">{student.notes ?? "Sem observações."}</p>
              </div>
            </section>
          ) : null}

          {state.tab === "documents" ? (
            <section className="space-y-4">
              <form className="grid gap-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5 md:grid-cols-3" onSubmit={(e) => { e.preventDefault(); void viewModel.saveDocument(); }}>
                <h2 className="md:col-span-3 text-base font-semibold text-white">{state.editingDocumentId ? "Editar documento" : "Adicionar documento"}</h2>
                <div className="space-y-1.5">
                  <label className={labelClass}>Tipo</label>
                  <select className={inputClass} value={state.documentForm.type} onChange={(e) => viewModel.setDocumentField("type", e.target.value as DocumentType)}>
                    {(["CPF","RG","CNH","PASSPORT","OTHER"] as DocumentType[]).map((t) => <option key={t} value={t}>{t}</option>)}
                  </select>
                </div>
                <div className="space-y-1.5"><label className={labelClass}>Número</label><input className={inputClass} value={state.documentForm.number} onChange={(e) => viewModel.setDocumentField("number", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>Emissor</label><input className={inputClass} value={state.documentForm.issuer} onChange={(e) => viewModel.setDocumentField("issuer", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>UF</label><input className={inputClass} value={state.documentForm.state} onChange={(e) => viewModel.setDocumentField("state", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>Emissão</label><input type="date" className={inputClass} value={state.documentForm.issuedAt} onChange={(e) => viewModel.setDocumentField("issuedAt", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>Validade</label><input type="date" className={inputClass} value={state.documentForm.expiresAt} onChange={(e) => viewModel.setDocumentField("expiresAt", e.target.value)} /></div>
                <label className="flex items-center gap-2 text-sm text-slate-300 md:col-span-3"><input type="checkbox" checked={state.documentForm.primary} onChange={(e) => viewModel.setDocumentField("primary", e.target.checked)} /> Documento principal</label>
                <div className="md:col-span-3 flex gap-2">
                  <button type="submit" disabled={state.saving} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Salvar</button>
                  {state.editingDocumentId ? <button type="button" onClick={viewModel.resetDocumentForm} className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300">Cancelar</button> : null}
                </div>
              </form>
              <div className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
                {student.documents.length === 0 ? <p className="px-5 py-8 text-sm text-slate-400">Nenhum documento cadastrado.</p> : (
                  <table className="w-full text-left text-sm">
                    <thead className="text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-5 py-3">Tipo</th><th className="px-5 py-3">Número</th><th className="px-5 py-3">Emissor</th><th className="px-5 py-3">Principal</th><th className="px-5 py-3 text-right">Ações</th></tr></thead>
                    <tbody className="divide-y divide-white/5">
                      {student.documents.map((doc) => (
                        <tr key={doc.id} className="text-slate-300">
                          <td className="px-5 py-3">{doc.type}</td>
                          <td className="px-5 py-3 font-mono text-xs">{doc.number}</td>
                          <td className="px-5 py-3">{doc.issuer ?? "—"}</td>
                          <td className="px-5 py-3">{doc.primary ? "Sim" : "Não"}</td>
                          <td className="px-5 py-3 text-right space-x-2">
                            <button type="button" onClick={() => viewModel.editDocument(doc)} className="text-xs text-accent-400 hover:underline">Editar</button>
                            <button type="button" onClick={() => void viewModel.removeDocument(doc.id)} className="text-xs text-red-300 hover:underline">Remover</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </section>
          ) : null}

          {state.tab === "contacts" ? (
            <section className="space-y-4">
              <form className="grid gap-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5 md:grid-cols-3" onSubmit={(e) => { e.preventDefault(); void viewModel.saveContact(); }}>
                <h2 className="md:col-span-3 text-base font-semibold text-white">{state.editingContactId ? "Editar contato" : "Adicionar contato"}</h2>
                <div className="space-y-1.5"><label className={labelClass}>Nome</label><input className={inputClass} value={state.contactForm.name} onChange={(e) => viewModel.setContactField("name", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>Parentesco/Relação</label><input className={inputClass} value={state.contactForm.relationship} onChange={(e) => viewModel.setContactField("relationship", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>Telefone</label><input className={inputClass} value={state.contactForm.phone} onChange={(e) => viewModel.setContactField("phone", e.target.value)} /></div>
                <div className="space-y-1.5"><label className={labelClass}>E-mail</label><input className={inputClass} value={state.contactForm.email} onChange={(e) => viewModel.setContactField("email", e.target.value)} /></div>
                <label className="flex items-center gap-2 text-sm text-slate-300 md:col-span-2"><input type="checkbox" checked={state.contactForm.primary} onChange={(e) => viewModel.setContactField("primary", e.target.checked)} /> Contato principal</label>
                <div className="md:col-span-3 space-y-1.5"><label className={labelClass}>Observações</label><input className={inputClass} value={state.contactForm.notes} onChange={(e) => viewModel.setContactField("notes", e.target.value)} /></div>
                <div className="md:col-span-3 flex gap-2">
                  <button type="submit" disabled={state.saving} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Salvar</button>
                  {state.editingContactId ? <button type="button" onClick={viewModel.resetContactForm} className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300">Cancelar</button> : null}
                </div>
              </form>
              <div className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
                {student.contacts.length === 0 ? <p className="px-5 py-8 text-sm text-slate-400">Nenhum contato cadastrado.</p> : (
                  <table className="w-full text-left text-sm">
                    <thead className="text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-5 py-3">Nome</th><th className="px-5 py-3">Relação</th><th className="px-5 py-3">Telefone</th><th className="px-5 py-3">Principal</th><th className="px-5 py-3 text-right">Ações</th></tr></thead>
                    <tbody className="divide-y divide-white/5">
                      {student.contacts.map((c) => (
                        <tr key={c.id} className="text-slate-300">
                          <td className="px-5 py-3">{c.name}</td><td className="px-5 py-3">{c.relationship ?? "—"}</td><td className="px-5 py-3">{c.phone}</td><td className="px-5 py-3">{c.primary ? "Sim" : "Não"}</td>
                          <td className="px-5 py-3 text-right space-x-2">
                            <button type="button" onClick={() => viewModel.editContact(c)} className="text-xs text-accent-400 hover:underline">Editar</button>
                            <button type="button" onClick={() => void viewModel.removeContact(c.id)} className="text-xs text-red-300 hover:underline">Remover</button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </section>
          ) : null}

          {state.tab === "address" ? (
            <form className="grid gap-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5 md:grid-cols-3" onSubmit={(e) => { e.preventDefault(); void viewModel.saveAddress(); }}>
              <h2 className="md:col-span-3 text-base font-semibold text-white">Endereço</h2>
              {(["zipCode","street","number","complement","neighborhood","city","state"] as const).map((field) => (
                <div key={field} className="space-y-1.5">
                  <label className={labelClass}>{{zipCode:"CEP",street:"Rua",number:"Número",complement:"Complemento",neighborhood:"Bairro",city:"Cidade",state:"UF"}[field]}</label>
                  <input className={inputClass} value={state.addressForm[field]} onChange={(e) => viewModel.setAddressField(field, e.target.value)} />
                </div>
              ))}
              <div className="md:col-span-3"><button type="submit" disabled={state.saving} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Salvar endereço</button></div>
            </form>
          ) : null}

          {state.tab === "branches" ? (
            <section className="space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <form className="space-y-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5" onSubmit={(e) => { e.preventDefault(); void viewModel.assignBranch(); }}>
                  <h2 className="text-base font-semibold text-white">Vincular unidade</h2>
                  <select className={inputClass} value={state.assignBranchId} onChange={(e) => viewModel.setAssignBranchId(e.target.value)}>
                    <option value="">Selecione</option>
                    {state.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                  </select>
                  <label className="flex items-center gap-2 text-sm text-slate-300"><input type="checkbox" checked={state.assignPrimary} onChange={(e) => viewModel.setAssignPrimary(e.target.checked)} /> Definir como principal</label>
                  <button type="submit" disabled={state.saving || !state.assignBranchId} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Vincular</button>
                </form>
                <form className="space-y-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5" onSubmit={(e) => { e.preventDefault(); void viewModel.transfer(); }}>
                  <h2 className="text-base font-semibold text-white">Transferir unidade principal</h2>
                  <select className={inputClass} value={state.transferBranchId} onChange={(e) => viewModel.setTransferBranchId(e.target.value)}>
                    <option value="">Selecione destino</option>
                    {state.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
                  </select>
                  <input className={inputClass} placeholder="Motivo (opcional)" value={state.transferReason} onChange={(e) => viewModel.setTransferReason(e.target.value)} />
                  <button type="submit" disabled={state.saving || !state.transferBranchId} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Transferir</button>
                </form>
              </div>
              <div className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
                {student.branches.length === 0 ? <p className="px-5 py-8 text-sm text-slate-400">Nenhum vínculo registrado.</p> : (
                  <table className="w-full text-left text-sm">
                    <thead className="text-xs uppercase tracking-wide text-slate-500"><tr><th className="px-5 py-3">Unidade</th><th className="px-5 py-3">Principal</th><th className="px-5 py-3">Início</th><th className="px-5 py-3">Fim</th><th className="px-5 py-3">Motivo</th><th className="px-5 py-3 text-right">Ações</th></tr></thead>
                    <tbody className="divide-y divide-white/5">
                      {student.branches.map((a) => (
                        <tr key={a.id} className="text-slate-300">
                          <td className="px-5 py-3">{a.branchName ?? a.branchId}</td>
                          <td className="px-5 py-3">{a.primary ? "Sim" : "Não"}</td>
                          <td className="px-5 py-3">{formatDateTime(a.startedAt)}</td>
                          <td className="px-5 py-3">{formatDateTime(a.endedAt)}</td>
                          <td className="px-5 py-3">{a.transferReason ?? "—"}</td>
                          <td className="px-5 py-3 text-right">{!a.endedAt ? <button type="button" onClick={() => void viewModel.removeBranch(a.id)} className="text-xs text-red-300 hover:underline">Encerrar</button> : "—"}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                )}
              </div>
            </section>
          ) : null}

          {state.tab === "history" ? (
            <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5">
              <h2 className="mb-4 text-base font-semibold text-white">Histórico</h2>
              {state.history.length === 0 ? <p className="text-sm text-slate-400">Nenhum evento registrado.</p> : (
                <ul className="space-y-3">
                  {state.history.map((entry, idx) => (
                    <li key={`${entry.occurredAt}-${idx}`} className="rounded-xl border border-white/5 bg-white/5 p-3 text-sm">
                      <div className="flex flex-wrap items-center justify-between gap-2">
                        <span className="font-medium text-white">{entry.title} <span className="text-xs text-slate-500">({entry.kind})</span></span>
                        <span className="text-xs text-slate-400">{formatDateTime(entry.occurredAt)}</span>
                      </div>
                      {entry.detail ? <p className="mt-1 text-slate-300">{entry.detail}</p> : null}
                    </li>
                  ))}
                </ul>
              )}
            </section>
          ) : null}

          {state.tab === "notes" ? (
            <section className="space-y-4">
              <form className="space-y-3 rounded-2xl border border-white/10 bg-surface-900/60 p-5" onSubmit={(e) => { e.preventDefault(); void viewModel.addNote(); }}>
                <h2 className="text-base font-semibold text-white">Nova observação administrativa</h2>
                <textarea className={`${inputClass} min-h-[100px]`} value={state.noteContent} onChange={(e) => viewModel.setNoteContent(e.target.value)} placeholder="Registrar observação..." />
                <button type="submit" disabled={state.saving || !state.noteContent.trim()} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 disabled:opacity-50">Adicionar</button>
              </form>
              <div className="space-y-3">
                {state.notes.length === 0 ? <p className="text-sm text-slate-400">Nenhuma observação registrada.</p> : state.notes.map((note) => (
                  <div key={note.id} className="rounded-2xl border border-white/10 bg-surface-900/60 p-4 text-sm">
                    <div className="flex items-center justify-between gap-2 text-xs text-slate-400"><span>{formatDateTime(note.createdAt)}</span></div>
                    <p className="mt-2 whitespace-pre-wrap text-slate-200">{note.content}</p>
                  </div>
                ))}
              </div>
            </section>
          ) : null}
        </div>
      )}
    </PageShell>
  );
}
