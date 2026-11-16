import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { isValidDocument, isValidEmail, onlyDigits, required } from "@/core/validation";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { studentApi } from "@/features/students/data/StudentApi";
import {
  EMPTY_STUDENT_FORM,
  type StudentFormErrors,
  type StudentInput,
  type StudentUpdateInput,
} from "@/features/students/model/types";

export interface StudentFormState {
  loading: boolean;
  saving: boolean;
  editingId: string | null;
  form: StudentInput;
  errors: StudentFormErrors;
  branches: BranchSummary[];
  error: string | null;
  success: string | null;
}

const initialState: StudentFormState = {
  loading: false,
  saving: false,
  editingId: null,
  form: { ...EMPTY_STUDENT_FORM },
  errors: {},
  branches: [],
  error: null,
  success: null,
};

export class StudentFormViewModel extends ViewModel<StudentFormState> {
  constructor() {
    super(initialState);
  }

  async init(id?: string): Promise<void> {
    this.setState({ loading: Boolean(id), editingId: id ?? null, error: null, success: null });
    try {
      const [branches, detail] = await Promise.all([
        branchApi.list({ page: 0, size: 100 }).then((r) => r.content).catch(() => [] as BranchSummary[]),
        id ? studentApi.detail(id) : Promise.resolve(null),
      ]);
      if (detail) {
        const primary = detail.branches.find((b) => b.primary && !b.endedAt);
        this.setState({
          loading: false,
          branches,
          form: {
            name: detail.name ?? "",
            socialName: detail.socialName ?? "",
            birthDate: detail.birthDate ?? "",
            email: detail.email ?? "",
            phone: detail.phone ?? "",
            gender: detail.gender ?? "",
            photoUrl: detail.photoUrl ?? "",
            status: detail.status ?? "PENDING",
            notes: detail.notes ?? "",
            cpf: detail.cpf ?? "",
            primaryBranchId: primary?.branchId ?? "",
          },
        });
      } else {
        this.setState({ loading: false, branches });
      }
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  }

  setField = (field: keyof StudentInput, value: string) => {
    this.setState({
      form: { ...this.state.form, [field]: value },
      errors: { ...this.state.errors, [field]: undefined },
    });
  };

  validate(): boolean {
    const form = this.state.form;
    const errors: StudentFormErrors = {};
    if (!required(form.name)) {
      errors.name = "Nome é obrigatório.";
    }
    if (form.cpf) {
      const digits = onlyDigits(form.cpf);
      if (digits.length !== 11 || !isValidDocument(form.cpf)) {
        errors.cpf = "CPF inválido.";
      }
    }
    if (form.email && !isValidEmail(form.email)) {
      errors.email = "E-mail inválido.";
    }
    if (form.birthDate) {
      const birth = new Date(form.birthDate);
      if (Number.isNaN(birth.getTime()) || birth > new Date()) {
        errors.birthDate = "Data de nascimento inválida.";
      }
    }
    this.setState({ errors });
    return Object.keys(errors).length === 0;
  }

  async save(): Promise<boolean> {
    if (!this.validate()) return false;
    this.setState({ saving: true, error: null, success: null });
    try {
      const form = this.state.form;
      if (this.state.editingId) {
        const payload: StudentUpdateInput = {
          name: form.name,
          socialName: form.socialName,
          birthDate: form.birthDate,
          email: form.email,
          phone: form.phone,
          gender: form.gender,
          photoUrl: form.photoUrl,
          notes: form.notes,
          cpf: form.cpf,
        };
        await studentApi.update(this.state.editingId, payload);
        this.setState({ saving: false, success: "Aluno atualizado com sucesso." });
      } else {
        await studentApi.create(form);
        this.setState({ saving: false, success: "Aluno cadastrado com sucesso." });
      }
      return true;
    } catch (error) {
      this.setState({ saving: false, error: errorMessage(error) });
      return false;
    }
  }

  dismissFeedback = () => this.setState({ error: null, success: null });
}
