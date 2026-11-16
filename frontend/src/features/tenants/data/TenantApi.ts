import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type { BranchSummary } from "@/features/branches/model/types";
import type { TenantDetail, TenantInput, TenantStatus, TenantSummary } from "@/features/tenants/model/types";

export interface TenantListParams {
  status?: string;
  search?: string;
  page: number;
  size: number;
}

export const tenantApi = {
  list(params: TenantListParams): Promise<PageResult<TenantSummary>> {
    return apiClient.get<PageResult<TenantSummary>>(`/api/admin/tenants${toQuery({ ...params })}`);
  },

  detail(tenantId: string): Promise<TenantDetail> {
    return apiClient.get<TenantDetail>(`/api/admin/tenants/${tenantId}`);
  },

  branches(tenantId: string, params: TenantListParams): Promise<PageResult<BranchSummary>> {
    return apiClient.get<PageResult<BranchSummary>>(
      `/api/admin/tenants/${tenantId}/branches${toQuery({ ...params })}`,
    );
  },

  create(input: TenantInput): Promise<TenantDetail> {
    return apiClient.post<TenantDetail>("/api/admin/tenants", input);
  },

  update(tenantId: string, input: TenantInput): Promise<TenantDetail> {
    return apiClient.put<TenantDetail>(`/api/admin/tenants/${tenantId}`, input);
  },

  changeStatus(tenantId: string, status: TenantStatus): Promise<TenantDetail> {
    return apiClient.patch<TenantDetail>(`/api/admin/tenants/${tenantId}/status`, { status });
  },

  current(): Promise<TenantDetail> {
    return apiClient.get<TenantDetail>("/api/tenants/current");
  },

  updateCurrent(input: TenantInput): Promise<TenantDetail> {
    return apiClient.put<TenantDetail>("/api/tenants/current", input);
  },
};

