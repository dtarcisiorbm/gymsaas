import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { todayIso } from "@/core/format";
import { normalizeMoney } from "@/core/validation";
import { billingApi } from "@/features/billing/data/BillingApi";
import type {
  BillingSummary,
  Charge,
  ChargeDetail,
  ManualChargeInput,
  PaymentInput,
} from "@/features/billing/model/types";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { studentApi } from "@/features/students/data/StudentApi";
import type { StudentSummary } from "@/features/students/model/types";

export type ChargeModal = "none" | "detail" | "pay" | "cancel" | "refund" | "create";

export interface ChargesState extends FeedbackState {
  items: Charge[];
  loading: boolean;
  page: number;
  size: number;
  totalElements: number;
  search: string;
  statusFilter: string;
  branchFilter: string;
  dueFrom: string;
  dueTo: string;
  sort: string;
  direction: "asc" | "desc";
  /** Fixed scope (membership tab or ?studentId=/?membershipId= links). */
  studentFilter: string;
  membershipFilter: string;
  branches: BranchSummary[];
  summary: BillingSummary | null;
  summaryFrom: string;
  summaryTo: string;
  summaryError: string | null;
  modal: ChargeModal;
  selected: ChargeDetail | null;
  loadingDetail: boolean;
  payment: PaymentInput;
  reason: string;
  refundPaymentId: string | null;
  create: ManualChargeInput;
  createStudent: StudentSummary | null;
  studentQuery: string;
  studentResults: StudentSummary[];
  fieldErrors: Record<string, string>;
}

export interface ChargesOptions {
  showSummary: boolean;
  studentId?: string | null;
  membershipId?: string | null;
}

const emptyPayment: PaymentInput = { amount: "", method: "PIX", paidAt: "", reference: "", notes: "" };

function emptyCreate(): ManualChargeInput {
  return { studentId: "", branchId: "", membershipId: "", description: "", amount: "", dueDate: todayIso(), notes: "" };
}

const initialState: ChargesState = {
  items: [],
  loading: true,
  page: 0,
  size: 20,
  totalElements: 0,
  search: "",
  statusFilter: "",
  branchFilter: "",
  dueFrom: "",
  dueTo: "",
  sort: "dueDate",
  direction: "asc",
  studentFilter: "",
  membershipFilter: "",
  branches: [],
  summary: null,
  summaryFrom: "",
  summaryTo: "",
  summaryError: null,
  modal: "none",
  selected: null,
  loadingDetail: false,
  payment: emptyPayment,
  reason: "",
  refundPaymentId: null,
  create: emptyCreate(),
  createStudent: null,
  studentQuery: "",
  studentResults: [],
  fieldErrors: {},
  error: null,
  success: null,
  saving: false,
};

export type ChargeFilterKey = "search" | "statusFilter" | "branchFilter" | "dueFrom" | "dueTo";

export class ChargesViewModel extends FeedbackViewModel<ChargesState> {
  private options: ChargesOptions = { showSummary: false };
  private searchTimer: ReturnType<typeof setTimeout> | null = null;
  private searchSeq = 0;

  constructor() {
    super(initialState);
  }

  init(options: ChargesOptions): void {
    this.options = options;
    this.setState({
      studentFilter: options.studentId ?? "",
      membershipFilter: options.membershipId ?? "",
    });
    void this.load(0);
    void this.loadBranches();
    if (options.showSummary) void this.loadSummary();
  }

  async load(page = this.state.page): Promise<void> {
    this.setState({ loading: true, error: null, page });
    try {
      const result = await billingApi.list({
        search: this.state.search,
        status: this.state.statusFilter,
        branchId: this.state.branchFilter,
        studentId: this.state.studentFilter,
        membershipId: this.state.membershipFilter,
        dueFrom: this.state.dueFrom,
        dueTo: this.state.dueTo,
        page,
        size: this.state.size,
        sort: this.state.sort,
        direction: this.state.direction,
      });
      this.setState({ items: result.content, totalElements: result.totalElements, page: result.page, loading: false });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  }

  async loadSummary(): Promise<void> {
    this.setState({ summaryError: null });
    try {
      const summary = await billingApi.summary({
        from: this.state.summaryFrom,
        to: this.state.summaryTo,
        branchId: this.state.branchFilter,
      });
      this.setState({ summary, summaryFrom: summary.from, summaryTo: summary.to });
    } catch (error) {
      this.setState({ summaryError: errorMessage(error) });
    }
  }

  private async loadBranches(): Promise<void> {
    try {
      const result = await branchApi.list({ page: 0, size: 100 });
      this.setState({ branches: result.content });
    } catch {
      // Branch filter is optional; the list still works without it.
    }
  }

  /** Summary needs `billing:reports`; permissions may arrive after the first render. */
  enableSummary(enabled: boolean): void {
    if (this.options.showSummary === enabled) return;
    this.options = { ...this.options, showSummary: enabled };
    if (enabled) void this.loadSummary();
    else this.setState({ summary: null });
  }

  private refresh(): void {
    void this.load();
    if (this.options.showSummary) void this.loadSummary();
  }

  setFilter = (key: ChargeFilterKey, value: string) => this.setState({ [key]: value } as Partial<ChargesState>);

  setSummaryPeriod = (key: "summaryFrom" | "summaryTo", value: string) =>
    this.setState({ [key]: value } as Partial<ChargesState>);

  applyFilters = () => {
    void this.load(0);
    if (this.options.showSummary) void this.loadSummary();
  };

  clearScope = () => {
    this.setState({ studentFilter: "", membershipFilter: "" });
    void this.load(0);
  };

  changePage = (delta: number) => {
    const next = this.state.page + delta;
    const maxPage = Math.max(0, Math.ceil(this.state.totalElements / this.state.size) - 1);
    if (next < 0 || next > maxPage) return;
    void this.load(next);
  };

  changeSize = (size: number) => {
    this.setState({ size, page: 0 });
    void this.load(0);
  };

  toggleSort = (sort: string) => {
    const direction = this.state.sort === sort && this.state.direction === "asc" ? "desc" : "asc";
    this.setState({ sort, direction, page: 0 });
    void this.load(0);
  };

  // ---- modals -------------------------------------------------------------

  closeModal = () =>
    this.setState({ modal: "none", error: null, fieldErrors: {}, reason: "", refundPaymentId: null });

  openDetail = async (chargeId: string) => {
    this.setState({ modal: "detail", loadingDetail: true, selected: null, error: null });
    try {
      const selected = await billingApi.detail(chargeId);
      this.setState({ selected, loadingDetail: false });
    } catch (error) {
      this.setState({ loadingDetail: false, error: errorMessage(error) });
    }
  };

  openPay = async (charge: Charge) => {
    await this.ensureSelected(charge.id);
    this.setState({
      modal: "pay",
      error: null,
      fieldErrors: {},
      payment: { ...emptyPayment, amount: charge.amount },
    });
  };

  openCancel = async (charge: Charge) => {
    await this.ensureSelected(charge.id);
    this.setState({ modal: "cancel", reason: "", error: null, fieldErrors: {} });
  };

  openRefund = (paymentId: string) =>
    this.setState({ modal: "refund", refundPaymentId: paymentId, reason: "", error: null, fieldErrors: {} });

  openCreate = async () => {
    const create = emptyCreate();
    create.studentId = this.state.studentFilter;
    create.membershipId = this.state.membershipFilter;
    let createStudent: StudentSummary | null = null;
    if (this.state.studentFilter) {
      try {
        const detail = await studentApi.detail(this.state.studentFilter);
        createStudent = {
          id: detail.id,
          registrationNumber: detail.registrationNumber,
          name: detail.name,
          socialName: detail.socialName,
          cpfMasked: null,
          phone: null,
          status: detail.status,
          primaryBranchId: null,
          primaryBranchName: null,
          createdAt: detail.createdAt,
        };
      } catch {
        create.studentId = "";
      }
    }
    if (!create.branchId && this.state.branches.length === 1) create.branchId = this.state.branches[0].id;
    this.setState({
      modal: "create",
      create,
      createStudent,
      studentQuery: "",
      studentResults: [],
      error: null,
      fieldErrors: {},
    });
  };

  private async ensureSelected(chargeId: string): Promise<void> {
    if (this.state.selected?.charge.id === chargeId) return;
    try {
      this.setState({ selected: await billingApi.detail(chargeId) });
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    }
  }

  setReason = (reason: string) => this.setState({ reason });

  updatePayment = (patch: Partial<PaymentInput>) => this.setState({ payment: { ...this.state.payment, ...patch } });

  updateCreate = (patch: Partial<ManualChargeInput>) => this.setState({ create: { ...this.state.create, ...patch } });

  setStudentQuery = (studentQuery: string) => {
    this.setState({ studentQuery });
    if (this.searchTimer) clearTimeout(this.searchTimer);
    if (studentQuery.trim().length < 2) {
      this.setState({ studentResults: [] });
      return;
    }
    this.searchTimer = setTimeout(() => void this.searchStudents(studentQuery), 300);
  };

  private async searchStudents(query: string): Promise<void> {
    const seq = ++this.searchSeq;
    try {
      const result = await studentApi.list({ search: query, page: 0, size: 8 });
      if (seq === this.searchSeq) this.setState({ studentResults: result.content });
    } catch (error) {
      if (seq === this.searchSeq) this.setState({ error: errorMessage(error) });
    }
  }

  selectStudent = (student: StudentSummary) =>
    this.setState({
      createStudent: student,
      studentResults: [],
      studentQuery: "",
      create: {
        ...this.state.create,
        studentId: student.id,
        branchId: this.state.create.branchId || student.primaryBranchId || "",
      },
    });

  clearStudent = () =>
    this.setState({ createStudent: null, create: { ...this.state.create, studentId: "", membershipId: "" } });

  // ---- mutations ----------------------------------------------------------

  submitPayment = async (): Promise<void> => {
    const charge = this.state.selected?.charge;
    if (!charge) return;
    const amount = normalizeMoney(this.state.payment.amount);
    const fieldErrors: Record<string, string> = {};
    if (!amount) fieldErrors.amount = "Informe um valor válido (até 2 casas decimais).";
    else if (Number(amount) !== Number(charge.amount)) fieldErrors.amount = "O valor deve ser igual ao da cobrança.";
    if (this.state.payment.paidAt && new Date(this.state.payment.paidAt).getTime() > Date.now() + 5 * 60_000) {
      fieldErrors.paidAt = "A data do recebimento não pode estar no futuro.";
    }
    this.setState({ fieldErrors });
    if (Object.keys(fieldErrors).length > 0 || !amount) return;
    const ok = await this.run(async () => {
      const selected = await billingApi.registerPayment(charge.id, { ...this.state.payment, amount });
      this.setState({ selected, modal: "detail" });
    }, "Recebimento registrado.");
    if (ok) this.refresh();
  };

  submitCancel = async (): Promise<void> => {
    const charge = this.state.selected?.charge;
    if (!charge) return;
    if (!this.state.reason.trim()) {
      this.setState({ fieldErrors: { reason: "Informe o motivo." } });
      return;
    }
    const ok = await this.run(async () => {
      const selected = await billingApi.cancel(charge.id, this.state.reason);
      this.setState({ selected, modal: "detail", reason: "" });
    }, "Cobrança cancelada.");
    if (ok) this.refresh();
  };

  submitRefund = async (): Promise<void> => {
    const paymentId = this.state.refundPaymentId;
    if (!paymentId) return;
    if (!this.state.reason.trim()) {
      this.setState({ fieldErrors: { reason: "Informe o motivo do estorno." } });
      return;
    }
    const ok = await this.run(async () => {
      const selected = await billingApi.refund(paymentId, this.state.reason);
      this.setState({ selected, modal: "detail", reason: "", refundPaymentId: null });
    }, "Recebimento estornado; a cobrança voltou a ficar em aberto.");
    if (ok) this.refresh();
  };

  submitCreate = async (): Promise<void> => {
    const form = this.state.create;
    const amount = normalizeMoney(form.amount);
    const fieldErrors: Record<string, string> = {};
    if (!form.studentId) fieldErrors.studentId = "Selecione o aluno.";
    if (!form.branchId) fieldErrors.branchId = "Selecione a unidade.";
    if (!form.description.trim()) fieldErrors.description = "Informe a descrição.";
    if (!amount || Number(amount) <= 0) fieldErrors.amount = "Informe um valor maior que zero.";
    if (!form.dueDate) fieldErrors.dueDate = "Informe o vencimento.";
    this.setState({ fieldErrors });
    if (Object.keys(fieldErrors).length > 0 || !amount) return;
    const ok = await this.run(async () => {
      const selected = await billingApi.createManual({ ...form, amount });
      this.setState({ selected, modal: "detail" });
    }, "Cobrança lançada.");
    if (ok) this.refresh();
  };
}
