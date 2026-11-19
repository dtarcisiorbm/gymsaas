export type PlanStatus = "ACTIVE" | "INACTIVE" | "ARCHIVED";
export type DurationType = "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "ANNUAL" | "CUSTOM_MONTHS" | "CUSTOM_DAYS";
export type BillingPeriod = "MONTHLY" | "QUARTERLY" | "SEMIANNUAL" | "ANNUAL" | "ONE_TIME";
export type PlanFeatureType =
  | "ALL_BRANCHES_ACCESS"
  | "BRANCH_ACCESS"
  | "TIME_WINDOW"
  | "DAILY_CHECKIN_LIMIT"
  | "WEEKLY_CHECKIN_LIMIT"
  | "GUEST_PASSES_PER_MONTH"
  | "CUSTOM";

export const PLAN_STATUSES: PlanStatus[] = ["ACTIVE", "INACTIVE", "ARCHIVED"];
export const PLAN_STATUS_LABELS: Record<PlanStatus, string> = {
  ACTIVE: "Ativo",
  INACTIVE: "Inativo",
  ARCHIVED: "Arquivado",
};

export const PLAN_STATUS_STYLES: Record<PlanStatus, string> = {
  ACTIVE: "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
  INACTIVE: "border-amber-500/30 bg-amber-500/10 text-amber-300",
  ARCHIVED: "border-white/10 bg-white/5 text-slate-400",
};

/** Mirrors PlanStatus.TRANSITIONS in the backend. */
export const PLAN_STATUS_TRANSITIONS: Record<PlanStatus, PlanStatus[]> = {
  ACTIVE: ["INACTIVE", "ARCHIVED"],
  INACTIVE: ["ACTIVE", "ARCHIVED"],
  ARCHIVED: ["INACTIVE"],
};

export const DURATION_TYPES: DurationType[] = [
  "MONTHLY",
  "QUARTERLY",
  "SEMIANNUAL",
  "ANNUAL",
  "CUSTOM_MONTHS",
  "CUSTOM_DAYS",
];
export const DURATION_LABELS: Record<DurationType, string> = {
  MONTHLY: "Mensal",
  QUARTERLY: "Trimestral",
  SEMIANNUAL: "Semestral",
  ANNUAL: "Anual",
  CUSTOM_MONTHS: "Meses personalizados",
  CUSTOM_DAYS: "Dias personalizados",
};

export const BILLING_PERIODS: BillingPeriod[] = ["MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUAL", "ONE_TIME"];
export const BILLING_LABELS: Record<BillingPeriod, string> = {
  MONTHLY: "Mensal",
  QUARTERLY: "Trimestral",
  SEMIANNUAL: "Semestral",
  ANNUAL: "Anual",
  ONE_TIME: "Pagamento único",
};

export const FEATURE_TYPES: PlanFeatureType[] = [
  "ALL_BRANCHES_ACCESS",
  "BRANCH_ACCESS",
  "TIME_WINDOW",
  "DAILY_CHECKIN_LIMIT",
  "WEEKLY_CHECKIN_LIMIT",
  "GUEST_PASSES_PER_MONTH",
  "CUSTOM",
];
export const FEATURE_LABELS: Record<PlanFeatureType, string> = {
  ALL_BRANCHES_ACCESS: "Acesso a todas as unidades",
  BRANCH_ACCESS: "Acesso à unidade",
  TIME_WINDOW: "Janela de horário",
  DAILY_CHECKIN_LIMIT: "Limite de check-ins por dia",
  WEEKLY_CHECKIN_LIMIT: "Limite de check-ins por semana",
  GUEST_PASSES_PER_MONTH: "Convidados por mês",
  CUSTOM: "Benefício personalizado",
};
/** Types the backend accepts at most once per plan. */
export const SINGLETON_FEATURES: PlanFeatureType[] = [
  "ALL_BRANCHES_ACCESS",
  "DAILY_CHECKIN_LIMIT",
  "WEEKLY_CHECKIN_LIMIT",
  "GUEST_PASSES_PER_MONTH",
];
export const FEATURES_WITH_INT: PlanFeatureType[] = [
  "DAILY_CHECKIN_LIMIT",
  "WEEKLY_CHECKIN_LIMIT",
  "GUEST_PASSES_PER_MONTH",
];

/** Bitmask used by the backend: Monday = 1 … Sunday = 64. */
export const WEEKDAYS: { bit: number; short: string }[] = [
  { bit: 1, short: "Seg" },
  { bit: 2, short: "Ter" },
  { bit: 4, short: "Qua" },
  { bit: 8, short: "Qui" },
  { bit: 16, short: "Sex" },
  { bit: 32, short: "Sáb" },
  { bit: 64, short: "Dom" },
];

export function describeWeekdays(mask: number | null): string {
  if (!mask) return "—";
  if (mask === 127) return "Todos os dias";
  return WEEKDAYS.filter((d) => (mask & d.bit) !== 0)
    .map((d) => d.short)
    .join(", ");
}

export interface FreezePolicy {
  maxFreezeDays: number | null;
  maxFreezes: number | null;
  minFreezeDays: number | null;
}

export interface PlanPrice {
  id: string;
  amount: string;
  currency: string;
  billingPeriod: BillingPeriod;
  billingInterval: number;
  setupFee: string;
  validFrom: string;
  validUntil: string | null;
  status: "ACTIVE" | "CANCELLED";
  notes: string | null;
  current: boolean;
  scheduled: boolean;
  createdAt: string;
}

export interface PlanFeature {
  id: string;
  type: PlanFeatureType;
  branchId: string | null;
  branchName: string | null;
  intValue: number | null;
  startTime: string | null;
  endTime: string | null;
  daysOfWeek: number | null;
  label: string | null;
  description: string | null;
  sortOrder: number;
}

export interface PlanSummary {
  id: string;
  code: string;
  name: string;
  durationType: DurationType;
  durationValue: number;
  status: PlanStatus;
  currentPrice: PlanPrice | null;
  updatedAt: string;
}

export interface PlanDetail {
  id: string;
  code: string;
  name: string;
  description: string | null;
  durationType: DurationType;
  durationValue: number;
  status: PlanStatus;
  freezePolicy: FreezePolicy | null;
  contractTerms: string | null;
  version: number;
  currentPrice: PlanPrice | null;
  features: PlanFeature[];
  createdAt: string;
  updatedAt: string;
}

export function describeDuration(type: DurationType, value: number): string {
  if (type === "CUSTOM_MONTHS") return `${value} ${value === 1 ? "mês" : "meses"}`;
  if (type === "CUSTOM_DAYS") return `${value} ${value === 1 ? "dia" : "dias"}`;
  return DURATION_LABELS[type];
}

export function describeBilling(period: BillingPeriod, interval: number): string {
  if (interval <= 1 || period === "ONE_TIME") return BILLING_LABELS[period];
  return `${BILLING_LABELS[period]} × ${interval}`;
}

export interface PlanListParams {
  search?: string;
  status?: string;
  page: number;
  size: number;
  sort?: string;
  direction?: string;
}

/** Form inputs are kept as strings; the repository converts them. */
export interface PriceInput {
  amount: string;
  billingPeriod: BillingPeriod;
  billingInterval: string;
  setupFee: string;
  validFrom: string;
  notes: string;
}

export interface PlanInput {
  code: string;
  name: string;
  description: string;
  durationType: DurationType;
  durationValue: string;
  maxFreezeDays: string;
  maxFreezes: string;
  minFreezeDays: string;
  contractTerms: string;
  withInitialPrice: boolean;
  initialPrice: PriceInput;
}

export interface FeatureInput {
  type: PlanFeatureType;
  branchId: string;
  intValue: string;
  startTime: string;
  endTime: string;
  daysOfWeek: number;
  label: string;
  description: string;
}

export const EMPTY_PRICE_FORM: PriceInput = {
  amount: "",
  billingPeriod: "MONTHLY",
  billingInterval: "1",
  setupFee: "",
  validFrom: "",
  notes: "",
};

export const EMPTY_PLAN_FORM: PlanInput = {
  code: "",
  name: "",
  description: "",
  durationType: "MONTHLY",
  durationValue: "",
  maxFreezeDays: "",
  maxFreezes: "",
  minFreezeDays: "",
  contractTerms: "",
  withInitialPrice: true,
  initialPrice: { ...EMPTY_PRICE_FORM },
};

export const EMPTY_FEATURE_FORM: FeatureInput = {
  type: "ALL_BRANCHES_ACCESS",
  branchId: "",
  intValue: "",
  startTime: "06:00",
  endTime: "22:00",
  daysOfWeek: 31,
  label: "",
  description: "",
};

export function featureToInput(feature: PlanFeature): FeatureInput {
  return {
    type: feature.type,
    branchId: feature.branchId ?? "",
    intValue: feature.intValue === null ? "" : String(feature.intValue),
    startTime: feature.startTime?.slice(0, 5) ?? "06:00",
    endTime: feature.endTime?.slice(0, 5) ?? "22:00",
    daysOfWeek: feature.daysOfWeek ?? 31,
    label: feature.label ?? "",
    description: feature.description ?? "",
  };
}

export function planToInput(plan: PlanDetail): PlanInput {
  const policy = plan.freezePolicy;
  const custom = plan.durationType === "CUSTOM_DAYS" || plan.durationType === "CUSTOM_MONTHS";
  return {
    code: plan.code,
    name: plan.name,
    description: plan.description ?? "",
    durationType: plan.durationType,
    durationValue: custom ? String(plan.durationValue) : "",
    maxFreezeDays: policy?.maxFreezeDays == null ? "" : String(policy.maxFreezeDays),
    maxFreezes: policy?.maxFreezes == null ? "" : String(policy.maxFreezes),
    minFreezeDays: policy?.minFreezeDays == null ? "" : String(policy.minFreezeDays),
    contractTerms: plan.contractTerms ?? "",
    withInitialPrice: false,
    initialPrice: { ...EMPTY_PRICE_FORM },
  };
}
