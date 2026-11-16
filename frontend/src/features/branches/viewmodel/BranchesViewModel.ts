import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { isValidDocument, isValidEmail, onlyDigits, required } from "@/core/validation";
import { branchApi } from "@/features/branches/data/BranchApi";
import {
  EMPTY_BRANCH_FORM,
  EMPTY_SETTINGS_FORM,
  type BranchFormErrors,
  type BranchInput,
  type BranchSettingsInput,
  type BranchStatus,
  type BranchSummary,
} from "@/features/branches/model/types";

export interface BranchesState {
  items: BranchSummary[];
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
  form: BranchInput;
  formErrors: BranchFormErrors;
  settingsOpen: boolean;
  settingsBranchName: string;
  settingsBranchId: string | null;
  settingsForm: BranchSettingsInput;
  settingsSaving: boolean;
  settingsError: string | null;
}

const CODE_PATTERN = /^[A-Za-z0-9_-]{2,30}$/;

export class BranchesViewModel extends ViewModel<BranchesState> {
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
      form: { ...EMPTY_BRANCH_FORM },
      formErrors: {},
      settingsOpen: false,
      settingsBranchName: "",
      settingsBranchId: null,
      settingsForm: { ...EMPTY_SETTINGS_FORM },
      settingsSaving: false,
      settingsError: null,
    });
  }

  load = async (): Promise<void> => {
    const { page, size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await branchApi.list({
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
    this.setState({
      formOpen: true,
      editingId: null,
      form: { ...EMPTY_BRANCH_FORM },
      formErrors: {},
      error: null,
      success: null,
    });
  };

  openEdit = async (branchId: string): Promise<void> => {
    this.setState({ editingId: branchId, formOpen: true, formErrors: {}, error: null, success: null });
    try {
      const detail = await branchApi.detail(branchId);
      this.setState({
        form: {
          name: detail.name,
          code: detail.code,
          document: detail.document ?? "",
          email: detail.email ?? "",
          phone: detail.phone ?? "",
          zipCode: detail.zipCode ?? "",
          street: detail.street ?? "",
          number: detail.number ?? "",
          complement: detail.complement ?? "",
          neighborhood: detail.neighborhood ?? "",
          city: detail.city ?? "",
          state: detail.state ?? "",
        },
      });
    } catch (error) {
      this.setState({ formOpen: false, editingId: null, error: errorMessage(error) });
    }
  };

  closeForm = (): void => {
    this.setState({ formOpen: false, editingId: null, formErrors: {}, form: { ...EMPTY_BRANCH_FORM } });
  };

  setField = <K extends keyof BranchInput>(field: K, value: BranchInput[K]): void => {
    const form = { ...this.getSnapshot().form, [field]: value };
    const formErrors = { ...this.getSnapshot().formErrors } as Record<string, string | undefined>;
    delete formErrors[field];
    this.setState({ form, formErrors });
  };

  save = async (): Promise<void> => {
    const { form, editingId } = this.getSnapshot();
    const formErrors = this.validateBranch(form);
    if (Object.keys(formErrors).length > 0) {
      this.setState({ formErrors });
      return;
    }

    this.setState({ saving: true, error: null });
    try {
      if (editingId) {
        await branchApi.update(editingId, form);
      } else {
        await branchApi.create(form);
      }
      this.setState({
        saving: false,
        formOpen: false,
        editingId: null,
        form: { ...EMPTY_BRANCH_FORM },
        success: editingId ? "Unidade atualizada com sucesso." : "Unidade cadastrada com sucesso.",
      });
      await this.load();
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
    }
  };

  changeStatus = async (branchId: string, status: BranchStatus): Promise<void> => {
    this.setState({ statusUpdatingId: branchId, error: null, success: null });
    try {
      await branchApi.changeStatus(branchId, status);
      this.setState({ statusUpdatingId: null, success: `Unidade ${status === "ACTIVE" ? "ativada" : "desativada"}.` });
      await this.load();
    } catch (error) {
      this.setState({ statusUpdatingId: null, error: errorMessage(error) });
    }
  };

  openSettings = async (branchId: string, branchName: string): Promise<void> => {
    this.setState({
      settingsOpen: true,
      settingsBranchId: branchId,
      settingsBranchName: branchName,
      settingsError: null,
      settingsForm: { ...EMPTY_SETTINGS_FORM },
    });
    try {
      const settings = await branchApi.settings(branchId);
      this.setState({
        settingsForm: {
          timezone: settings.timezone ?? "America/Sao_Paulo",
          openingTime: settings.openingTime ? settings.openingTime.slice(0, 5) : "",
          closingTime: settings.closingTime ? settings.closingTime.slice(0, 5) : "",
          maxCapacity: settings.maxCapacity ? String(settings.maxCapacity) : "",
          allow24hAccess: settings.allow24hAccess,
        },
      });
    } catch (error) {
      this.setState({ settingsError: errorMessage(error) });
    }
  };

  closeSettings = (): void => {
    this.setState({ settingsOpen: false, settingsBranchId: null, settingsError: null });
  };

  setSettingsField = <K extends keyof BranchSettingsInput>(field: K, value: BranchSettingsInput[K]): void => {
    this.setState({ settingsForm: { ...this.getSnapshot().settingsForm, [field]: value }, settingsError: null });
  };

  saveSettings = async (): Promise<void> => {
    const { settingsBranchId, settingsForm } = this.getSnapshot();
    if (!settingsBranchId) {
      return;
    }
    if (!settingsForm.timezone.trim()) {
      this.setState({ settingsError: "Informe o fuso horário." });
      return;
    }
    if (settingsForm.maxCapacity.trim() && (!/^\d+$/.test(settingsForm.maxCapacity.trim()) || Number(settingsForm.maxCapacity) < 1)) {
      this.setState({ settingsError: "Capacidade deve ser um número inteiro positivo." });
      return;
    }
    if (settingsForm.openingTime && settingsForm.closingTime && settingsForm.openingTime >= settingsForm.closingTime) {
      this.setState({ settingsError: "O horário de fechamento deve ser depois da abertura." });
      return;
    }

    this.setState({ settingsSaving: true, settingsError: null });
    try {
      await branchApi.updateSettings(settingsBranchId, settingsForm);
      this.setState({
        settingsSaving: false,
        settingsOpen: false,
        settingsBranchId: null,
        success: "Configurações da unidade atualizadas.",
      });
    } catch (error) {
      this.setState({ settingsSaving: false, settingsError: errorMessage(error) });
    }
  };

  dismissFeedback = (): void => {
    this.setState({ error: null, success: null });
  };

  private loadFromFirstPage = async (): Promise<void> => {
    const { size, statusFilter, search } = this.getSnapshot();
    this.setState({ loading: true, error: null });
    try {
      const result = await branchApi.list({
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

  private validateBranch(form: BranchInput): BranchFormErrors {
    const errors: BranchFormErrors = {};
    if (!required(form.name)) {
      errors.name = "Informe o nome da unidade.";
    }
    if (!CODE_PATTERN.test(form.code.trim())) {
      errors.code = "Código deve ter de 2 a 30 caracteres (letras, números, - ou _).";
    }
    if (form.document.trim() && !isValidDocument(form.document)) {
      errors.document = "Documento deve ter 11 ou 14 dígitos.";
    }
    if (form.email.trim() && !isValidEmail(form.email)) {
      errors.email = "Informe um e-mail válido.";
    }
    if (form.phone.trim().length > 20) {
      errors.phone = "Telefone deve ter no máximo 20 caracteres.";
    }
    if (form.zipCode.trim() && onlyDigits(form.zipCode).length !== 8) {
      errors.zipCode = "CEP deve ter 8 dígitos.";
    }
    if (form.state.trim() && !/^[A-Za-z]{2}$/.test(form.state.trim())) {
      errors.state = "UF deve ter 2 letras.";
    }
    return errors;
  }
}
