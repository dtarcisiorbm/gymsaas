"use client";

import { useRouter } from "next/navigation";
import { AppHeader } from "@/components/layout/AppHeader";
import { AppNav } from "@/components/layout/AppNav";
import { useSession } from "@/core/session/useSession";
import { authRepository } from "@/features/auth/data/AuthRepository";

export function PageShell({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const session = useSession();

  async function handleLogout() {
    await authRepository.logout();
    router.replace("/login");
  }

  return (
    <div className="min-h-screen">
      <AppHeader
        onLogout={handleLogout}
        onSwitchTenant={session.activeTenantId ? () => router.push("/select-tenant") : undefined}
      />
      <AppNav />
      <main className="mx-auto max-w-6xl space-y-6 px-6 py-8">{children}</main>
    </div>
  );
}
