import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { planApi } from "@/features/plans/data/PlanApi";
import {
  EMPTY_PLAN_FORM,
  EMPTY_PRICE_FORM,
  type PlanInput,
  type PlanStatus,
  type PlanSummary,
  type PriceInput,
} from "@/features/plans/model/types";
import { validatePlan, type FormErrors } from "@/features/plans/model/validation";

export interface PlansState extends FeedbackState {
  items: PlanSummary[];
  loading: boolean;
  page: number;
  size: number;
  totalElements: number;
  search: string;
  statusFilter: string;
  sort: string;
  direction: "asc" | "desc";
  busyId: string | null;
  createOpen: boolean;
  form: PlanInput;
  formErrors: FormErrors;
}

const initialState: PlansState = {
  items: [],
  loading: true,
  page: 0,
  size: 20,
  totalElements: 0,
  search: "",
  statusFilter: "",
  sort: "name",
  direction: "asc",
  busyId: null,
  createOpen: false,
  form: { ...EMPTY_PLAN_FORM, initialPrice: { ...EMPTY_PRICE_FORM } },
  formErrors: {},
  error: null,
  success: null,
  saving: false,
};

export class PlansViewModel extends FeedbackViewModel<PlansState> {
  constructor() {
    super(initialState);
  }

  async load(page = this.state.page): Promise<void> {
    this.setState({ loading: true, error: null, page });
    try {
      const result = await planApi.list({
        search: this.state.search,
        status: this.state.statusFilter,
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

  setSearch = (search: string) => this.setState({ search });
  setStatusFilter = (statusFilter: string) => this.setState({ statusFilter, page: 0 });
  applyFilters = () => void this.load(0);

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

  openCreate = () =>
    this.setState({
      createOpen: true,
      form: { ...EMPTY_PLAN_FORM, initialPrice: { ...EMPTY_PRICE_FORM } },
      formErrors: {},
      error: null,
    });

  closeCreate = () => this.setState({ createOpen: false });

  updateForm = (patch: Partial<PlanInput>) => this.setState({ form: { ...this.state.form, ...patch } });

  updatePrice = (patch: Partial<PriceInput>) =>
    this.setState({ form: { ...this.state.form, initialPrice: { ...this.state.form.initialPrice, ...patch } } });

  /** Returns the created plan id so the page can navigate to it. */
  async create(): Promise<string | null> {
    const formErrors = validatePlan(this.state.form, true);
    this.setState({ formErrors });
    if (Object.keys(formErrors).length > 0) return null;
    let createdId: string | null = null;
    const ok = await this.run(async () => {
      const created = await planApi.create(this.state.form);
      createdId = created.id;
    }, "Plano cadastrado.");
    if (ok) {
      this.setState({ createOpen: false });
      await this.load(0);
    }
    return createdId;
  }

  async changeStatus(id: string, status: PlanStatus): Promise<void> {
    this.setState({ busyId: id });
    const ok = await this.run(() => planApi.changeStatus(id, status).then(() => undefined), "Status do plano atualizado.");
    this.setState({ busyId: null });
    if (ok) await this.load(this.state.page);
  }
}
