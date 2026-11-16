"use client";

import { useEffect, useMemo } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { LoadingScreen, Spinner } from "@/components/ui/Loading";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { useSession } from "@/core/session/useSession";
import { authRepository } from "@/features/auth/data/AuthRepository";
import { SelectTenantViewModel } from "@/features/auth/viewmodel/SelectTenantViewModel";

export default function SelectTenantPage() {
  const router = useRouter();
  const session = useSession();
  const viewModel = useMemo(() => new SelectTenantViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    if (session.status === "authenticated") {
      void viewModel.load();
    }
  }, [session.status, viewModel]);

  useEffect(() => {
    if (session.status === "anonymous") {
      router.replace("/login");
    } else if (session.status === "authenticated" && session.activeTenantId) {
      router.replace("/dashboard");
    }
  }, [session.status, session.activeTenantId, router]);

  if (session.status !== "authenticated") {
    return <LoadingScreen message="Verificando sua sessão…" />;
  }

  async function select(tenantId: string) {
    const selected = await viewModel.select(tenantId);
    if (selected) {
      router.replace("/dashboard");
    }
  }

  return (
    <main className="mx-auto flex min-h-screen w-full max-w-4xl flex-col justify-center px-6 py-12">
      <div className="mb-8 space-y-2">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">
          Multi-tenant
        </p>
        <h1 className="text-3xl font-semibold text-white">Selecione a academia</h1>
        <p className="text-sm text-slate-400">
          Cada academia possui dados e permissões isolados. Sua escolha fica vinculada à sessão.
        </p>
      </div>

      {state.error ? (
        <p
          role="alert"
          className="mb-4 rounded-xl border border-red-500/30 bg-red-500/10 px-4 py-3 text-sm text-red-200"
        >
          {state.error}
        </p>
      ) : null}

      {state.loading ? (
        <div className="flex items-center gap-3 rounded-2xl border border-white/10 bg-surface-900/60 p-6 text-sm text-slate-400">
          <Spinner />
          Carregando academias…
        </div>
      ) : (
        <ul className="space-y-3">
          {state.tenants.map((tenant) => (
            <li
              key={tenant.id}
              className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-white/10 bg-surface-900/60 p-5 transition hover:border-accent-500/30"
            >
              <div className="space-y-1">
                <p className="text-base font-semibold text-white">{tenant.name}</p>
                <div className="flex flex-wrap gap-1.5">
                  {tenant.roles.length > 0 ? (
                    tenant.roles.map((role) => (
                      <span
                        key={role}
                        className="rounded-full border border-accent-500/20 bg-accent-500/10 px-2 py-0.5 text-[11px] font-medium text-accent-400"
                      >
                        {role}
                      </span>
                    ))
                  ) : (
                    <span className="text-xs text-slate-500">Sem papéis atribuídos</span>
                  )}
                </div>
              </div>
              <button
                type="button"
                onClick={() => void select(tenant.id)}
                disabled={state.selectingId !== null}
                className="flex items-center gap-2 rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400 disabled:cursor-not-allowed disabled:opacity-60"
              >
                {state.selectingId === tenant.id ? <Spinner className="h-4 w-4 border-surface-950/30 border-t-surface-950" /> : null}
                Entrar nesta academia
              </button>
            </li>
          ))}
          {state.tenants.length === 0 ? (
            <li className="rounded-2xl border border-white/10 bg-surface-900/60 p-6 text-sm text-slate-400">
              {session.platformAdmin ? (
                <span className="flex flex-wrap items-center gap-3">
                  Seu usuário é administrador da plataforma e não possui vínculo com academias.
                  <Link href="/admin/tenants" className="font-semibold text-accent-400 hover:text-accent-300">
                    Gerenciar academias →
                  </Link>
                </span>
              ) : (
                "Nenhuma academia vinculada ao seu usuário."
              )}
            </li>
          ) : null}
        </ul>
      )}

      <button
        type="button"
        onClick={async () => {
          await authRepository.logout();
          router.replace("/login");
        }}
        className="mt-8 self-start text-xs text-slate-400 underline-offset-4 transition hover:text-slate-200 hover:underline"
      >
        Sair e usar outra conta
      </button>
    </main>
  );
}
