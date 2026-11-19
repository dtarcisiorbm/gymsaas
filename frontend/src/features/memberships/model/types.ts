import type { BillingPeriod, DurationType, FreezePolicy } from "@/features/plans/model/types";

export type MembershipStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "FROZEN" | "EXPIRED" | "CANCELLED";
export type FreezeStatus = "ACTIVE" | "COMPLETED" | "CANCELLED";

export const MEMBERSHIP_STATUSES: MembershipStatus[] = [
  "PENDING",
  "ACTIVE",
  "FROZEN",
  "SUSPENDED",
  "EXPIRED",
  "CANCELLED",
];

export const MEMBERSHIP_STATUS_LABELS: Record<MembershipStatus, string> = {
  PENDING: "Aguardando início",
  ACTIVE: "Ativa",
  FROZEN: "Congelada",
  SUSPENDED: "Suspensa",
  EXPIRED: "Expirada",
  CANCELLED: "Cancelada",
};

export const MEMBERSHIP_STATUS_STYLES: Record<MembershipStatus, string> = {
  PENDING: "border-sky-500/30 bg-sky-500/10 text-sky-300",
  ACTIVE: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
  FROZEN: "border-cyan-500/30 bg-cyan-500/10 text-cyan-300",
  SUSPENDED: "border-amber-500/30 bg-amber-500/10 text-amber-300",
  EXPIRED: "border-white/10 bg-white/5 text-slate-400",
  CANCELLED: "border-red-500/30 bg-red-500/10 text-red-300",
};

export const FREEZE_STATUS_LABELS: Record<FreezeStatus, string> = {
  ACTIVE: "Em andamento",
  COMPLETED: "Concluído",
  CANCELLED: "Cancelado",
};

export const EVENT_LABELS: Record<string, string> = {
  CREATED: "Matrícula criada",
  ACTIVATED: "Ativada",
  UPDATED: "Dados atualizados",
  RENEWED: "Renovada",
  CANCELLED: "Cancelada",
  FROZEN: "Congelada",
  UNFROZEN: "Descongelada",
  SUSPENDED: "Suspensa",
  REACTIVATED: "Reativada",
  EXPIRED: "Expirada",
  CONTRACT_GENERATED: "Contrato gerado",
};

export interface Conditions {
  durationType: DurationType;
  durationValue: number;
  amount: string;
  setupFee: string;
  currency: string;
  billingPeriod: BillingPeriod;
  billingInterval: number;
  discountAmount: string;
  discountReason: string | null;
  finalAmount: string;
  freezePolicy: FreezePolicy | null;
  snapshot: Record<string, unknown> | null;
}

export interface MembershipQuote {
  studentId: string;
  studentName: string;
  planId: string;
  planName: string;
  planPriceId: string;
  branchId: string;
  branchName: string;
  startDate: string;
  endDate: string;
  initialStatus: MembershipStatus;
  conditions: Conditions;
}

export interface MembershipFreeze {
  id: string;
  startDate: string;
  plannedEndDate: string;
  actualEndDate: string | null;
  plannedDays: number;
  effectiveDays: number | null;
  status: FreezeStatus;
  reason: string;
  createdAt: string;
  endedAt: string | null;
}

export interface MembershipSummary {
  id: string;
  registrationNumber: string;
  status: MembershipStatus;
  studentId: string;
  studentName: string;
  studentRegistration: string;
  planId: string;
  planName: string;
  branchId: string;
  branchName: string;
  startDate: string;
  endDate: string;
  finalAmount: string;
  currency: string;
  billingPeriod: BillingPeriod;
  createdAt: string;
}

export interface MembershipDetail {
  id: string;
  registrationNumber: string;
  status: MembershipStatus;
  studentId: string;
  studentName: string;
  studentRegistration: string;
  planId: string;
  planName: string;
  planPriceId: string;
  branchId: string;
  branchName: string;
  startDate: string;
  endDate: string;
  originalEndDate: string;
  anchorDate: string;
  termSequence: number;
  conditions: Conditions;
  renewedFromId: string | null;
  renewedById: string | null;
  notes: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  freezesUsed: number;
  frozenDaysUsed: number;
  freezes: MembershipFreeze[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface MembershipHistoryEntry {
  id: string;
  type: string;
  fromStatus: MembershipStatus | null;
  toStatus: MembershipStatus | null;
  reason: string | null;
  metadata: Record<string, unknown> | null;
  changedBy: string | null;
  changedAt: string;
}

export interface MembershipListParams {
  search?: string;
  studentId?: string;
  planId?: string;
  branchId?: string;
  status?: string;
  endingFrom?: string;
  endingTo?: string;
  page: number;
  size: number;
  sort?: string;
  direction?: string;
}

export interface MembershipInput {
  studentId: string;
  planId: string;
  branchId: string;
  startDate: string;
  discountAmount: string;
  discountReason: string;
  notes: string;
}

export interface RenewInput {
  startDate: string;
  branchId: string;
  discountAmount: string;
  discountReason: string;
  notes: string;
}

export const EMPTY_MEMBERSHIP_FORM: MembershipInput = {
  studentId: "",
  planId: "",
  branchId: "",
  startDate: "",
  discountAmount: "",
  discountReason: "",
  notes: "",
};

export const EMPTY_RENEW_FORM: RenewInput = {
  startDate: "",
  branchId: "",
  discountAmount: "",
  discountReason: "",
  notes: "",
};

/**
 * Mirrors MembershipStatus transitions and RenewMembershipUseCase so the UI
 * only offers valid actions; the backend still enforces every rule.
 */
export function availableActions(membership: Pick<MembershipDetail, "status" | "renewedById">) {
  const { status } = membership;
  return {
    freeze: status === "ACTIVE" && !membership.renewedById,
    unfreeze: status === "FROZEN",
    suspend: status === "ACTIVE",
    reactivate: status === "SUSPENDED",
    cancel: status === "PENDING" || status === "ACTIVE" || status === "FROZEN" || status === "SUSPENDED",
    renew: (status === "ACTIVE" || status === "EXPIRED") && !membership.renewedById,
    edit: status !== "CANCELLED" && status !== "EXPIRED",
  };
}
