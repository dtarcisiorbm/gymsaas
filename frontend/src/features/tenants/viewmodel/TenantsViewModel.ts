import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { isValidDocument, isValidEmail, onlyDigits, required } from "@/core/validation";
import { tenantApi } from "@/features/tenants/data/TenantApi";
import {
  EMPTY_TENANT_FORM,
  type TenantFormErrors,
  type TenantInput,
  type TenantStatus,
  type TenantSummary,
} from "@/features/tenants/model/types";

export interface TenantsState {
  items: TenantSummary[];
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
  form: TenantInput;
  formErrors: TenantFormErrors;
}

export class TenantsViewModel extends ViewModel<TenantsState> {
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
      form: { ...EMPTY_TENANT_FORM },
      formErrors: {},
    });
  }

  load = async (): Promise<void> => {
    const { page, size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await tenantApi.list({
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

  setStatusFilter = (statusFilter: string): void => {
    this.setState({ statusFilter });
  };

  setSearch = (search: string): void => {
    this.setState({ search });
  };

  applyFilters = (): void => {
    this.setState({ page: 0, success: null });
    void this.reloadFromFirstPage();
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
    this.setState({
      formOpen: true,
      editingId: null,
      form: { ...EMPTY_TENANT_FORM },
      formErrors: {},
      error: null,
      success: null,
    });
  };

  openEdit = async (tenantId: string): Promise<void> => {
    this.setState({ editingId: tenantId, formOpen: true, formErrors: {}, error: null, success: null });
    try {
      const detail = await tenantApi.detail(tenantId);
      this.setState({
        form: {
          name: detail.name,
          legalName: detail.legalName,
          document: detail.document,
          email: detail.email,
          phone: detail.phone ?? "",
        },
      });
    } catch (error) {
      this.setState({ formOpen: false, editingId: null, error: errorMessage(error) });
    }
  };

  closeForm = (): void => {
    this.setState({ formOpen: false, editingId: null, formErrors: {}, form: { ...EMPTY_TENANT_FORM } });
  };

  setField = <K extends keyof TenantInput>(field: K, value: TenantInput[K]): void => {
    const form = { ...this.getSnapshot().form, [field]: value };
    const formErrors = { ...this.getSnapshot().formErrors };
    delete formErrors[field];
    this.setState({ form, formErrors });
  };

  save = async (): Promise<void> => {
    const { form, editingId } = this.getSnapshot();
    const formErrors = this.validate(form);
    if (Object.keys(formErrors).length > 0) {
      this.setState({ formErrors });
      return;
    }

    const payload: TenantInput = {
      name: form.name.trim(),
      legalName: form.legalName.trim(),
      document: onlyDigits(form.document),
      email: form.email.trim(),
      phone: form.phone.trim(),
    };

    this.setState({ saving: true, error: null });
    try {
      if (editingId) {
        await tenantApi.update(editingId, payload);
      } else {
        await tenantApi.create(payload);
      }
      this.setState({
        saving: false,
        formOpen: false,
        editingId: null,
        form: { ...EMPTY_TENANT_FORM },
        success: editingId ? "Academia atualizada com sucesso." : "Academia cadastrada com status pendente.",
      });
      await this.load();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    }
  };

  changeStatus = async (tenantId: string, status: TenantStatus): Promise<void> => {
    this.setState({ statusUpdatingId: tenantId, error: null, success: null });
    try {
      await tenantApi.changeStatus(tenantId, status);
      this.setState({ statusUpdatingId: null, success: `Status alterado para ${status}.` });
      await this.load();
    } catch (error) {
      this.setState({ statusUpdatingId: null, error: errorMessage(error) });
    }
  };

  dismissFeedback = (): void => {
    this.setState({ error: null, success: null });
  };

  private reloadFromFirstPage = async (): Promise<void> => {
    const { size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await tenantApi.list({
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

  private validate(form: TenantInput): TenantFormErrors {
    const errors: TenantFormErrors = {};
    if (!required(form.name)) {
      errors.name = "Informe o nome da academia.";
    }
    if (!required(form.legalName)) {
      errors.legalName = "Informe a razão social.";
    }
    if (!isValidDocument(form.document)) {
      errors.document = "Informe um CPF (11 dígitos) ou CNPJ (14 dígitos) válido.";
    }
    if (!isValidEmail(form.email)) {
      errors.email = "Informe um e-mail válido.";
    }
    if (form.phone.trim().length > 20) {
      errors.phone = "Telefone deve ter no máximo 20 caracteres.";
    }
    return errors;
  }
}
