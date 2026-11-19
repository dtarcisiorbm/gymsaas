import { ApiError } from "@/core/http/ApiError";
import { sessionManager } from "@/core/session/SessionManager";
import { refreshSession } from "@/core/session/refreshSession";
import { authApi } from "@/features/auth/data/AuthApi";
import type { MeResponse, TenantView } from "@/features/auth/model/types";

/**
 * Application-level auth repository: keeps the session store as the single
 * source of truth and isolates the UI from HTTP details.
 */
class AuthRepository {
  /** Rehydrates the in-memory access token from the HttpOnly refresh cookie. */
  async bootstrap(): Promise<void> {
    const restored = await refreshSession();
    // The refresh response carries no permissions; without /me a full page
    // reload would leave the nav and permission-gated actions empty.
    if (restored && sessionManager.getSnapshot().activeTenantId) {
      try {
        await this.me();
      } catch {
        // Permissions stay empty; the backend still enforces every request.
      }
    }
  }

  async login(email: string, password: string): Promise<{ needsTenantSelection: boolean }> {
    const session = await authApi.login(email, password);
    sessionManager.setSession(session);
    return { needsTenantSelection: !session.activeTenantId };
  }

  async selectTenant(tenantId: string): Promise<void> {
    const selection = await authApi.selectTenant(tenantId);
    sessionManager.setSelection(selection);
  }

  async logout(): Promise<void> {
    try {
      await authApi.logout();
    } finally {
      sessionManager.clear();
    }
  }

  async me(): Promise<MeResponse> {
    const me = await authApi.me();
    sessionManager.setMe(me);
    return me;
  }

  async tenants(): Promise<TenantView[]> {
    return authApi.tenants();
  }

  /** Proactive renewal used before the access token expires. */
  async renew(): Promise<void> {
    const renewed = await refreshSession();
    if (!renewed) {
      throw new ApiError(401, "SESSION_EXPIRED", "Sessão expirada. Entre novamente.");
    }
  }
}

export const authRepository = new AuthRepository();
