import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult, RoleView, TenantDetails, UserSummary } from "@/features/auth/model/types";

export const dashboardApi = {
  currentTenant(): Promise<TenantDetails> {
    return apiClient.get<TenantDetails>("/api/tenants/current");
  },

  users(page = 0, size = 5): Promise<PageResult<UserSummary>> {
    return apiClient.get<PageResult<UserSummary>>(`/api/users${toQuery({ page, size })}`);
  },

  roles(): Promise<RoleView[]> {
    return apiClient.get<RoleView[]>("/api/roles");
  },
};
