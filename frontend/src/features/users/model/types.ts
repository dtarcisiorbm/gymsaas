export type UserStatus = "ACTIVE" | "INACTIVE" | "LOCKED";

export const USER_STATUSES: UserStatus[] = ["ACTIVE", "INACTIVE", "LOCKED"];

export interface UserAdminSummary {
  id: string;
  email: string;
  fullName: string;
  phone: string | null;
  status: UserStatus;
  membershipId: string;
  defaultMembership: boolean;
  roles: string[];
  branchIds: string[];
}

export interface UserBranchView {
  id: string;
  name: string;
  code: string;
  status: string;
}

export interface CreateUserInput {
  email: string;
  fullName: string;
  password: string;
  phone: string;
  roleIds: string[];
  branchIds: string[];
}

export interface UpdateUserInput {
  fullName: string;
  phone: string;
}

export interface UserFormErrors {
  email?: string;
  fullName?: string;
  password?: string;
  phone?: string;
}

export const EMPTY_USER_FORM = {
  email: "",
  fullName: "",
  password: "",
  phone: "",
  roleIds: [] as string[],
  branchIds: [] as string[],
};
