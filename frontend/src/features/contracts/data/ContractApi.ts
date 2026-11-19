import { apiClient } from "@/core/http/ApiClient";
import type { ContractSummary, ContractView } from "@/features/contracts/model/types";

export const contractApi = {
  history(membershipId: string): Promise<ContractSummary[]> {
    return apiClient.get<ContractSummary[]>(`/api/memberships/${membershipId}/contracts`);
  },

  generate(membershipId: string): Promise<ContractView> {
    return apiClient.post<ContractView>(`/api/memberships/${membershipId}/contracts`);
  },

  detail(id: string): Promise<ContractView> {
    return apiClient.get<ContractView>(`/api/contracts/${id}`);
  },

  document(id: string): Promise<Blob> {
    return apiClient.download(`/api/contracts/${id}/document`);
  },

  /**
   * contentSha256 must be the hash of the content shown to the student; the
   * backend rejects the acceptance if the stored content differs.
   */
  accept(id: string, acceptorName: string, contentSha256: string, confirmed: boolean): Promise<ContractView> {
    return apiClient.post<ContractView>(`/api/contracts/${id}/acceptance`, {
      acceptorName: acceptorName.trim(),
      contentSha256,
      confirmed,
    });
  },
};
