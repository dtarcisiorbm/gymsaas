"use client";

import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useMemo } from "react";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { Field, InfoItem, StatusBadge } from "@/components/ui/Field";
import { inputClass, panelClass, primaryButtonClass, secondaryButtonClass } from "@/components/ui/formStyles";
import { Spinner } from "@/components/ui/Loading";
import { Modal } from "@/components/ui/Modal";
import { formatDateTime } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { usePermission } from "@/core/session/usePermission";
import { CONTRACT_STATUS_LABELS, CONTRACT_STATUS_STYLES } from "@/features/contracts/model/types";
import { ContractViewModel, integrityOk } from "@/features/contracts/viewmodel/ContractViewModel";

function ContractContent() {
  const params = useParams<{ id: string }>();
  const can = usePermission();
  const viewModel = useMemo(() => new ContractViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load(params.id);
  }, [viewModel, params.id]);

  const contract = state.contract;
  const ok = integrityOk(state);

  return (
    <PageShell>
      {state.error && !state.acceptOpen ? <Alert>{state.error}</Alert> : null}
      {state.success ? <Alert kind="success">{state.success}</Alert> : null}

      {state.loading ? (
        <div className="flex items-center gap-3 text-sm text-slate-400"><Spinner /> Carregando contrato…</div>
      ) : !contract ? (
        <Link href="/academy/memberships" className={secondaryButtonClass}>Voltar para matrículas</Link>
      ) : (
        <div className="space-y-6">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="space-y-1">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent-400">Contrato</p>
              <h1 className="text-2xl font-semibold text-white">Versão {contract.contractVersion}</h1>
              <p className="flex flex-wrap items-center gap-2 text-sm text-slate-400">
                <StatusBadge className={CONTRACT_STATUS_STYLES[contract.status]}>{CONTRACT_STATUS_LABELS[contract.status]}</StatusBadge>
                <span>gerado em {formatDateTime(contract.generatedAt)}</span>
                <span>· modelo {contract.templateVersion}</span>
              </p>
            </div>
            <div className="flex flex-wrap gap-2">
              <button type="button" disabled={state.downloading} onClick={() => void viewModel.download()} className={secondaryButtonClass}>
                {state.downloading ? "Baixando…" : "Baixar PDF"}
              </button>
              {contract.status === "GENERATED" && can("contracts:register_acceptance") ? (
                <button type="button" disabled={!ok} onClick={viewModel.openAccept} className={primaryButtonClass}>
                  Registrar aceite
                </button>
              ) : null}
              <Link href={`/academy/memberships/${contract.membershipId}`} className={secondaryButtonClass}>Voltar à matrícula</Link>
            </div>
          </div>

          {!ok ? (
            <Alert>O conteúdo recebido não confere com o hash registrado. Não registre aceite e recarregue a página.</Alert>
          ) : null}
          {contract.status === "SUPERSEDED" ? (
            <Alert kind="info">Esta versão foi substituída por uma mais recente e não pode mais ser aceita.</Alert>
          ) : null}

          {contract.acceptance ? (
            <section className={`${panelClass} space-y-3 p-5`}>
              <h2 className="text-base font-semibold text-white">Aceite registrado</h2>
              <dl className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                <InfoItem label="Aceito por">{contract.acceptance.acceptorName}</InfoItem>
                <InfoItem label="Documento">{contract.acceptance.acceptorDocumentMasked ?? "—"}</InfoItem>
                <InfoItem label="Data e hora">{formatDateTime(contract.acceptance.acceptedAt)}</InfoItem>
                <InfoItem label="Canal">{contract.acceptance.channel}</InfoItem>
                <InfoItem label="IP">{contract.acceptance.ipAddress ?? "—"}</InfoItem>
                <InfoItem label="Navegador">
                  <span className="break-all text-xs">{contract.acceptance.userAgent ?? "—"}</span>
                </InfoItem>
              </dl>
              <p className="text-xs text-slate-400">{contract.acceptance.declaration}</p>
            </section>
          ) : null}

          <section className={`${panelClass} p-5`}>
            <pre className="max-h-[70vh] overflow-y-auto whitespace-pre-wrap break-words font-sans text-sm leading-relaxed text-slate-200">
              {contract.content}
            </pre>
          </section>

          <section className={`${panelClass} space-y-2 p-5 text-xs text-slate-400`}>
            <p>{contract.legalNotice}</p>
            <p>
              SHA-256 do conteúdo: <span className="break-all font-mono text-slate-300">{contract.contentSha256}</span>
              {state.hashUnavailable ? (
                <span className="block text-amber-300">
                  Verificação local indisponível (conexão sem HTTPS). O hash informado pelo servidor será usado.
                </span>
              ) : ok ? (
                <span className="block text-emerald-300">Conteúdo exibido verificado neste navegador.</span>
              ) : null}
            </p>
            <p>
              SHA-256 do PDF: <span className="break-all font-mono text-slate-300">{contract.documentSha256}</span> ·{" "}
              {(contract.documentSizeBytes / 1024).toFixed(1)} KB
            </p>
          </section>
        </div>
      )}

      {contract && state.acceptOpen ? (
        <Modal
          title="Registrar aceite presencial"
          description="Aceite simples registrado pelo operador. Não é assinatura digital qualificada."
          onClose={viewModel.closeAccept}
          footer={
            <div className="flex justify-end gap-2">
              <button type="button" onClick={viewModel.closeAccept} className={secondaryButtonClass}>Voltar</button>
              <button type="button" disabled={state.saving} onClick={() => void viewModel.accept()} className={primaryButtonClass}>
                {state.saving ? "Registrando…" : "Registrar aceite"}
              </button>
            </div>
          }
        >
          {state.error ? <div className="mb-4"><Alert>{state.error}</Alert></div> : null}
          <div className="space-y-4">
            <Field id="acceptor-name" label="Nome de quem aceita *" error={state.fieldErrors.acceptorName}>
              <input id="acceptor-name" className={inputClass} value={state.acceptorName}
                onChange={(e) => viewModel.setAcceptorName(e.target.value)} />
            </Field>
            <label className="flex items-start gap-3 text-sm text-slate-300">
              <input
                type="checkbox"
                className="mt-0.5 h-4 w-4 accent-lime-500"
                checked={state.confirmed}
                onChange={(e) => viewModel.setConfirmed(e.target.checked)}
              />
              <span>
                Confirmo que o aluno leu o conteúdo exibido nesta tela (versão {contract.contractVersion}) e declarou aceitá-lo.
              </span>
            </label>
            {state.fieldErrors.confirmed ? <p className="text-xs text-red-300">{state.fieldErrors.confirmed}</p> : null}
            <p className="text-xs text-slate-500">
              Serão registrados: nome, CPF mascarado, IP, navegador, data/hora e o operador responsável.
            </p>
          </div>
        </Modal>
      ) : null}
    </PageShell>
  );
}

export default function ContractPage() {
  return (
    <RequireAuth>
      <ContractContent />
    </RequireAuth>
  );
}
