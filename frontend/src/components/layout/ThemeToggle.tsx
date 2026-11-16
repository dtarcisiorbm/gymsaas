"use client";

import { useTheme } from "@/core/theme/ThemeProvider";

export function ThemeToggle() {
  const { theme, toggle } = useTheme();
  const isDark = theme === "dark";

  return (
    <button
      type="button"
      onClick={toggle}
      className="rounded-lg border border-white/10 px-3 py-1.5 text-xs text-slate-300 transition hover:border-accent-500/40 hover:text-white"
      aria-label={isDark ? "Ativar tema claro" : "Ativar tema escuro"}
      title={isDark ? "Tema claro" : "Tema escuro"}
    >
      {isDark ? "☀️ Claro" : "🌙 Escuro"}
    </button>
  );
}
