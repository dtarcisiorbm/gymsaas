export interface UserView {
  id: string;
  email: string;
  fullName: string;
  status: string;
}

export interface TenantView {
  id: string;
  name: string;
  defaultMembership: boolean;
  roles: string[];
}

export interface SessionResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: UserView;
  tenants: TenantView[];
  activeTenantId: string | null;
  platformAdmin: boolean;
}

export interface TenantSelectionResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  tenant: TenantView;
}

export interface MeResponse {
  user: UserView;
  tenant: TenantView | null;
  roles: string[];
  permissions: string[];
  platformAdmin: boolean;
}

export interface PageResult<T> {
  content: T[];
  totalElements: number;
  page: number;
  size: number;
}

export interface TenantDetails {
  id: string;
  name: string;
  legalName: string;
  document: string;
  cnpj: string;
  email: string;
  phone: string | null;
  status: string;
}

export interface UserSummary {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  status: string;
  membershipId: string;
  defaultMembership: boolean;
  roles: string[];
  branchIds: string[];
}

export interface RoleView {
  id: string;
  code: string;
  name: string;
  description: string | null;
  system: boolean;
  permissions: string[];
}
