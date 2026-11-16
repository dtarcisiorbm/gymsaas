interface PaginationProps {
  page: number;
  size: number;
  totalElements: number;
  onPrevious: () => void;
  onNext: () => void;
  onSizeChange?: (size: number) => void;
}

export function Pagination({ page, size, totalElements, onPrevious, onNext, onSizeChange }: PaginationProps) {
  const start = totalElements === 0 ? 0 : page * size + 1;
  const end = Math.min(totalElements, (page + 1) * size);
  const hasPrevious = page > 0;
  const hasNext = end < totalElements;

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 border-t border-white/5 px-5 py-3 text-xs text-slate-400">
      <span>
        {start}–{end} de {totalElements}
      </span>
      <div className="flex items-center gap-2">
        {onSizeChange ? (
          <select
            value={size}
            onChange={(event) => onSizeChange(Number(event.target.value))}
            aria-label="Itens por página"
            className="rounded-lg border border-white/10 bg-surface-950/70 px-2 py-1 text-xs text-slate-200 outline-none focus:border-accent-500/60"
          >
            {[10, 20, 50].map((option) => (
              <option key={option} value={option}>
                {option} / página
              </option>
            ))}
          </select>
        ) : null}
        <button
          type="button"
          onClick={onPrevious}
          disabled={!hasPrevious}
          className="rounded-lg border border-white/10 px-3 py-1.5 transition hover:border-accent-500/40 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Anterior
        </button>
        <span className="px-1 text-slate-300">Página {page + 1}</span>
        <button
          type="button"
          onClick={onNext}
          disabled={!hasNext}
          className="rounded-lg border border-white/10 px-3 py-1.5 transition hover:border-accent-500/40 hover:text-white disabled:cursor-not-allowed disabled:opacity-40"
        >
          Próxima
        </button>
      </div>
    </div>
  );
}
