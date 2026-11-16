import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { branchApi } from "@/features/branches/data/BranchApi";
import type { BranchSummary } from "@/features/branches/model/types";
import { studentApi } from "@/features/students/data/StudentApi";
import type { StudentStatus, StudentSummary } from "@/features/students/model/types";

export interface StudentsState {
  items: StudentSummary[];
  loading: boolean;
  page: number;
  size: number;
  totalElements: number;
  search: string;
  document: string;
  statusFilter: string;
  branchFilter: string;
  sort: string;
  direction: "asc" | "desc";
  branches: BranchSummary[];
  error: string | null;
  success: string | null;
  busyId: string | null;
}

const initialState: StudentsState = {
  items: [],
  loading: true,
  page: 0,
  size: 20,
  totalElements: 0,
  search: "",
  document: "",
  statusFilter: "",
  branchFilter: "",
  sort: "name",
  direction: "asc",
  branches: [],
  error: null,
  success: null,
  busyId: null,
};

export class StudentsViewModel extends ViewModel<StudentsState> {
  private feedbackTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    super(initialState);
  }

  async load(page = this.state.page): Promise<void> {
    this.setState({ loading: true, error: null, page });
    try {
      const result = await studentApi.list({
        search: this.state.search,
        document: this.state.document,
        status: this.state.statusFilter,
        branchId: this.state.branchFilter,
        page,
        size: this.state.size,
        sort: this.state.sort,
        direction: this.state.direction,
      });
      this.setState({
        items: result.content,
        totalElements: result.totalElements,
        page: result.page,
        loading: false,
      });
    } catch (error) {
      this.setState({ loading: false, error: errorMessage(error) });
    }
  }

  async loadBranches(): Promise<void> {
    try {
      const result = await branchApi.list({ page: 0, size: 100 });
      this.setState({ branches: result.content });
    } catch {
      // Branch filter is optional; ignore load failures.
    }
  }

  setSearch = (search: string) => this.setState({ search });
  setDocument = (document: string) => this.setState({ document });
  setStatusFilter = (statusFilter: string) => this.setState({ statusFilter, page: 0 });
  setBranchFilter = (branchFilter: string) => this.setState({ branchFilter, page: 0 });

  applyFilters = () => void this.load(0);

  changePage = (delta: number) => {
    const next = this.state.page + delta;
    if (next < 0) return;
    const maxPage = Math.max(0, Math.ceil(this.state.totalElements / this.state.size) - 1);
    if (next > maxPage) return;
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

  async changeStatus(id: string, status: StudentStatus): Promise<void> {
    this.setState({ busyId: id, error: null });
    try {
      await studentApi.changeStatus(id, status);
      this.flashSuccess("Status do aluno atualizado.");
      await this.load(this.state.page);
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    } finally {
      this.setState({ busyId: null });
    }
  }

  async remove(id: string): Promise<void> {
    this.setState({ busyId: id, error: null });
    try {
      await studentApi.remove(id);
      this.flashSuccess("Aluno excluído com sucesso.");
      const nextPage =
        this.state.items.length === 1 && this.state.page > 0 ? this.state.page - 1 : this.state.page;
      await this.load(nextPage);
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    } finally {
      this.setState({ busyId: null });
    }
  }

  dismissFeedback = () => this.setState({ error: null, success: null });

  private flashSuccess(message: string) {
    this.setState({ success: message });
    if (this.feedbackTimer) clearTimeout(this.feedbackTimer);
    this.feedbackTimer = setTimeout(() => this.setState({ success: null }), 4000);
  }
}
