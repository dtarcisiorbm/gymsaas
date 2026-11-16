"use client";

import { sessionManager } from "@/core/session/SessionManager";
import { useSession } from "@/core/session/useSession";
import { ThemeToggle } from "@/components/layout/ThemeToggle";

interface AppHeaderProps {
  onLogout: () => void | Promise<void>;
  onSwitchTenant?: () => void;
}

export function AppHeader({ onLogout, onSwitchTenant }: AppHeaderProps) {
  const session = useSession();
  const tenantName =
    sessionManager.activeTenant?.name ?? (session.platformAdmin ? "Plataforma SaaS" : "Academia");

  return (
    <header className="border-b border-white/5 bg-surface-900/60 backdrop-blur">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-6 py-4">
        <div className="flex items-center gap-3">
          <div className="flex h-9 w-9 items-center justify-center rounded-xl bg-accent-500 font-black text-surface-950">
            G
          </div>
          <div>
            <p className="text-sm font-semibold tracking-wide text-white">GYMSAAS</p>
            <p className="text-xs text-slate-400">{tenantName}</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {onSwitchTenant ? (
            <button
              type="button"
              onClick={onSwitchTenant}
              className="rounded-lg border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
            >
              Trocar academia
            </button>
          ) : null}
          <div className="hidden text-right sm:block">
            <p className="text-sm font-medium text-white">{session.user?.fullName}</p>
            <p className="text-xs text-slate-400">{session.user?.email}</p>
          </div>
          <ThemeToggle />
          <button
            type="button"
            onClick={() => void onLogout()}
            className="rounded-lg bg-white/5 px-3 py-1.5 text-xs font-medium text-slate-300 transition hover:bg-white/10 hover:text-white"
          >
            Sair
          </button>
        </div>
      </div>
    </header>
  );
}
