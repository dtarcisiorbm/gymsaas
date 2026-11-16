import Link from "next/link";

export default function NotFound() {
  return (
    <main className="flex min-h-screen items-center justify-center px-6">
      <div className="w-full max-w-md space-y-4 rounded-2xl border border-white/10 bg-surface-900/70 p-8 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">404</p>
        <h1 className="text-xl font-semibold text-white">Página não encontrada</h1>
        <p className="text-sm text-slate-400">O endereço informado não existe.</p>
        <Link
          href="/dashboard"
          className="inline-block rounded-xl bg-accent-500 px-4 py-2 text-sm font-semibold text-surface-950 transition hover:bg-accent-400"
        >
          Ir para o painel
        </Link>
      </div>
    </main>
  );
}
