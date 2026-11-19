import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  BillingSummary,
  Charge,
  ChargeDetail,
  ChargeListParams,
  ManualChargeInput,
  PaymentInput,
} from "@/features/billing/model/types";

function clean(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

export const billingApi = {
  list(params: ChargeListParams): Promise<PageResult<Charge>> {
    return apiClient.get<PageResult<Charge>>(`/api/charges${toQuery({ ...params })}`);
  },

  detail(id: string): Promise<ChargeDetail> {
    return apiClient.get<ChargeDetail>(`/api/charges/${id}`);
  },

  /** `amount` must already be normalized ("123.45"). */
  createManual(input: ManualChargeInput): Promise<ChargeDetail> {
    return apiClient.post<ChargeDetail>("/api/charges", {
      studentId: input.studentId,
      branchId: input.branchId,
      membershipId: clean(input.membershipId),
      description: input.description.trim(),
      amount: input.amount,
      dueDate: input.dueDate,
      notes: clean(input.notes),
    });
  },

  cancel(id: string, reason: string): Promise<ChargeDetail> {
    return apiClient.post<ChargeDetail>(`/api/charges/${id}/cancel`, { reason: reason.trim() });
  },

  /** `amount` must already be normalized; `paidAt` is converted from local time to an ISO instant. */
  registerPayment(id: string, input: PaymentInput): Promise<ChargeDetail> {
    return apiClient.post<ChargeDetail>(`/api/charges/${id}/payments`, {
      amount: input.amount,
      method: input.method,
      paidAt: input.paidAt ? new Date(input.paidAt).toISOString() : null,
      reference: clean(input.reference),
      notes: clean(input.notes),
    });
  },

  refund(paymentId: string, reason: string): Promise<ChargeDetail> {
    return apiClient.post<ChargeDetail>(`/api/payments/${paymentId}/refund`, { reason: reason.trim() });
  },

  summary(params: { from?: string; to?: string; branchId?: string }): Promise<BillingSummary> {
    return apiClient.get<BillingSummary>(`/api/billing/summary${toQuery({ ...params })}`);
  },
};
