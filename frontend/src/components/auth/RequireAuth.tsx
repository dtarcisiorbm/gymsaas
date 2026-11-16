"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useSession } from "@/core/session/useSession";
import { LoadingScreen } from "@/components/ui/Loading";

interface RequireAuthProps {
  children: React.ReactNode;
  requireTenant?: boolean;
  requirePlatformAdmin?: boolean;
}

export function RequireAuth({ children, requireTenant = true, requirePlatformAdmin = false }: RequireAuthProps) {
  const session = useSession();
  const router = useRouter();

  useEffect(() => {
    if (session.status === "anonymous") {
      router.replace("/login");
    } else if (session.status === "authenticated" && requireTenant && !session.activeTenantId) {
      router.replace(session.platformAdmin ? "/admin/tenants" : "/select-tenant");
    } else if (session.status === "authenticated" && requirePlatformAdmin && !session.platformAdmin) {
      router.replace("/dashboard");
    }
  }, [session.status, session.activeTenantId, session.platformAdmin, requireTenant, requirePlatformAdmin, router]);

  if (session.status === "booting") {
    return <LoadingScreen message="Verificando sua sessão…" />;
  }
  if (session.status === "anonymous") {
    return <LoadingScreen message="Redirecionando para o login…" />;
  }
  if (requireTenant && !session.activeTenantId) {
    return <LoadingScreen message={session.platformAdmin ? "Abrindo administração…" : "Selecione uma academia…"} />;
  }
  if (requirePlatformAdmin && !session.platformAdmin) {
    return <LoadingScreen message="Verificando permissões…" />;
  }
  return <>{children}</>;
}
