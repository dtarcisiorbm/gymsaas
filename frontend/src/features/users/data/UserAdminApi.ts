import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  CreateUserInput,
  UpdateUserInput,
  UserAdminSummary,
  UserBranchView,
  UserStatus,
} from "@/features/users/model/types";

export interface UserListParams {
  status?: string;
  search?: string;
  page: number;
  size: number;
}

export const userAdminApi = {
  list(params: UserListParams): Promise<PageResult<UserAdminSummary>> {
    return apiClient.get<PageResult<UserAdminSummary>>(`/api/users${toQuery({ ...params })}`);
  },

  create(input: CreateUserInput): Promise<UserAdminSummary> {
    return apiClient.post<UserAdminSummary>("/api/users", {
      email: input.email.trim(),
      fullName: input.fullName.trim(),
      password: input.password,
      phone: input.phone.trim() || null,
      roleIds: input.roleIds,
      branchIds: input.branchIds,
    });
  },

  update(userId: string, input: UpdateUserInput): Promise<UserAdminSummary> {
    return apiClient.put<UserAdminSummary>(`/api/users/${userId}`, {
      fullName: input.fullName.trim(),
      phone: input.phone.trim() || null,
    });
  },

  changeStatus(userId: string, status: UserStatus): Promise<UserAdminSummary> {
    return apiClient.patch<UserAdminSummary>(`/api/users/${userId}/status`, { status });
  },

  assignRoles(userId: string, roleIds: string[]): Promise<UserAdminSummary> {
    return apiClient.put<UserAdminSummary>(`/api/users/${userId}/roles`, { roleIds });
  },

  branches(userId: string): Promise<UserBranchView[]> {
    return apiClient.get<UserBranchView[]>(`/api/users/${userId}/branches`);
  },

  replaceBranches(userId: string, branchIds: string[]): Promise<UserBranchView[]> {
    return apiClient.put<UserBranchView[]>(`/api/users/${userId}/branches`, { branchIds });
  },
};
