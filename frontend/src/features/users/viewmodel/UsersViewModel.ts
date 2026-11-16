import { errorMessage } from "@/core/http/ApiError";
import { apiClient } from "@/core/http/ApiClient";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { isValidEmail, required } from "@/core/validation";
import type { RoleView } from "@/features/auth/model/types";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { userAdminApi } from "@/features/users/data/UserAdminApi";
import {
  EMPTY_USER_FORM,
  type UserAdminSummary,
  type UserFormErrors,
  type UserStatus,
} from "@/features/users/model/types";

export interface UserFormState {
  email: string;
  fullName: string;
  password: string;
  phone: string;
  roleIds: string[];
  branchIds: string[];
}

export interface UsersState {
  items: UserAdminSummary[];
  loading: boolean;
  page: number;
  size: number;
  totalElements: number;
  statusFilter: string;
  search: string;
  error: string | null;
  success: string | null;
  saving: boolean;
  statusUpdatingId: string | null;
  formOpen: boolean;
  editingId: string | null;
  form: UserFormState;
  formErrors: UserFormErrors;
  roles: RoleView[];
  branches: BranchSummary[];
  optionsError: string | null;
  rolesOpen: boolean;
  rolesUserId: string | null;
  rolesUserName: string;
  selectedRoleIds: string[];
  rolesSaving: boolean;
  rolesError: string | null;
  branchesOpen: boolean;
  branchesUserId: string | null;
  branchesUserName: string;
  selectedBranchIds: string[];
  branchesSaving: boolean;
  branchesError: string | null;
}

export class UsersViewModel extends ViewModel<UsersState> {
  constructor() {
    super({
      items: [],
      loading: true,
      page: 0,
      size: 10,
      totalElements: 0,
      statusFilter: "",
      search: "",
      error: null,
      success: null,
      saving: false,
      statusUpdatingId: null,
      formOpen: false,
      editingId: null,
      form: { ...EMPTY_USER_FORM },
      formErrors: {},
      roles: [],
      branches: [],
      optionsError: null,
      rolesOpen: false,
      rolesUserId: null,
      rolesUserName: "",
      selectedRoleIds: [],
      rolesSaving: false,
      rolesError: null,
      branchesOpen: false,
      branchesUserId: null,
      branchesUserName: "",
      selectedBranchIds: [],
      branchesSaving: false,
      branchesError: null,
    });
  }

  load = async (): Promise<void> => {
    const { page, size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await userAdminApi.list({
        status: statusFilter || undefined,
        search: search.trim() || undefined,
        page,
        size,
      });
      this.setState({
        items: result.content,
        totalElements: result.totalElements,
        page: result.page,
        loading: false,
      });
    } catch (error) {
      this.setState({ error: errorMessage(error), loading: false });
    }
  };

  loadOptions = async (): Promise<void> => {
    try {
      const [roles, branches] = await Promise.all([
        apiClient.get<RoleView[]>("/api/roles").catch(() => []),
        branchApi.list({ page: 0, size: 100 }).then((page) => page.content).catch(() => []),
      ]);
      this.setState({ roles, branches, optionsError: null });
    } catch {
      this.setState({ optionsError: "Não foi possível carregar papéis e unidades." });
    }
  };

  setStatusFilter = (statusFilter: string): void => {
    this.setState({ statusFilter });
  };

  setSearch = (search: string): void => {
    this.setState({ search });
  };

  applyFilters = (): void => {
    this.setState({ page: 0, success: null });
    void this.loadFromFirstPage();
  };

  changePage = (delta: number): void => {
    const { page, totalElements, size } = this.getSnapshot();
    const next = page + delta;
    if (next < 0 || next * size >= totalElements) {
      return;
    }
    this.setState({ page: next });
    void this.load();
  };

  changeSize = (size: number): void => {
    this.setState({ size, page: 0 });
    void this.load();
  };

  openCreate = (): void => {
    void this.loadOptions();
    this.setState({
      formOpen: true,
      editingId: null,
      form: { ...EMPTY_USER_FORM },
      formErrors: {},
      error: null,
      success: null,
    });
  };

  openEdit = (userId: string): void => {
    const user = this.getSnapshot().items.find((item) => item.id === userId);
    if (!user) {
      return;
    }
    this.setState({
      formOpen: true,
      editingId: userId,
      form: {
        email: user.email,
        fullName: user.fullName,
        password: "",
        phone: user.phone ?? "",
        roleIds: [],
        branchIds: [],
      },
      formErrors: {},
      error: null,
      success: null,
    });
  };

  closeForm = (): void => {
    this.setState({ formOpen: false, editingId: null, formErrors: {}, form: { ...EMPTY_USER_FORM } });
  };

  setField = (field: "email" | "fullName" | "password" | "phone", value: string): void => {
    const form = { ...this.getSnapshot().form, [field]: value };
    const formErrors = { ...this.getSnapshot().formErrors };
    delete formErrors[field];
    this.setState({ form, formErrors });
  };

  toggleFormRole = (roleId: string, checked: boolean): void => {
    const { form } = this.getSnapshot();
    this.setState({
      form: {
        ...form,
        roleIds: checked ? [...form.roleIds, roleId] : form.roleIds.filter((id) => id !== roleId),
      },
    });
  };

  toggleFormBranch = (branchId: string, checked: boolean): void => {
    const { form } = this.getSnapshot();
    this.setState({
      form: {
        ...form,
        branchIds: checked ? [...form.branchIds, branchId] : form.branchIds.filter((id) => id !== branchId),
      },
    });
  };

  save = async (): Promise<void> => {
    const { form, editingId } = this.getSnapshot();
    const formErrors = this.validate(form, editingId !== null);
    if (Object.keys(formErrors).length > 0) {
      this.setState({ formErrors });
      return;
    }

    this.setState({ saving: true, error: null });
    try {
      if (editingId) {
        await userAdminApi.update(editingId, { fullName: form.fullName, phone: form.phone });
      } else {
        await userAdminApi.create(form);
      }
      this.setState({
        saving: false,
        formOpen: false,
        editingId: null,
        form: { ...EMPTY_USER_FORM },
        success: editingId ? "Usuário atualizado com sucesso." : "Usuário criado e vinculado à academia.",
      });
      await this.load();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    }
  };

  changeStatus = async (userId: string, status: UserStatus): Promise<void> => {
    this.setState({ statusUpdatingId: userId, error: null, success: null });
    try {
      await userAdminApi.changeStatus(userId, status);
      this.setState({ statusUpdatingId: null, success: `Status do usuário alterado para ${status}.` });
      await this.load();
    } catch (error) {
      this.setState({ statusUpdatingId: null, error: errorMessage(error) });
    }
  };

  openRoles = (userId: string): void => {
    const user = this.getSnapshot().items.find((item) => item.id === userId);
    if (!user) {
      return;
    }
    const roleIds = this.getSnapshot().roles
      .filter((role) => user.roles.includes(role.code))
      .map((role) => role.id);
    this.setState({
      rolesOpen: true,
      rolesUserId: userId,
      rolesUserName: user.fullName,
      selectedRoleIds: roleIds,
      rolesError: null,
    });
    if (this.getSnapshot().roles.length === 0) {
      void this.loadOptions();
    }
  };

  closeRoles = (): void => {
    this.setState({ rolesOpen: false, rolesUserId: null, rolesError: null });
  };

  toggleRole = (roleId: string, checked: boolean): void => {
    const { selectedRoleIds } = this.getSnapshot();
    this.setState({
      selectedRoleIds: checked ? [...selectedRoleIds, roleId] : selectedRoleIds.filter((id) => id !== roleId),
    });
  };

  saveRoles = async (): Promise<void> => {
    const { rolesUserId, selectedRoleIds } = this.getSnapshot();
    if (!rolesUserId) {
      return;
    }
    this.setState({ rolesSaving: true, rolesError: null });
    try {
      await userAdminApi.assignRoles(rolesUserId, selectedRoleIds);
      this.setState({ rolesSaving: false, rolesOpen: false, rolesUserId: null, success: "Papéis atualizados." });
      await this.load();
    } catch (error) {
      this.setState({ rolesSaving: false, rolesError: errorMessage(error) });
    }
  };

  openBranches = (userId: string): void => {
    const user = this.getSnapshot().items.find((item) => item.id === userId);
    if (!user) {
      return;
    }
    this.setState({
      branchesOpen: true,
      branchesUserId: userId,
      branchesUserName: user.fullName,
      selectedBranchIds: [...user.branchIds],
      branchesError: null,
    });
    if (this.getSnapshot().branches.length === 0) {
      void this.loadOptions();
    }
  };

  closeBranches = (): void => {
    this.setState({ branchesOpen: false, branchesUserId: null, branchesError: null });
  };

  toggleBranch = (branchId: string, checked: boolean): void => {
    const { selectedBranchIds } = this.getSnapshot();
    this.setState({
      selectedBranchIds: checked
        ? [...selectedBranchIds, branchId]
        : selectedBranchIds.filter((id) => id !== branchId),
    });
  };

  saveBranches = async (): Promise<void> => {
    const { branchesUserId, selectedBranchIds } = this.getSnapshot();
    if (!branchesUserId) {
      return;
    }
    this.setState({ branchesSaving: true, branchesError: null });
    try {
      await userAdminApi.replaceBranches(branchesUserId, selectedBranchIds);
      this.setState({ branchesSaving: false, branchesOpen: false, branchesUserId: null, success: "Unidades do usuário atualizadas." });
      await this.load();
    } catch (error) {
      this.setState({ branchesSaving: false, branchesError: errorMessage(error) });
    }
  };

  dismissFeedback = (): void => {
    this.setState({ error: null, success: null });
  };

  private loadFromFirstPage = async (): Promise<void> => {
    const { size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await userAdminApi.list({
        status: statusFilter || undefined,
        search: search.trim() || undefined,
        page: 0,
        size,
      });
      this.setState({
        items: result.content,
        totalElements: result.totalElements,
        page: 0,
        loading: false,
      });
    } catch (error) {
      this.setState({ error: errorMessage(error), loading: false });
    }
  };

  private validate(form: UserFormState, editing: boolean): UserFormErrors {
    const errors: UserFormErrors = {};
    if (!editing && !isValidEmail(form.email)) {
      errors.email = "Informe um e-mail válido.";
    }
    if (!required(form.fullName)) {
      errors.fullName = "Informe o nome completo.";
    }
    if (!editing && form.password.length < 10) {
      errors.password = "A senha deve ter ao menos 10 caracteres.";
    }
    if (form.phone.trim().length > 20) {
      errors.phone = "Telefone deve ter no máximo 20 caracteres.";
    }
    return errors;
  }
}
