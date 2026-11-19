export type ContractStatus = "GENERATED" | "ACCEPTED" | "SUPERSEDED";

export const CONTRACT_STATUS_LABELS: Record<ContractStatus, string> = {
  GENERATED: "Aguardando aceite",
  ACCEPTED: "Aceito",
  SUPERSEDED: "Substituído",
};

export const CONTRACT_STATUS_STYLES: Record<ContractStatus, string> = {
  GENERATED: "border-amber-500/30 bg-amber-500/10 text-amber-300",
  ACCEPTED: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
  SUPERSEDED: "border-white/10 bg-white/5 text-slate-400",
};

export interface ContractAcceptance {
  id: string;
  acceptedAt: string;
  channel: string;
  acceptorName: string;
  acceptorDocumentMasked: string | null;
  contentSha256: string;
  declaration: string;
  ipAddress: string | null;
  userAgent: string | null;
  registeredBy: string | null;
}

export interface ContractSummary {
  id: string;
  membershipId: string;
  contractVersion: number;
  templateVersion: string;
  status: ContractStatus;
  contentSha256: string;
  documentSha256: string;
  documentSizeBytes: number;
  documentContentType: string;
  generatedBy: string | null;
  generatedAt: string;
  statusChangedAt: string;
  acceptance: ContractAcceptance | null;
}

export interface ContractView extends ContractSummary {
  content: string;
  legalNotice: string;
}
