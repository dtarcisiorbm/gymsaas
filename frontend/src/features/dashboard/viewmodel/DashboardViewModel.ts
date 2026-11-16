import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { authRepository } from "@/features/auth/data/AuthRepository";
import type { MeResponse, RoleView, TenantDetails, UserSummary } from "@/features/auth/model/types";
import { dashboardApi } from "@/features/dashboard/data/DashboardApi";

export interface DashboardState {
  loading: boolean;
  error: string | null;
  me: MeResponse | null;
  tenant: TenantDetails | null;
  users: UserSummary[];
  userTotal: number;
  roles: RoleView[];
}

export class DashboardViewModel extends ViewModel<DashboardState> {
  constructor() {
    super({
      loading: true,
      error: null,
      me: null,
      tenant: null,
      users: [],
      userTotal: 0,
      roles: [],
    });
  }

  load = async (): Promise<void> => {
    this.setState({ loading: true, error: null });
    try {
      const me = await authRepository.me();
      const permissions = new Set(me.permissions);
      const canReadUsers = permissions.has("users:read") || permissions.has("user:read");
      const canReadRoles = permissions.has("role:read");

      const tenant = me.tenant ? await dashboardApi.currentTenant().catch(() => null) : null;
      const [usersPage, roles] = await Promise.all([
        canReadUsers ? dashboardApi.users(0, 5).catch(() => null) : Promise.resolve(null),
        canReadRoles ? dashboardApi.roles().catch(() => []) : Promise.resolve([]),
      ]);

      this.setState({
        me,
        tenant,
        users: usersPage?.content ?? [],
        userTotal: usersPage?.totalElements ?? 0,
        roles,
        loading: false,
      });
    } catch (error) {
      this.setState({ error: errorMessage(error), loading: false });
    }
  };

  renew = async (): Promise<boolean> => {
    try {
      await authRepository.renew();
      return true;
    } catch (error) {
      this.setState({ error: errorMessage(error) });
      return false;
    }
  };

  logout = async (): Promise<void> => {
    await authRepository.logout();
  };
}
