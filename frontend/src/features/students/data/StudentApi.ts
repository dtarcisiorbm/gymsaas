import { apiClient } from "@/core/http/ApiClient";
import { toQuery } from "@/core/http/query";
import type { PageResult } from "@/features/auth/model/types";
import type {
  AddressInput,
  ContactInput,
  DocumentInput,
  StudentAddress,
  StudentBranchAssignment,
  StudentContact,
  StudentDetail,
  StudentDocument,
  StudentHistoryEntry,
  StudentInput,
  StudentListParams,
  StudentNote,
  StudentSummary,
  StudentUpdateInput,
} from "@/features/students/model/types";

function clean(value: string): string | null {
  const trimmed = value.trim();
  return trimmed ? trimmed : null;
}

function digits(value: string): string | null {
  const cleaned = value.replace(/\D/g, "").trim();
  return cleaned ? cleaned : null;
}

function createPayload(input: StudentInput) {
  return {
    name: input.name.trim(),
    socialName: clean(input.socialName),
    birthDate: clean(input.birthDate),
    email: clean(input.email),
    phone: clean(input.phone),
    gender: clean(input.gender),
    photoUrl: clean(input.photoUrl),
    status: clean(input.status),
    notes: clean(input.notes),
    cpf: digits(input.cpf),
    primaryBranchId: clean(input.primaryBranchId),
  };
}

function updatePayload(input: StudentUpdateInput) {
  return {
    name: input.name.trim(),
    socialName: clean(input.socialName),
    birthDate: clean(input.birthDate),
    email: clean(input.email),
    phone: clean(input.phone),
    gender: clean(input.gender),
    photoUrl: clean(input.photoUrl),
    notes: clean(input.notes),
    cpf: digits(input.cpf),
  };
}

export const studentApi = {
  list(params: StudentListParams): Promise<PageResult<StudentSummary>> {
    return apiClient.get<PageResult<StudentSummary>>(
      `/api/students${toQuery({
        search: params.search,
        document: params.document,
        status: params.status,
        branchId: params.branchId,
        page: params.page,
        size: params.size,
        sort: params.sort,
        direction: params.direction,
      })}`,
    );
  },

  detail(id: string, reveal = false): Promise<StudentDetail> {
    return apiClient.get<StudentDetail>(`/api/students/${id}${toQuery({ reveal })}`);
  },

  create(input: StudentInput): Promise<StudentDetail> {
    return apiClient.post<StudentDetail>("/api/students", createPayload(input));
  },

  update(id: string, input: StudentUpdateInput): Promise<StudentDetail> {
    return apiClient.put<StudentDetail>(`/api/students/${id}`, updatePayload(input));
  },

  remove(id: string): Promise<void> {
    return apiClient.delete<void>(`/api/students/${id}`);
  },

  changeStatus(id: string, status: string, reason?: string): Promise<StudentDetail> {
    return apiClient.patch<StudentDetail>(`/api/students/${id}/status`, {
      status,
      reason: clean(reason ?? ""),
    });
  },

  history(id: string): Promise<StudentHistoryEntry[]> {
    return apiClient.get<StudentHistoryEntry[]>(`/api/students/${id}/history`);
  },

  listDocuments(id: string, reveal = false): Promise<StudentDocument[]> {
    return apiClient.get<StudentDocument[]>(`/api/students/${id}/documents${toQuery({ reveal })}`);
  },

  addDocument(id: string, input: DocumentInput): Promise<StudentDocument> {
    return apiClient.post<StudentDocument>(`/api/students/${id}/documents`, {
      type: input.type,
      number: input.number.trim(),
      issuer: clean(input.issuer),
      state: clean(input.state)?.toUpperCase() ?? null,
      issuedAt: clean(input.issuedAt),
      expiresAt: clean(input.expiresAt),
      notes: clean(input.notes),
      primary: input.primary,
    });
  },

  updateDocument(id: string, documentId: string, input: DocumentInput): Promise<StudentDocument> {
    return apiClient.put<StudentDocument>(`/api/students/${id}/documents/${documentId}`, {
      type: input.type,
      number: input.number.trim(),
      issuer: clean(input.issuer),
      state: clean(input.state)?.toUpperCase() ?? null,
      issuedAt: clean(input.issuedAt),
      expiresAt: clean(input.expiresAt),
      notes: clean(input.notes),
      primary: input.primary,
    });
  },

  removeDocument(id: string, documentId: string): Promise<void> {
    return apiClient.delete<void>(`/api/students/${id}/documents/${documentId}`);
  },

  listContacts(id: string): Promise<StudentContact[]> {
    return apiClient.get<StudentContact[]>(`/api/students/${id}/contacts`);
  },

  addContact(id: string, input: ContactInput): Promise<StudentContact> {
    return apiClient.post<StudentContact>(`/api/students/${id}/contacts`, {
      name: input.name.trim(),
      relationship: clean(input.relationship),
      phone: input.phone.trim(),
      email: clean(input.email),
      primary: input.primary,
      notes: clean(input.notes),
    });
  },

  updateContact(id: string, contactId: string, input: ContactInput): Promise<StudentContact> {
    return apiClient.put<StudentContact>(`/api/students/${id}/contacts/${contactId}`, {
      name: input.name.trim(),
      relationship: clean(input.relationship),
      phone: input.phone.trim(),
      email: clean(input.email),
      primary: input.primary,
      notes: clean(input.notes),
    });
  },

  removeContact(id: string, contactId: string): Promise<void> {
    return apiClient.delete<void>(`/api/students/${id}/contacts/${contactId}`);
  },

  getAddress(id: string): Promise<StudentAddress> {
    return apiClient.get<StudentAddress>(`/api/students/${id}/address`);
  },

  saveAddress(id: string, input: AddressInput): Promise<StudentAddress> {
    return apiClient.put<StudentAddress>(`/api/students/${id}/address`, {
      zipCode: clean(input.zipCode),
      street: clean(input.street),
      number: clean(input.number),
      complement: clean(input.complement),
      neighborhood: clean(input.neighborhood),
      city: clean(input.city),
      state: clean(input.state)?.toUpperCase() ?? null,
    });
  },

  listBranches(id: string): Promise<StudentBranchAssignment[]> {
    return apiClient.get<StudentBranchAssignment[]>(`/api/students/${id}/branches`);
  },

  assignBranch(id: string, branchId: string, primary: boolean): Promise<StudentBranchAssignment> {
    return apiClient.post<StudentBranchAssignment>(`/api/students/${id}/branches`, { branchId, primary });
  },

  transfer(id: string, branchId: string, reason?: string): Promise<StudentBranchAssignment> {
    return apiClient.post<StudentBranchAssignment>(`/api/students/${id}/transfer`, {
      branchId,
      reason: clean(reason ?? ""),
    });
  },

  removeBranch(id: string, assignmentId: string): Promise<void> {
    return apiClient.delete<void>(`/api/students/${id}/branches/${assignmentId}`);
  },

  listNotes(id: string): Promise<StudentNote[]> {
    return apiClient.get<StudentNote[]>(`/api/students/${id}/notes`);
  },

  addNote(id: string, content: string): Promise<StudentNote> {
    return apiClient.post<StudentNote>(`/api/students/${id}/notes`, { content: content.trim() });
  },
};
