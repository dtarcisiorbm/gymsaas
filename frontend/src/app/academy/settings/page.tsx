"use client";

import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { LoadingScreen, Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { authRepository } from "@/features/auth/data/AuthRepository";
import type { TenantInput } from "@/features/tenants/model/types";
import { AcademySettingsViewModel } from "@/features/tenants/viewmodel/AcademySettingsViewModel";

const inputClass =
  "w-full rounded-xl border border-white/10 bg-surface-950/70 px-3 py-2 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20";
const labelClass = "text-xs font-medium text-slate-300";

function AcademySettingsContent() {
  const viewModel = useMemo(() => new AcademySettingsViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load();
    void authRepository.me().catch(() => undefined);
  }, [viewModel]);

  if (state.loading) {
    return <LoadingScreen message="Carregando dados da academia…" />;
  }

  function field(name: keyof TenantInput, label: string, options?: { type?: string; placeholder?: string }) {
    return (
      <div className="space-y-1.5">
        <label htmlFor={name} className={labelClass}>{label}</label>
        <input
          id={name}
          type={options?.type ?? "text"}
          className={inputClass}
          placeholder={options?.placeholder}
          value={state.form[name]}
          onChange={(event) => viewModel.setField(name, event.target.value)}
        />
        {state.formErrors[name] ? <p className="text-[11px] text-red-300">{state.formErrors[name]}</p> : null}
      </div>
    );
  }

  return (
    <PageShell>
      <div className="space-y-1">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Administração da academia</p>
        <h1 className="text-2xl font-semibold text-white">Dados da academia</h1>
        <p className="text-sm text-slate-400">
          Atualize o cadastro da academia ativa. O status e o ciclo de vida são controlados pela plataforma.
        </p>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      <form
        className="max-w-3xl space-y-4 rounded-2xl border border-white/10 bg-surface-900/60 p-6"
        onSubmit={(event) => {
          event.preventDefault();
          void viewModel.save();
        }}
        noValidate
      >
        <div className="grid gap-4 sm:grid-cols-2">
          {field("name", "Nome fantasia", { placeholder: "Academia Central" })}
          {field("legalName", "Razão social", { placeholder: "Academia Central Ltda" })}
          {field("document", "CPF / CNPJ", { placeholder: "12.345.678/0001-99" })}
          {field("email", "E-mail", { type: "email", placeholder: "contato@academia.com" })}
          {field("phone", "Telefone", { placeholder: "(11) 4000-0000" })}
        </div>

        <div className="flex justify-end">
          <button
            type="submit"
            disabled={state.saving}
            className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:opacity-60"
          >
            {state.saving ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
            Salvar alterações
          </button>
        </div>
      </form>
    </PageShell>
  );
}

export default function AcademySettingsPage() {
  return (
    <RequireAuth requireTenant>
      <AcademySettingsContent />
    </RequireAuth>
  );
}
