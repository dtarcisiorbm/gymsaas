"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useSession } from "@/core/session/useSession";

interface NavItem {
  href: string;
  label: string;
}

export function AppNav() {
  const pathname = usePathname();
  const session = useSession();
  const permissions = new Set(session.permissions);
  const hasPlatform = session.platformAdmin || permissions.has("tenants:read");

  const items: NavItem[] = [{ href: "/dashboard", label: "Painel" }];
  if (hasPlatform) {
    items.push({ href: "/admin/tenants", label: "Academias" });
  }
  if (session.activeTenantId && (permissions.has("branches:read") || session.platformAdmin)) {
    items.push({ href: "/academy/branches", label: "Unidades" });
  }
  if (
    session.activeTenantId &&
    (permissions.has("students:read") || permissions.has("student:read") || session.platformAdmin)
  ) {
    items.push({ href: "/academy/students", label: "Alunos" });
  }
  if (session.activeTenantId && (permissions.has("users:read") || session.platformAdmin)) {
    items.push({ href: "/academy/users", label: "Usuários" });
  }
  if (
    session.activeTenantId &&
    (permissions.has("tenant:update") || permissions.has("tenants:update") || session.platformAdmin)
  ) {
    items.push({ href: "/academy/settings", label: "Academia" });
  }

  return (
    <nav className="border-b border-white/5 bg-surface-950/40">
      <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-1 px-6 py-2">
        {items.map((item) => {
          const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
          return (
            <Link
              key={item.href}
              href={item.href}
              className={`rounded-lg px-3 py-1.5 text-xs font-medium transition ${
                active
                  ? "bg-accent-500/15 text-accent-400"
                  : "text-slate-400 hover:bg-white/5 hover:text-slate-100"
              }`}
            >
              {item.label}
            </Link>
          );
        })}
        {session.platformAdmin ? (
          <span className="ml-auto rounded-full border border-sky-500/30 bg-sky-500/10 px-2.5 py-1 text-[11px] font-medium text-sky-300">
            Administrador da plataforma
          </span>
        ) : null}
      </div>
    </nav>
  );
}
