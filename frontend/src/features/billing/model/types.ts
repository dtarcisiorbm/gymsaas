export type ChargeStatus = "OPEN" | "PAID" | "CANCELLED";
export type ChargeStatusFilter = ChargeStatus | "OVERDUE";
export type ChargeKind = "INSTALLMENT" | "SETUP_FEE" | "MANUAL";
export type PaymentMethod = "CASH" | "PIX" | "CREDIT_CARD" | "DEBIT_CARD" | "BANK_TRANSFER" | "OTHER";
export type PaymentStatus = "CONFIRMED" | "REFUNDED";

/** Money is a decimal string; dates are LocalDate strings ("YYYY-MM-DD"). */
export interface Charge {
  id: string;
  chargeNumber: string;
  kind: ChargeKind;
  status: ChargeStatus;
  overdue: boolean;
  studentId: string;
  studentName: string | null;
  studentRegistration: string | null;
  membershipId: string | null;
  membershipRegistrationNumber: string | null;
  branchId: string;
  branchName: string | null;
  installmentNumber: number | null;
  installmentCount: number | null;
  description: string;
  amount: string;
  currency: string;
  dueDate: string;
  periodStart: string | null;
  periodEnd: string | null;
  paidAt: string | null;
  cancelledAt: string | null;
  cancelledBy: string | null;
  cancellationReason: string | null;
  notes: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface Payment {
  id: string;
  paymentNumber: string;
  chargeId: string;
  amount: string;
  currency: string;
  method: PaymentMethod;
  paidAt: string;
  reference: string | null;
  notes: string | null;
  status: PaymentStatus;
  receivedBy: string | null;
  refundedAt: string | null;
  refundedBy: string | null;
  refundReason: string | null;
  createdAt: string;
}

export interface ChargeDetail {
  charge: Charge;
  payments: Payment[];
}

export interface AmountView {
  count: number;
  total: string;
}

export interface BillingSummary {
  from: string;
  to: string;
  today: string;
  branchId: string | null;
  currency: string;
  receivable: AmountView;
  overdue: AmountView;
  received: AmountView;
  receivedByMethod: { method: PaymentMethod; count: number; total: string }[];
  refunded: AmountView;
}

export interface ChargeListParams {
  search?: string;
  status?: string;
  studentId?: string;
  membershipId?: string;
  branchId?: string;
  dueFrom?: string;
  dueTo?: string;
  page: number;
  size: number;
  sort?: string;
  direction?: "asc" | "desc";
}

export interface PaymentInput {
  amount: string;
  method: PaymentMethod;
  /** datetime-local value ("YYYY-MM-DDTHH:mm"); empty = now. */
  paidAt: string;
  reference: string;
  notes: string;
}

export interface ManualChargeInput {
  studentId: string;
  branchId: string;
  membershipId: string;
  description: string;
  amount: string;
  dueDate: string;
  notes: string;
}

export const CHARGE_STATUS_FILTERS: ChargeStatusFilter[] = ["OPEN", "OVERDUE", "PAID", "CANCELLED"];

export const CHARGE_STATUS_LABELS: Record<ChargeStatusFilter, string> = {
  OPEN: "Em aberto",
  OVERDUE: "Vencida",
  PAID: "Paga",
  CANCELLED: "Cancelada",
};

export const CHARGE_STATUS_STYLES: Record<ChargeStatusFilter, string> = {
  OPEN: "border-sky-500/30 bg-sky-500/10 text-sky-300",
  OVERDUE: "border-red-500/30 bg-red-500/10 text-red-300",
  PAID: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
  CANCELLED: "border-white/10 bg-white/5 text-slate-400",
};

export const CHARGE_KIND_LABELS: Record<ChargeKind, string> = {
  INSTALLMENT: "Parcela",
  SETUP_FEE: "Adesão",
  MANUAL: "Avulsa",
};

export const PAYMENT_METHODS: PaymentMethod[] = ["PIX", "CASH", "CREDIT_CARD", "DEBIT_CARD", "BANK_TRANSFER", "OTHER"];

export const PAYMENT_METHOD_LABELS: Record<PaymentMethod, string> = {
  CASH: "Dinheiro",
  PIX: "PIX",
  CREDIT_CARD: "Cartão de crédito",
  DEBIT_CARD: "Cartão de débito",
  BANK_TRANSFER: "Transferência",
  OTHER: "Outro",
};

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  CONFIRMED: "Confirmado",
  REFUNDED: "Estornado",
};

/** Overdue is derived by the backend (OPEN and due before the tenant's today). */
export function displayStatus(charge: Charge): ChargeStatusFilter {
  return charge.overdue ? "OVERDUE" : charge.status;
}
