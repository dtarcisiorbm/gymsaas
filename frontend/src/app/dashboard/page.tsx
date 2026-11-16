"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { LoadingScreen, Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { useSession } from "@/core/session/useSession";
import { formatDocument } from "@/core/format";
import { DashboardViewModel } from "@/features/dashboard/viewmodel/DashboardViewModel";

function useCountdown(expiresAt: number | null): string {
  const [now, setNow] = useState<number | null>(null);

  useEffect(() => {
    setNow(Date.now());
    const interval = window.setInterval(() => setNow(Date.now()), 1_000);
    return () => window.clearInterval(interval);
  }, [expiresAt]);

  if (!expiresAt || now === null) {
    return "—";
  }
  const seconds = Math.max(0, Math.floor((expiresAt - now) / 1000));
  const minutes = Math.floor(seconds / 60);
  return `${minutes}m ${String(seconds % 60).padStart(2, "0")}s`;
}

function DashboardContent() {
  const session = useSession();
  const viewModel = useMemo(() => new DashboardViewModel(), []);
  const state = useViewModel(viewModel);
  const countdown = useCountdown(session.expiresAt);
  const [renewing, setRenewing] = useState(false);

  useEffect(() => {
    void viewModel.load();
  }, [viewModel]);

  async function handleRenew() {
    setRenewing(true);
    await viewModel.renew();
    setRenewing(false);
  }

  if (state.loading) {
    return <LoadingScreen message="Carregando painel…" />;
  }

  return (
    <PageShell>
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div className="space-y-1">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">
            Painel
          </p>
          <h1 className="text-3xl font-semibold text-white">
            Olá, {state.me?.user.fullName?.split(" ")[0] ?? session.user?.fullName ?? "usuário"}
          </h1>
          <p className="text-sm text-slate-400">
            {session.activeTenantId ? (
              <>
                Sessão autenticada em{" "}
                <span className="text-slate-200">
                  {state.tenant?.name ?? session.tenants.find((t) => t.id === session.activeTenantId)?.name}
                </span>
              </>
            ) : (
              "Sessão de administração da plataforma, sem academia ativa."
            )}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-2 text-xs">
          {session.roles.map((role) => (
            <span
              key={role}
              className="rounded-full border border-accent-500/20 bg-accent-500/10 px-3 py-1 font-medium text-accent-400"
            >
              {role}
            </span>
          ))}
        </div>
      </div>

        {state.error ? (
          <p role="alert" className="rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200">
            {state.error}
          </p>
        ) : null}

        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5">
            <h2 className="text-sm font-semibold text-white">Academia ativa</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Nome</dt>
                <dd className="text-right text-slate-200">{state.tenant?.name ?? "—"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Razão social</dt>
                <dd className="text-right text-slate-200">{state.tenant?.legalName ?? "—"}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">CNPJ / CPF</dt>
                <dd className="text-right text-slate-200">
                  {state.tenant ? formatDocument(state.tenant.document ?? state.tenant.cnpj) : "—"}
                </dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Status</dt>
                <dd className="text-right">
                  <span className="rounded-full bg-emerald-500/10 px-2 py-0.5 text-xs font-medium text-emerald-300">
                    {state.tenant?.status ?? "—"}
                  </span>
                </dd>
              </div>
            </dl>
          </section>

          <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5">
            <h2 className="text-sm font-semibold text-white">Sessão</h2>
            <dl className="mt-4 space-y-2 text-sm">
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Usuário</dt>
                <dd className="truncate text-right text-slate-200">{state.me?.user.email ?? session.user?.email}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Access token expira em</dt>
                <dd className="text-right font-medium text-accent-400">{countdown}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Academias disponíveis</dt>
                <dd className="text-right text-slate-200">{session.tenants.length}</dd>
              </div>
              <div className="flex justify-between gap-4">
                <dt className="text-slate-400">Permissões</dt>
                <dd className="text-right text-slate-200">{state.me?.permissions.length ?? 0}</dd>
              </div>
            </dl>
            <button
              type="button"
              onClick={() => void handleRenew()}
              disabled={renewing}
              className="mt-4 flex w-full items-center justify-center gap-2 rounded-xl border border-accent-500/30 bg-accent-500/10 px-4 py-2 text-xs font-semibold text-accent-400 transition hover:bg-accent-500/20 disabled:opacity-60"
            >
              {renewing ? <Spinner className="h-4 w-4" /> : null}
              {renewing ? "Renovando…" : "Renovar sessão agora"}
            </button>
          </section>

          <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5 md:col-span-2 xl:col-span-1">
            <h2 className="text-sm font-semibold text-white">Permissões efetivas</h2>
            <div className="mt-3 flex flex-wrap gap-1.5">
              {(state.me?.permissions ?? []).map((permission) => (
                <span
                  key={permission}
                  className="rounded-md border border-white/10 bg-white/[0.04] px-2 py-1 font-mono text-[11px] text-slate-300"
                >
                  {permission}
                </span>
              ))}
              {state.me && state.me.permissions.length === 0 ? (
                <p className="text-xs text-slate-500">Nenhuma permissão atribuída.</p>
              ) : null}
            </div>
          </section>
        </div>

        {session.platformAdmin ? (
          <section className="rounded-2xl border border-sky-500/20 bg-sky-500/5 p-5">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-semibold text-white">Administração da plataforma</h2>
                <p className="mt-1 text-xs text-slate-400">
                  Cadastre academias, acompanhe o status e controle o acesso de cada tenant.
                </p>
              </div>
              <Link
                href="/admin/tenants"
                className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
              >
                Gerenciar academias
              </Link>
            </div>
          </section>
        ) : null}

        {state.users.length > 0 ? (
          <section className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
            <div className="flex items-center justify-between border-b border-white/5 px-5 py-4">
              <h2 className="text-sm font-semibold text-white">Usuários da academia</h2>
              <div className="flex items-center gap-3">
                <span className="text-xs text-slate-500">{state.userTotal} vínculo(s)</span>
                <Link href="/academy/users" className="text-xs text-accent-400 hover:text-accent-300">
                  Ver todos
                </Link>
              </div>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-sm">
                <thead className="text-xs uppercase tracking-wide text-slate-500">
                  <tr>
                    <th className="px-5 py-3 font-medium">Nome</th>
                    <th className="px-5 py-3 font-medium">E-mail</th>
                    <th className="px-5 py-3 font-medium">Papéis</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-white/5">
                  {state.users.map((user) => (
                    <tr key={user.id} className="text-slate-300">
                      <td className="px-5 py-3">{user.fullName}</td>
                      <td className="px-5 py-3 text-slate-400">{user.email}</td>
                      <td className="px-5 py-3">
                        <div className="flex flex-wrap gap-1">
                          {user.roles.map((role) => (
                            <span key={role} className="rounded bg-accent-500/10 px-1.5 py-0.5 text-[11px] text-accent-400">
                              {role}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td className="px-5 py-3 text-xs">{user.status}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        ) : null}

        {state.roles.length > 0 ? (
          <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5">
            <h2 className="text-sm font-semibold text-white">Papéis disponíveis</h2>
            <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
              {state.roles.map((role) => (
                <article key={role.id} className="rounded-xl border border-white/5 bg-white/[0.02] p-4">
                  <div className="flex items-center justify-between gap-2">
                    <p className="text-sm font-medium text-white">{role.name}</p>
                    {role.system ? (
                      <span className="rounded bg-white/5 px-1.5 py-0.5 text-[10px] uppercase tracking-wide text-slate-400">
                        Sistema
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-1 font-mono text-[11px] text-accent-400">{role.code}</p>
                  <p className="mt-2 text-xs text-slate-400">{role.description ?? "—"}</p>
                  <p className="mt-2 text-[11px] text-slate-500">{role.permissions.length} permissão(ões)</p>
                </article>
              ))}
            </div>
          </section>
        ) : null}
    </PageShell>
  );
}

export default function DashboardPage() {
  return (
    <RequireAuth>
      <DashboardContent />
    </RequireAuth>
  );
}
