"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo } from "react";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { STUDENT_STATUSES, STUDENT_STATUS_LABELS, type StudentInput } from "@/features/students/model/types";
import { StudentFormViewModel } from "@/features/students/viewmodel/StudentFormViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function Field({
  id, label, error, children,
}: { id: string; label: string; error?: string; children: React.ReactNode }) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={id} className={labelClass}>{label}</label>
      {children}
      {error ? <p className="text-[11px] text-red-300">{error}</p> : null}
    </div>
  );
}

export default function NewStudentPage() {
  const router = useRouter();
  const viewModel = useMemo(() => new StudentFormViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => { void viewModel.init(); }, [viewModel]);

  function bind(name: keyof StudentInput, props?: { type?: string; placeholder?: string }) {
    return (
      <input
        id={name}
        type={props?.type ?? "text"}
        className={inputClass}
        value={state.form[name] ?? ""}
        placeholder={props?.placeholder}
        onChange={(e) => viewModel.setField(name, e.target.value)}
      />
    );
  }

  return (
    <PageShell>
      <div className="space-y-1">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Gestão de alunos</p>
        <h1 className="text-2xl font-semibold text-white">Novo aluno</h1>
        <p className="text-sm text-slate-400">Preencha os dados cadastrais. Campos sensíveis são protegidos por permissão.</p>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      {state.loading ? (
        <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando…</div>
      ) : (
        <form
          className="space-y-6 rounded-2xl border border-white/10 bg-surface-900/60 p-5"
          onSubmit={async (e) => {
            e.preventDefault();
            const ok = await viewModel.save();
            if (ok) router.push("/academy/students");
          }}
          noValidate
        >
          <div className="grid gap-4 md:grid-cols-2">
            <Field id="name" label="Nome completo" error={state.errors.name}>{bind("name", { placeholder: "Maria Silva" })}</Field>
            <Field id="socialName" label="Nome social">{bind("socialName")}</Field>
            <Field id="cpf" label="CPF" error={state.errors.cpf}>{bind("cpf", { placeholder: "Somente números" })}</Field>
            <Field id="birthDate" label="Data de nascimento" error={state.errors.birthDate}>{bind("birthDate", { type: "date" })}</Field>
            <Field id="email" label="E-mail" error={state.errors.email}>{bind("email", { type: "email" })}</Field>
            <Field id="phone" label="Telefone">{bind("phone")}</Field>
            <Field id="gender" label="Gênero">
              <select id="gender" className={inputClass} value={state.form.gender} onChange={(e) => viewModel.setField("gender", e.target.value)}>
                <option value="">Não informado</option>
                <option value="MALE">Masculino</option>
                <option value="FEMALE">Feminino</option>
                <option value="OTHER">Outro</option>
                <option value="NOT_INFORMED">Prefiro não informar</option>
              </select>
            </Field>
            <Field id="status" label="Status inicial">
              <select id="status" className={inputClass} value={state.form.status} onChange={(e) => viewModel.setField("status", e.target.value)}>
                {STUDENT_STATUSES.map((s) => <option key={s} value={s}>{STUDENT_STATUS_LABELS[s]}</option>)}
              </select>
            </Field>
            <Field id="primaryBranchId" label="Unidade principal">
              <select id="primaryBranchId" className={inputClass} value={state.form.primaryBranchId} onChange={(e) => viewModel.setField("primaryBranchId", e.target.value)}>
                <option value="">Nenhuma</option>
                {state.branches.map((b) => <option key={b.id} value={b.id}>{b.name}</option>)}
              </select>
            </Field>
            <Field id="photoUrl" label="URL da foto">{bind("photoUrl", { placeholder: "https://..." })}</Field>
          </div>
          <Field id="notes" label="Observações">
            <textarea id="notes" className={`${inputClass} min-h-[90px]`} value={state.form.notes} onChange={(e) => viewModel.setField("notes", e.target.value)} />
          </Field>

          <div className="flex flex-wrap gap-3">
            <button type="submit" disabled={state.saving} className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-50">
              {state.saving ? "Salvando…" : "Cadastrar aluno"}
            </button>
            <Link href="/academy/students" className="rounded-xl border border-white/10 px-4 py-2 text-sm text-slate-300 transition hover:text-white">
              Cancelar
            </Link>
          </div>
        </form>
      )}
    </PageShell>
  );
}
