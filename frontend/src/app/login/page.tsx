"use client";

import { type FormEvent, useEffect, useMemo } from "react";
import { useRouter } from "next/navigation";
import { Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { useSession } from "@/core/session/useSession";
import { LoginViewModel } from "@/features/auth/viewmodel/LoginViewModel";

export default function LoginPage() {
  const router = useRouter();
  const session = useSession();
  const viewModel = useMemo(() => new LoginViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    if (session.status === "authenticated") {
      if (session.platformAdmin && !session.activeTenantId) {
        router.replace("/admin/tenants");
      } else {
        router.replace(session.activeTenantId ? "/dashboard" : "/select-tenant");
      }
    }
  }, [session.status, session.activeTenantId, session.platformAdmin, router]);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const target = await viewModel.submit();
    if (target) {
      router.replace(`/${target}`);
    }
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-6xl items-center justify-center px-6 py-12">
      <div className="grid w-full overflow-hidden rounded-3xl border border-white/10 bg-surface-900/70 shadow-2xl backdrop-blur lg:grid-cols-[1.05fr_1fr]">
        <section className="relative hidden flex-col justify-between gap-10 border-r border-white/5 bg-gradient-to-br from-accent-500/15 via-surface-900 to-surface-950 p-10 lg:flex">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-accent-500 text-lg font-black text-surface-950">
              G
            </div>
            <span className="text-sm font-semibold tracking-[0.2em] text-white">GYMSAAS</span>
          </div>

          <div className="space-y-5">
            <h1 className="text-4xl font-bold leading-tight text-white">
              Uma plataforma.
              <br />
              <span className="text-accent-400">Todas as suas academias.</span>
            </h1>
            <p className="max-w-sm text-sm leading-relaxed text-slate-300">
              Gestão multi-tenant com isolamento de dados, controle de acesso por
              academia e sessões protegidas por refresh tokens rotativos.
            </p>
            <ul className="space-y-3 text-sm text-slate-300">
              {[
                "Isolamento por academia garantido no banco (RLS)",
                "RBAC com permissões granulares por papel",
                "Auditoria de acessos e alterações sensíveis",
              ].map((item) => (
                <li key={item} className="flex items-start gap-3">
                  <span className="mt-1 inline-block h-1.5 w-1.5 rounded-full bg-accent-400" />
                  {item}
                </li>
              ))}
            </ul>
          </div>

          <p className="text-xs text-slate-500">Sessão protegida · LGPD · OWASP ASVS</p>
        </section>

        <section className="p-8 sm:p-10">
          <div className="mb-8 space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">
              Acesso
            </p>
            <h2 className="text-2xl font-semibold text-white">Entrar na plataforma</h2>
            <p className="text-sm text-slate-400">
              Use suas credenciais para acessar a gestão das suas academias.
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5" noValidate>
            <div className="space-y-2">
              <label htmlFor="email" className="text-sm font-medium text-slate-200">
                E-mail
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                value={state.email}
                onChange={(event) => viewModel.setEmail(event.target.value)}
                placeholder="voce@academia.com"
                className="w-full rounded-xl border border-white/10 bg-surface-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20"
              />
            </div>

            <div className="space-y-2">
              <label htmlFor="password" className="text-sm font-medium text-slate-200">
                Senha
              </label>
              <input
                id="password"
                type="password"
                autoComplete="current-password"
                value={state.password}
                onChange={(event) => viewModel.setPassword(event.target.value)}
                placeholder="••••••••••"
                className="w-full rounded-xl border border-white/10 bg-surface-950/70 px-4 py-3 text-sm text-white outline-none transition placeholder:text-slate-500 focus:border-accent-500/60 focus:ring-2 focus:ring-accent-500/20"
              />
            </div>

            {state.error ? (
              <p
                role="alert"
                className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200"
              >
                {state.error}
              </p>
            ) : null}

            <button
              type="submit"
              disabled={state.submitting || session.status === "booting"}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-accent-500 px-4 py-3 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:cursor-not-allowed disabled:opacity-60"
            >
              {state.submitting ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
              {state.submitting ? "Entrando…" : "Entrar"}
            </button>
          </form>

          <div className="mt-8 rounded-xl border border-white/5 bg-white/[0.03] p-4">
            <p className="text-xs font-semibold uppercase tracking-wide text-slate-400">
              Ambiente de demonstração
            </p>
            <div className="mt-2 space-y-1 text-xs text-slate-400">
              <p>
                <span className="text-slate-300">Plataforma:</span> superadmin@gymsaas.local · SuperAdmin@123
              </p>
              <p>
                <span className="text-slate-300">Admin:</span> admin@gymsaas.local · Admin@123
              </p>
              <p>
                <span className="text-slate-300">Recepção:</span> reception@gymsaas.local · Reception@123
              </p>
            </div>
          </div>
        </section>
      </div>
    </main>
  );
}
