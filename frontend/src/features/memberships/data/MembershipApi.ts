import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  MembershipDetail,
  MembershipHistoryEntry,
  MembershipInput,
  MembershipListParams,
  MembershipQuote,
  MembershipSummary,
  RenewInput,
} from "@/features/memberships/model/types";

function clean(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function money(value: string): string | null {
  const trimmed = value.trim().replace(",", ".");
  return trimmed ? trimmed : null;
}

/** The client never sends prices: only an optional discount the backend validates. */
function createPayload(input: MembershipInput) {
  const discount = money(input.discountAmount);
  return {
    studentId: input.studentId,
    planId: input.planId,
    branchId: input.branchId,
    startDate: clean(input.startDate),
    discountAmount: discount,
    discountReason: discount ? clean(input.discountReason) : null,
    notes: clean(input.notes),
  };
}

export const membershipApi = {
  list(params: MembershipListParams): Promise<PageResult<MembershipSummary>> {
    return apiClient.get<PageResult<MembershipSummary>>(`/api/memberships${toQuery({ ...params })}`);
  },

  detail(id: string): Promise<MembershipDetail> {
    return apiClient.get<MembershipDetail>(`/api/memberships/${id}`);
  },

  quote(input: MembershipInput): Promise<MembershipQuote> {
    return apiClient.post<MembershipQuote>("/api/memberships/quote", createPayload(input));
  },

  create(input: MembershipInput): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>("/api/memberships", createPayload(input));
  },

  update(id: string, branchId: string, notes: string, version: number): Promise<MembershipDetail> {
    return apiClient.put<MembershipDetail>(`/api/memberships/${id}`, { branchId, notes: clean(notes), version });
  },

  renew(id: string, input: RenewInput): Promise<MembershipDetail> {
    const discount = money(input.discountAmount);
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/renew`, {
      startDate: clean(input.startDate),
      branchId: clean(input.branchId),
      discountAmount: discount,
      discountReason: discount ? clean(input.discountReason) : null,
      notes: clean(input.notes),
    });
  },

  cancel(id: string, reason: string): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/cancel`, { reason: reason.trim() });
  },

  freeze(id: string, endDate: string, reason: string): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/freeze`, { endDate, reason: reason.trim() });
  },

  unfreeze(id: string, reason: string): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/unfreeze`, { reason: clean(reason) });
  },

  suspend(id: string, reason: string): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/suspend`, { reason: reason.trim() });
  },

  reactivate(id: string, reason: string): Promise<MembershipDetail> {
    return apiClient.post<MembershipDetail>(`/api/memberships/${id}/reactivate`, { reason: reason.trim() });
  },

  history(id: string): Promise<MembershipHistoryEntry[]> {
    return apiClient.get<MembershipHistoryEntry[]>(`/api/memberships/${id}/history`);
  },
};
