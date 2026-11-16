export type BranchStatus = "ACTIVE" | "INACTIVE";

export const BRANCH_STATUSES: BranchStatus[] = ["ACTIVE", "INACTIVE"];

export interface BranchSummary {
  id: string;
  name: string;
  code: string;
  city: string | null;
  state: string | null;
  phone: string | null;
  status: BranchStatus;
  createdAt: string;
}

export interface BranchDetail {
  id: string;
  tenantId: string;
  name: string;
  code: string;
  document: string | null;
  email: string | null;
  phone: string | null;
  zipCode: string | null;
  street: string | null;
  number: string | null;
  complement: string | null;
  neighborhood: string | null;
  city: string | null;
  state: string | null;
  status: BranchStatus;
  createdAt: string;
  updatedAt: string;
}

export interface BranchInput {
  name: string;
  code: string;
  document: string;
  email: string;
  phone: string;
  zipCode: string;
  street: string;
  number: string;
  complement: string;
  neighborhood: string;
  city: string;
  state: string;
}

export interface BranchFormErrors {
  name?: string;
  code?: string;
  document?: string;
  email?: string;
  phone?: string;
  zipCode?: string;
  state?: string;
}

export interface BranchSettings {
  id: string;
  branchId: string;
  timezone: string;
  openingTime: string | null;
  closingTime: string | null;
  maxCapacity: number | null;
  allow24hAccess: boolean;
  settings: Record<string, unknown>;
}

export interface BranchSettingsInput {
  timezone: string;
  openingTime: string;
  closingTime: string;
  maxCapacity: string;
  allow24hAccess: boolean;
}

export const EMPTY_BRANCH_FORM: BranchInput = {
  name: "",
  code: "",
  document: "",
  email: "",
  phone: "",
  zipCode: "",
  street: "",
  number: "",
  complement: "",
  neighborhood: "",
  city: "",
  state: "",
};

export const EMPTY_SETTINGS_FORM: BranchSettingsInput = {
  timezone: "America/Sao_Paulo",
  openingTime: "",
  closingTime: "",
  maxCapacity: "",
  allow24hAccess: false,
};
