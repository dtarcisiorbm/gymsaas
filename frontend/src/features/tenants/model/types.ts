export type TenantStatus = "PENDING" | "ACTIVE" | "SUSPENDED" | "INACTIVE";

export const TENANT_STATUSES: TenantStatus[] = ["PENDING", "ACTIVE", "SUSPENDED", "INACTIVE"];

export interface TenantSummary {
  id: string;
  name: string;
  document: string;
  email: string;
  status: TenantStatus;
  createdAt: string;
}

export interface TenantDetail {
  id: string;
  name: string;
  legalName: string;
  document: string;
  email: string;
  phone: string | null;
  status: TenantStatus;
  createdAt: string;
  updatedAt: string;
}

export interface TenantInput {
  name: string;
  legalName: string;
  document: string;
  email: string;
  phone: string;
}

export interface TenantFormErrors {
  name?: string;
  legalName?: string;
  document?: string;
  email?: string;
  phone?: string;
}

export const EMPTY_TENANT_FORM: TenantInput = {
  name: "",
  legalName: "",
  document: "",
  email: "",
  phone: "",
};
