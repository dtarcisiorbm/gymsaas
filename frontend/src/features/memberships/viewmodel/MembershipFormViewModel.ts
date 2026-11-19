import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { normalizeMoney } from "@/core/validation";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { membershipApi } from "@/features/memberships/data/MembershipApi";
import {
  EMPTY_MEMBERSHIP_FORM,
  type MembershipInput,
  type MembershipQuote,
} from "@/features/memberships/model/types";
import { planApi } from "@/features/plans/data/PlanApi";
import type { PlanSummary } from "@/features/plans/model/types";
import { studentApi } from "@/features/students/data/StudentApi";
import type { StudentSummary } from "@/features/students/model/types";

export interface MembershipFormState extends FeedbackState {
  form: MembershipInput;
  studentQuery: string;
  studentResults: StudentSummary[];
  searchingStudents: boolean;
  selectedStudent: StudentSummary | null;
  plans: PlanSummary[];
  branches: BranchSummary[];
  quote: MembershipQuote | null;
  quoting: boolean;
  fieldErrors: Record<string, string>;
}

const initialState: MembershipFormState = {
  form: { ...EMPTY_MEMBERSHIP_FORM },
  studentQuery: "",
  studentResults: [],
  searchingStudents: false,
  selectedStudent: null,
  plans: [],
  branches: [],
  quote: null,
  quoting: false,
  fieldErrors: {},
  error: null,
  success: null,
  saving: false,
};

/**
 * Two-step hiring: the operator fills the form, the backend computes the
 * quote (price, dates, discount) and only then the membership is created.
 * Any change to the form invalidates the quote.
 */
export class MembershipFormViewModel extends FeedbackViewModel<MembershipFormState> {
  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private searchSeq = 0;

  constructor() {
    super(initialState);
  }

  async init(studentId: string | null): Promise<void> {
    const [plans, branches] = await Promise.allSettled([
      planApi.list({ status: "ACTIVE", page: 0, size: 100, sort: "name", direction: "asc" }),
      branchApi.list({ status: "ACTIVE", page: 0, size: 100 }),
    ]);
    if (plans.status === "fulfilled") this.setState({ plans: plans.value.content });
    if (branches.status === "fulfilled") this.setState({ branches: branches.value.content });
    if (plans.status === "rejected") this.setState({ error: errorMessage(plans.reason) });

    if (studentId) {
      try {
        const student = await studentApi.detail(studentId);
        const primary = student.branches.find((b) => b.primary && !b.endedAt);
        this.selectStudent({
          id: student.id,
          registrationNumber: student.registrationNumber,
          name: student.name,
          socialName: student.socialName,
          cpfMasked: student.cpfMasked,
          phone: student.phone,
          status: student.status,
          primaryBranchId: primary?.branchId ?? null,
          primaryBranchName: primary?.branchName ?? null,
          createdAt: student.createdAt,
        });
      } catch (error) {
        this.setState({ error: errorMessage(error) });
      }
    }
  }

  setStudentQuery = (studentQuery: string) => {
    this.setState({ studentQuery });
    if (this.searchTimer) clearTimeout(this.searchTimer);
    if (studentQuery.trim().length < 2) {
      this.setState({ studentResults: [], searchingStudents: false });
      return;
    }
    this.searchTimer = setTimeout(() => void this.searchStudents(studentQuery), 300);
  };

  private async searchStudents(query: string): Promise<void> {
    const seq = ++this.searchSeq;
    this.setState({ searchingStudents: true });
    try {
      const digits = query.replace(/\D/g, "");
      const result = await studentApi.list(
        digits.length >= 3 && digits.length === query.trim().replace(/[.\-/\s]/g, "").length
          ? { document: query, page: 0, size: 8 }
          : { search: query, page: 0, size: 8 },
      );
      if (seq === this.searchSeq) this.setState({ studentResults: result.content, searchingStudents: false });
    } catch (error) {
      if (seq === this.searchSeq) this.setState({ searchingStudents: false, error: errorMessage(error) });
    }
  }

  selectStudent = (student: StudentSummary) => {
    const branchId = this.state.form.branchId || student.primaryBranchId || "";
    this.setState({
      selectedStudent: student,
      studentResults: [],
      studentQuery: "",
      form: { ...this.state.form, studentId: student.id, branchId },
      quote: null,
    });
  };

  clearStudent = () =>
    this.setState({ selectedStudent: null, form: { ...this.state.form, studentId: "" }, quote: null });

  updateForm = (patch: Partial<MembershipInput>) =>
    this.setState({ form: { ...this.state.form, ...patch }, quote: null });

  private validate(): boolean {
    const { form } = this.state;
    const fieldErrors: Record<string, string> = {};
    if (!form.studentId) fieldErrors.studentId = "Selecione o aluno.";
    if (!form.planId) fieldErrors.planId = "Selecione o plano.";
    if (!form.branchId) fieldErrors.branchId = "Selecione a unidade.";
    if (form.discountAmount.trim()) {
      if (!normalizeMoney(form.discountAmount)) fieldErrors.discountAmount = "Valor inválido (até 2 casas).";
      if (!form.discountReason.trim()) fieldErrors.discountReason = "Informe o motivo do desconto.";
    }
    this.setState({ fieldErrors });
    return Object.keys(fieldErrors).length === 0;
  }

  async requestQuote(): Promise<void> {
    if (!this.validate()) return;
    this.setState({ quoting: true, error: null });
    try {
      this.setState({ quote: await membershipApi.quote(this.state.form), quoting: false });
    } catch (error) {
      this.setState({ quoting: false, error: errorMessage(error) });
    }
  }

  /** Returns the new membership id on success. */
  async confirm(): Promise<string | null> {
    if (!this.state.quote || !this.validate()) return null;
    let createdId: string | null = null;
    await this.run(async () => {
      createdId = (await membershipApi.create(this.state.form)).id;
    }, "Matrícula criada.");
    return createdId;
  }
}
