"use client";

import { useEffect } from "react";

export default function GlobalError({
  error,
  reset,
}: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  useEffect(() => {
    console.error(error);
  }, [error]);

  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="w-full max-w-md space-y-4 rounded-2xl border border-red-500/20 bg-surface-900/70 p-8 text-center">
        <h1 className="text-xl font-semibold text-white">Algo deu errado</h1>
        <p className="text-sm text-slate-400">
          Não foi possível renderizar esta página. Tente novamente.
        </p>
        <button
          type="button"
          onClick={reset}
          className="rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Tentar novamente
        </button>
      </div>
    </main>
  );
}
