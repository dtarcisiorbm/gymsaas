import { errorMessage } from "@/core/http/ApiError";
import { FeedbackViewModel, type FeedbackState } from "@/core/mvvm/FeedbackViewModel";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { membershipApi } from "@/features/memberships/data/MembershipApi";
import type { MembershipSummary } from "@/features/memberships/model/types";
import { planApi } from "@/features/plans/data/PlanApi";
import type { PlanSummary } from "@/features/plans/model/types";

export interface MembershipsState extends FeedbackState {
  items: MembershipSummary[];
  loading: boolean;
  page: number;
  size: number;
  totalElements: number;
  search: string;
  statusFilter: string;
  planFilter: string;
  branchFilter: string;
  studentFilter: string;
  endingFrom: string;
  endingTo: string;
  sort: string;
  direction: "asc" | "desc";
  plans: PlanSummary[];
  branches: BranchSummary[];
}

const initialState: MembershipsState = {
  items: [],
  loading: true,
  page: 0,
  size: 20,
  totalElements: 0,
  search: "",
  statusFilter: "",
  planFilter: "",
  branchFilter: "",
  studentFilter: "",
  endingFrom: "",
  endingTo: "",
  sort: "createdAt",
  direction: "desc",
  plans: [],
  branches: [],
  error: null,
  success: null,
  saving: false,
};

export type MembershipFilterKey =
  | "search"
  | "statusFilter"
  | "planFilter"
  | "branchFilter"
  | "endingFrom"
  | "endingTo";

export class MembershipsViewModel extends FeedbackViewModel<MembershipsState> {
  constructor() {
    super(initialState);
  }

  /** Pre-filters by student when opened from the student page (?studentId=). */
  init(studentId: string | null): void {
    if (studentId) this.setState({ studentFilter: studentId });
    void this.load(0);
    void this.loadOptions();
  }

  async load(page = this.state.page): Promise<void> {
    this.setState({ loading: true, error: null, page });
    try {
      const result = await membershipApi.list({
        search: this.state.search,
        status: this.state.statusFilter,
        planId: this.state.planFilter,
        branchId: this.state.branchFilter,
        studentId: this.state.studentFilter,
        endingFrom: this.state.endingFrom,
        endingTo: this.state.endingTo,
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

  private async loadOptions(): Promise<void> {
    const [plans, branches] = await Promise.allSettled([
      planApi.list({ page: 0, size: 100, sort: "name", direction: "asc" }),
      branchApi.list({ page: 0, size: 100 }),
    ]);
    if (plans.status === "fulfilled") this.setState({ plans: plans.value.content });
    if (branches.status === "fulfilled") this.setState({ branches: branches.value.content });
  }

  setFilter = (key: MembershipFilterKey, value: string) =>
    this.setState({ [key]: value } as Partial<MembershipsState>);

  clearStudentFilter = () => {
    this.setState({ studentFilter: "" });
    void this.load(0);
  };

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
}
