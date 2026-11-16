export function Spinner({ className = "" }: { className?: string }) {
  return (
    <span
      className={`inline-block h-5 w-5 animate-spin rounded-full border-2 border-accent-500/30 border-t-accent-400 ${className}`}
      role="status"
      aria-label="Carregando"
    />
  );
}

export function LoadingScreen({ message = "Carregando…" }: { message?: string }) {
  return (
    <main className="flex min-h-screen items-center justify-center">
      <div className="flex flex-col items-center gap-3 text-slate-400">
        <Spinner className="h-8 w-8" />
        <p className="text-sm">{message}</p>
      </div>
    </main>
  );
}
