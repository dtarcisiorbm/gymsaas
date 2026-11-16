import { apiClient } from "@/core/http/ApiClient";
import type {
  MeResponse,
  SessionResponse,
  TenantSelectionResponse,
  TenantView,
} from "@/features/auth/model/types";

export const authApi = {
  login(email: string, password: string): Promise<SessionResponse> {
    return apiClient.post<SessionResponse>(
      "/api/auth/login",
      { email, password },
      { auth: false, skipRefresh: true },
    );
  },

  refresh(): Promise<SessionResponse> {
    return apiClient.request<SessionResponse>("/api/auth/refresh", {
      method: "POST",
      auth: false,
      skipRefresh: true,
    });
  },

  logout(): Promise<void> {
    return apiClient.request<void>("/api/auth/logout", {
      method: "POST",
      auth: false,
      skipRefresh: true,
    });
  },

  me(): Promise<MeResponse> {
    return apiClient.get<MeResponse>("/api/auth/me");
  },

  tenants(): Promise<TenantView[]> {
    return apiClient.get<TenantView[]>("/api/auth/tenants");
  },

  selectTenant(tenantId: string): Promise<TenantSelectionResponse> {
    return apiClient.post<TenantSelectionResponse>("/api/auth/select-tenant", { tenantId });
  },
};
