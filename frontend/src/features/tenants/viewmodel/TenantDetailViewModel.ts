import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import type { BranchSummary } from "@/features/branches/model/types";
import { tenantApi } from "@/features/tenants/data/TenantApi";
import type { TenantDetail } from "@/features/tenants/model/types";

export interface TenantDetailState {
  loading: boolean;
  error: string | null;
  tenant: TenantDetail | null;
  branches: BranchSummary[];
  branchesPage: number;
  branchesSize: number;
  branchesTotal: number;
}

export class TenantDetailViewModel extends ViewModel<TenantDetailState> {
  constructor() {
    super({
      loading: true,
      error: null,
      tenant: null,
      branches: [],
      branchesPage: 0,
      branchesSize: 10,
      branchesTotal: 0,
    });
  }

  load = async (tenantId: string): Promise<void> => {
    this.setState({ loading: true, error: null });
    try {
      const [tenant, branches] = await Promise.all([
        tenantApi.detail(tenantId),
        tenantApi.branches(tenantId, {
          page: this.getSnapshot().branchesPage,
          size: this.getSnapshot().branchesSize,
        }),
      ]);
      this.setState({
        tenant,
        branches: branches.content,
        branchesTotal: branches.totalElements,
        loading: false,
      });
    } catch (error) {
      this.setState({ error: errorMessage(error), loading: false });
    }
  };

  changeBranchesPage = async (tenantId: string, delta: number): Promise<void> => {
    const { branchesPage, branchesSize, branchesTotal } = this.getSnapshot();
    const next = branchesPage + delta;
    if (next < 0 || next * branchesSize >= branchesTotal) {
      return;
    }
    this.setState({ branchesPage: next });
    try {
      const branches = await tenantApi.branches(tenantId, { page: next, size: branchesSize });
      this.setState({
        branches: branches.content,
        branchesTotal: branches.totalElements,
      });
    } catch (error) {
      this.setState({ error: errorMessage(error) });
    }
  };
}
