import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  FeatureInput,
  PlanDetail,
  PlanInput,
  PlanListParams,
  PlanPrice,
  PlanStatus,
  PlanSummary,
  PriceInput,
} from "@/features/plans/model/types";

function clean(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function int(value: string): number | null {
  const trimmed = value.trim();
  return trimmed ? Number(trimmed) : null;
}

function money(value: string): string | null {
  const trimmed = value.trim().replace(",", ".");
  return trimmed ? trimmed : null;
}

function isCustom(input: PlanInput): boolean {
  return input.durationType === "CUSTOM_DAYS" || input.durationType === "CUSTOM_MONTHS";
}

function freezePolicy(input: PlanInput) {
  return {
    maxFreezeDays: int(input.maxFreezeDays),
    maxFreezes: int(input.maxFreezes),
    minFreezeDays: int(input.minFreezeDays),
  };
}

export function pricePayload(input: PriceInput) {
  return {
    amount: money(input.amount),
    currency: "BRL",
    billingPeriod: input.billingPeriod,
    billingInterval: int(input.billingInterval) ?? 1,
    setupFee: money(input.setupFee),
    validFrom: clean(input.validFrom),
    notes: clean(input.notes),
  };
}

export function featurePayload(input: FeatureInput) {
  const base = {
    type: input.type,
    branchId: null as string | null,
    intValue: null as number | null,
    startTime: null as string | null,
    endTime: null as string | null,
    daysOfWeek: null as number | null,
    label: clean(input.label),
    description: clean(input.description),
  };
  switch (input.type) {
    case "BRANCH_ACCESS":
      return { ...base, branchId: clean(input.branchId) };
    case "TIME_WINDOW":
      return {
        ...base,
        startTime: clean(input.startTime),
        endTime: clean(input.endTime),
        daysOfWeek: input.daysOfWeek,
      };
    case "DAILY_CHECKIN_LIMIT":
    case "WEEKLY_CHECKIN_LIMIT":
    case "GUEST_PASSES_PER_MONTH":
      return { ...base, intValue: int(input.intValue) };
    default:
      return base;
  }
}

export const planApi = {
  list(params: PlanListParams): Promise<PageResult<PlanSummary>> {
    return apiClient.get<PageResult<PlanSummary>>(`/api/plans${toQuery({ ...params })}`);
  },

  detail(id: string): Promise<PlanDetail> {
    return apiClient.get<PlanDetail>(`/api/plans/${id}`);
  },

  create(input: PlanInput): Promise<PlanDetail> {
    return apiClient.post<PlanDetail>("/api/plans", {
      code: input.code.trim(),
      name: input.name.trim(),
      description: clean(input.description),
      durationType: input.durationType,
      durationValue: isCustom(input) ? int(input.durationValue) : null,
      freezePolicy: freezePolicy(input),
      contractTerms: clean(input.contractTerms),
      initialPrice: input.withInitialPrice ? pricePayload(input.initialPrice) : null,
    });
  },

  update(id: string, input: PlanInput, version: number): Promise<PlanDetail> {
    return apiClient.put<PlanDetail>(`/api/plans/${id}`, {
      name: input.name.trim(),
      description: clean(input.description),
      durationType: input.durationType,
      durationValue: isCustom(input) ? int(input.durationValue) : null,
      freezePolicy: freezePolicy(input),
      contractTerms: clean(input.contractTerms),
      version,
    });
  },

  changeStatus(id: string, status: PlanStatus): Promise<PlanDetail> {
    return apiClient.patch<PlanDetail>(`/api/plans/${id}/status`, { status });
  },

  prices(id: string): Promise<PlanPrice[]> {
    return apiClient.get<PlanPrice[]>(`/api/plans/${id}/prices`);
  },

  addPrice(id: string, input: PriceInput): Promise<PlanPrice> {
    return apiClient.post<PlanPrice>(`/api/plans/${id}/prices`, pricePayload(input));
  },

  cancelPrice(id: string, priceId: string, reason: string): Promise<PlanPrice> {
    return apiClient.post<PlanPrice>(`/api/plans/${id}/prices/${priceId}/cancel`, { reason: reason.trim() });
  },

  replaceFeatures(id: string, features: FeatureInput[]): Promise<PlanDetail> {
    return apiClient.put<PlanDetail>(`/api/plans/${id}/features`, {
      features: features.map(featurePayload),
    });
  },
};
