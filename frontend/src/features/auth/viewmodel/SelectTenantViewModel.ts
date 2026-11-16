import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { sessionManager } from "@/core/session/SessionManager";
import { authRepository } from "@/features/auth/data/AuthRepository";
import type { TenantView } from "@/features/auth/model/types";

export interface SelectTenantState {
  tenants: TenantView[];
  loading: boolean;
  error: string | null;
  selectingId: string | null;
}

export class SelectTenantViewModel extends ViewModel<SelectTenantState> {
  constructor() {
    super({ tenants: [], loading: true, error: null, selectingId: null });
  }

  load = async (): Promise<void> => {
    this.setState({ loading: true, error: null });
    try {
      const tenants = await authRepository.tenants();
      this.setState({ tenants, loading: false });
    } catch (error) {
      // Fall back to the tenants carried by the current session when offline.
      const cached = sessionManager.getSnapshot().tenants;
      this.setState({
        tenants: cached,
        loading: false,
        error: cached.length === 0 ? errorMessage(error) : null,
      });
    }
  };

  select = async (tenantId: string): Promise<boolean> => {
    this.setState({ selectingId: tenantId, error: null });
    try {
      await authRepository.selectTenant(tenantId);
      return true;
    } catch (error) {
      this.setState({ error: errorMessage(error) });
      return false;
    } finally {
      this.setState({ selectingId: null });
    }
  };
}
