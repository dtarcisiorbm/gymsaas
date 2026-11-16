import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { isValidDocument, isValidEmail, onlyDigits, required } from "@/core/validation";
import { tenantApi } from "@/features/tenants/data/TenantApi";
import {
  EMPTY_TENANT_FORM,
  type TenantFormErrors,
  type TenantInput,
} from "@/features/tenants/model/types";

export interface AcademySettingsState {
  loading: boolean;
  saving: boolean;
  error: string | null;
  success: string | null;
  form: TenantInput;
  formErrors: TenantFormErrors;
}

export class AcademySettingsViewModel extends ViewModel<AcademySettingsState> {
  constructor() {
    super({
      loading: true,
      saving: false,
      error: null,
      success: null,
      form: { ...EMPTY_TENANT_FORM },
      formErrors: {},
    });
  }

  load = async (): Promise<void> => {
    this.setState({ loading: true, error: null });
    try {
      const tenant = await tenantApi.current();
      this.setState({
        loading: false,
        form: {
          name: tenant.name,
          legalName: tenant.legalName,
          document: tenant.document,
          email: tenant.email,
          phone: tenant.phone ?? "",
        },
      });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  };

  setField = <K extends keyof TenantInput>(field: K, value: TenantInput[K]): void => {
    const formErrors = { ...this.getSnapshot().formErrors };
    delete formErrors[field];
    this.setState({ form: { ...this.getSnapshot().form, [field]: value }, formErrors, success: null });
  };

  save = async (): Promise<void> => {
    const { form } = this.getSnapshot();
    const formErrors = this.validate(form);
    if (Object.keys(formErrors).length > 0) {
      this.setState({ formErrors });
      return;
    }

    this.setState({ saving: true, error: null, success: null });
    try {
      const updated = await tenantApi.updateCurrent({
        name: form.name.trim(),
        legalName: form.legalName.trim(),
        document: onlyDigits(form.document),
        email: form.email.trim(),
        phone: form.phone.trim(),
      });
      this.setState({
        saving: false,
        success: "Dados da academia atualizados.",
        form: {
          name: updated.name,
          legalName: updated.legalName,
          document: updated.document,
          email: updated.email,
          phone: updated.phone ?? "",
        },
      });
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
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
