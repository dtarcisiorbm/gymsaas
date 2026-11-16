"use client";

import { useEffect, useMemo } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { RequireAuth } from "@/components/auth/RequireAuth";
import { PageShell } from "@/components/layout/PageShell";
import { Alert } from "@/components/ui/Alert";
import { LoadingScreen } from "@/components/ui/Loading";
import { Pagination } from "@/components/ui/Pagination";
import { formatDateTime, formatDocument } from "@/core/format";
import { useViewModel } from "@/core/mvvm/useViewModel";
import { authRepository } from "@/features/auth/data/AuthRepository";
import { TenantDetailViewModel } from "@/features/tenants/viewmodel/TenantDetailViewModel";

function TenantDetailContent({ tenantId }: { tenantId: string }) {
  const viewModel = useMemo(() => new TenantDetailViewModel(), []);
  const state = useViewModel(viewModel);

  useEffect(() => {
    void viewModel.load(tenantId);
    void authRepository.me().catch(() => undefined);
  }, [viewModel, tenantId]);

  if (state.loading) {
    return <LoadingScreen message="Carregando academia…" />;
  }

  return (
    <PageShell>
      <div className="space-y-1">
        <Link href="/admin/tenants" className="text-xs text-accent-400 hover:text-accent-300">
          ← Voltar para academias
        </Link>
        <h1 className="text-2xl font-semibold text-white">{state.tenant?.name ?? "Academia"}</h1>
        <p className="text-sm text-slate-400">Dados cadastrais e unidades vinculadas.</p>
      </div>

      {state.error ? <Alert>{state.error}</Alert> : null}

      {state.tenant ? (
        <section className="rounded-2xl border border-white/10 bg-surface-900/60 p-5">
          <div className="grid gap-4 md:grid-cols-2">
            {[
              ["Razão social", state.tenant.legalName],
              ["CPF / CNPJ", formatDocument(state.tenant.document)],
              ["E-mail", state.tenant.email],
              ["Telefone", state.tenant.phone ?? "—"],
              ["Status", state.tenant.status],
              ["Atualizada em", formatDateTime(state.tenant.updatedAt)],
            ].map(([label, value]) => (
              <div key={label} className="flex justify-between gap-4 border-b border-white/5 pb-2 text-sm">
                <span className="text-slate-400">{label}</span>
                <span className="text-right text-slate-200">{value}</span>
              </div>
            ))}
          </div>
        </section>
      ) : null}

      <section className="overflow-hidden rounded-2xl border border-white/10 bg-surface-900/60">
        <div className="flex items-center justify-between border-b border-white/5 px-5 py-4">
          <h2 className="text-sm font-semibold text-white">Unidades</h2>
          <span className="text-xs text-slate-500">{state.branchesTotal} unidade(s)</span>
        </div>
        {state.branches.length === 0 ? (
          <p className="px-5 py-8 text-sm text-slate-400">Esta academia ainda não possui unidades.</p>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-slate-500">
                <tr>
                  <th className="px-5 py-3 font-medium">Unidade</th>
                  <th className="px-5 py-3 font-medium">Cidade / UF</th>
                  <th className="px-5 py-3 font-medium">Telefone</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-white/5">
                {state.branches.map((branch) => (
                  <tr key={branch.id} className="text-slate-300">
                    <td className="px-5 py-3">
                      <p className="font-medium text-white">{branch.name}</p>
                      <p className="font-mono text-[11px] text-slate-500">{branch.code}</p>
                    </td>
                    <td className="px-5 py-3 text-slate-400">
                      {branch.city ? `${branch.city}${branch.state ? ` / ${branch.state}` : ""}` : "—"}
                    </td>
                    <td className="px-5 py-3 text-slate-400">{branch.phone ?? "—"}</td>
                    <td className="px-5 py-3 text-xs">{branch.status}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        <Pagination
          page={state.branchesPage}
          size={state.branchesSize}
          totalElements={state.branchesTotal}
          onPrevious={() => void viewModel.changeBranchesPage(tenantId, -1)}
          onNext={() => void viewModel.changeBranchesPage(tenantId, 1)}
        />
      </section>
    </PageShell>
  );
}

export default function TenantDetailPage() {
  const params = useParams<{ tenantId: string }>();
  return (
    <RequireAuth requireTenant={false} requirePlatformAdmin>
      <TenantDetailContent tenantId={params.tenantId} />
    </RequireAuth>
  );
}
