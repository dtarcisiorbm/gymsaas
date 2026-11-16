"use client";

import { useEffect } from "react";

interface ModalProps {
  title: string;
  description?: string;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
  wide?: boolean;
}

export function Modal({ title, description, onClose, children, footer, wide = false }: ModalProps) {
  useEffect(() => {
    function handleKey(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKey);
    return () => window.removeEventListener("keydown", handleKey);
  }, [onClose]);

  return (
    <div className="fixed inset-0 z-50 flex items-start justify-center overflow-y-auto bg-surface-950/80 p-4 backdrop-blur-sm sm:items-center">
      <div
        role="dialog"
        aria-modal="true"
        aria-label={title}
        className={`my-auto w-full ${wide ? "max-w-3xl" : "max-w-xl"} rounded-2xl border border-white/10 bg-surface-900 shadow-2xl`}
      >
        <div className="flex items-start justify-between gap-4 border-b border-white/5 px-6 py-5">
          <div className="space-y-1">
            <h2 className="text-lg font-semibold text-white">{title}</h2>
            {description ? <p className="text-sm text-slate-400">{description}</p> : null}
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="Fechar"
            className="rounded-lg border border-white/10 px-2 py-1 text-xs text-slate-400 transition hover:border-white/20 hover:text-white"
          >
            ✕
          </button>
        </div>
        <div className="px-6 py-5">{children}</div>
        {footer ? <div className="border-t border-white/5 px-6 py-4">{footer}</div> : null}
      </div>
    </div>
  );
}
