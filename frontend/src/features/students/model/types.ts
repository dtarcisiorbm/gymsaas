export type StudentStatus = "PENDING" | "ACTIVE" | "INACTIVE" | "BLOCKED" | "ARCHIVED";

export const STUDENT_STATUSES: StudentStatus[] = [
  "PENDING",
  "ACTIVE",
  "INACTIVE",
  "BLOCKED",
  "ARCHIVED",
];

export const STUDENT_STATUS_LABELS: Record<StudentStatus, string> = {
  PENDING: "Pendente",
  ACTIVE: "Ativo",
  INACTIVE: "Inativo",
  BLOCKED: "Bloqueado",
  ARCHIVED: "Arquivado",
};

export type DocumentType = "CPF" | "RG" | "CNH" | "PASSPORT" | "OTHER";

export interface StudentSummary {
  id: string;
  registrationNumber: string;
  name: string;
  socialName: string | null;
  cpfMasked: string | null;
  phone: string | null;
  status: StudentStatus;
  primaryBranchId: string | null;
  primaryBranchName: string | null;
  createdAt: string;
}

export interface StudentAddress {
  id?: string;
  zipCode: string | null;
  street: string | null;
  number: string | null;
  complement: string | null;
  neighborhood: string | null;
  city: string | null;
  state: string | null;
}

export interface StudentDocument {
  id: string;
  type: DocumentType;
  number: string;
  issuer: string | null;
  state: string | null;
  issuedAt: string | null;
  expiresAt: string | null;
  notes: string | null;
  primary: boolean;
}

export interface StudentContact {
  id: string;
  name: string;
  relationship: string | null;
  phone: string;
  email: string | null;
  primary: boolean;
  notes: string | null;
}

export interface StudentBranchAssignment {
  id: string;
  branchId: string;
  branchName: string | null;
  primary: boolean;
  startedAt: string;
  endedAt: string | null;
  transferReason: string | null;
}

export interface StudentHistoryEntry {
  occurredAt: string;
  kind: string;
  title: string;
  detail: string | null;
  actorId: string | null;
}

export interface StudentNote {
  id: string;
  content: string;
  createdBy: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface StudentDetail {
  id: string;
  tenantId: string;
  registrationNumber: string;
  name: string;
  socialName: string | null;
  birthDate: string | null;
  email: string | null;
  phone: string | null;
  gender: string | null;
  photoUrl: string | null;
  status: StudentStatus;
  notes: string | null;
  cpf: string | null;
  cpfMasked: string | null;
  address: StudentAddress | null;
  documents: StudentDocument[];
  contacts: StudentContact[];
  branches: StudentBranchAssignment[];
  createdAt: string;
  updatedAt: string;
}

export interface StudentListParams {
  search?: string;
  document?: string;
  status?: string;
  branchId?: string;
  page: number;
  size: number;
  sort?: string;
  direction?: string;
}

export interface StudentInput {
  name: string;
  socialName: string;
  birthDate: string;
  email: string;
  phone: string;
  gender: string;
  photoUrl: string;
  status: string;
  notes: string;
  cpf: string;
  primaryBranchId: string;
}

export interface StudentUpdateInput {
  name: string;
  socialName: string;
  birthDate: string;
  email: string;
  phone: string;
  gender: string;
  photoUrl: string;
  notes: string;
  cpf: string;
}

export interface DocumentInput {
  type: DocumentType;
  number: string;
  issuer: string;
  state: string;
  issuedAt: string;
  expiresAt: string;
  notes: string;
  primary: boolean;
}

export interface ContactInput {
  name: string;
  relationship: string;
  phone: string;
  email: string;
  primary: boolean;
  notes: string;
}

export interface AddressInput {
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
}

export interface StudentFormErrors {
  name?: string;
  cpf?: string;
  email?: string;
  birthDate?: string;
  phone?: string;
  zipCode?: string;
  state?: string;
}

export const EMPTY_STUDENT_FORM: StudentInput = {
  name: "",
  socialName: "",
  birthDate: "",
  email: "",
  phone: "",
  gender: "",
  photoUrl: "",
  status: "PENDING",
  notes: "",
  cpf: "",
  primaryBranchId: "",
};

export const EMPTY_DOCUMENT_FORM: DocumentInput = {
  type: "RG",
  number: "",
  issuer: "",
  state: "",
  issuedAt: "",
  expiresAt: "",
  notes: "",
  primary: false,
};

export const EMPTY_CONTACT_FORM: ContactInput = {
  name: "",
  relationship: "",
  phone: "",
  email: "",
  primary: false,
  notes: "",
};

export const EMPTY_ADDRESS_FORM: AddressInput = {
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
};
