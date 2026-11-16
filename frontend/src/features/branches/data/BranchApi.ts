import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  BranchDetail,
  BranchInput,
  BranchSettings,
  BranchSettingsInput,
  BranchStatus,
  BranchSummary,
} from "@/features/branches/model/types";

export interface BranchListParams {
  status?: string;
  search?: string;
  page: number;
  size: number;
}

function clean(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function payload(input: BranchInput) {
  return {
    name: input.name.trim(),
    code: input.code.trim().toUpperCase(),
    document: clean(input.document),
    email: clean(input.email),
    phone: clean(input.phone),
    zipCode: clean(input.zipCode),
    street: clean(input.street),
    number: clean(input.number),
    complement: clean(input.complement),
    neighborhood: clean(input.neighborhood),
    city: clean(input.city),
    state: clean(input.state)?.toUpperCase() ?? null,
  };
}

export const branchApi = {
  list(params: BranchListParams): Promise<PageResult<BranchSummary>> {
    return apiClient.get<PageResult<BranchSummary>>(`/api/branches${toQuery({ ...params })}`);
  },

  detail(branchId: string): Promise<BranchDetail> {
    return apiClient.get<BranchDetail>(`/api/branches/${branchId}`);
  },

  create(input: BranchInput): Promise<BranchDetail> {
    return apiClient.post<BranchDetail>("/api/branches", payload(input));
  },

  update(branchId: string, input: BranchInput): Promise<BranchDetail> {
    return apiClient.put<BranchDetail>(`/api/branches/${branchId}`, payload(input));
  },

  changeStatus(branchId: string, status: BranchStatus): Promise<BranchDetail> {
    return apiClient.patch<BranchDetail>(`/api/branches/${branchId}/status`, { status });
  },

  settings(branchId: string): Promise<BranchSettings> {
    return apiClient.get<BranchSettings>(`/api/branches/${branchId}/settings`);
  },

  updateSettings(branchId: string, input: BranchSettingsInput): Promise<BranchSettings> {
    return apiClient.put<BranchSettings>(`/api/branches/${branchId}/settings`, {
      timezone: clean(input.timezone),
      openingTime: clean(input.openingTime),
      closingTime: clean(input.closingTime),
      maxCapacity: input.maxCapacity.trim() ? Number(input.maxCapacity) : null,
      allow24hAccess: input.allow24hAccess,
    });
  },
};
