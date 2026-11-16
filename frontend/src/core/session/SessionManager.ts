import type { MeResponse, SessionResponse, TenantSelectionResponse, TenantView, UserView } from "@/features/auth/model/types";
import { tokenStore } from "@/core/session/TokenStore";

export type SessionStatus = "booting" | "anonymous" | "authenticated";

export interface SessionSnapshot {
  status: SessionStatus;
  accessToken: string | null;
  user: UserView | null;
  tenants: TenantView[];
  activeTenantId: string | null;
  expiresAt: number | null;
  roles: string[];
  permissions: string[];
  platformAdmin: boolean;
}

const initialState: SessionSnapshot = {
  status: "booting",
  accessToken: null,
  user: null,
  tenants: [],
  activeTenantId: null,
  expiresAt: null,
  roles: [],
  permissions: [],
  platformAdmin: false,
};

class SessionManager {
  private snapshot: SessionSnapshot = initialState;
  private readonly listeners = new Set<() => void>();

  subscribe = (listener: () => void): (() => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  getSnapshot = (): SessionSnapshot => this.snapshot;

  setSession(session: SessionResponse): void {
    tokenStore.set(session.accessToken);
    const activeTenant = session.tenants.find((tenant) => tenant.id === session.activeTenantId) ?? null;
    this.publish({
      status: "authenticated",
      accessToken: session.accessToken,
      user: session.user,
      tenants: session.tenants,
      activeTenantId: session.activeTenantId,
      expiresAt: Date.now() + session.expiresIn * 1000,
      roles: activeTenant?.roles ?? this.snapshot.roles,
      permissions: this.snapshot.permissions,
      platformAdmin: session.platformAdmin,
    });
  }

  setSelection(selection: TenantSelectionResponse): void {
    tokenStore.set(selection.accessToken);
    this.publish({
      ...this.snapshot,
      status: "authenticated",
      accessToken: selection.accessToken,
      activeTenantId: selection.tenant.id,
      tenants: this.snapshot.tenants.map((tenant) =>
        tenant.id === selection.tenant.id ? { ...tenant, ...selection.tenant } : tenant,
      ),
      expiresAt: Date.now() + selection.expiresIn * 1000,
      roles: selection.tenant.roles,
    });
  }

  setMe(me: MeResponse): void {
    this.publish({
      ...this.snapshot,
      roles: me.roles,
      permissions: me.permissions,
      platformAdmin: me.platformAdmin,
    });
  }

  clear(): void {
    tokenStore.clear();
    this.publish({ ...initialState, status: "anonymous" });
  }

  get activeTenant(): TenantView | null {
    const { tenants, activeTenantId } = this.snapshot;
    return tenants.find((tenant) => tenant.id === activeTenantId) ?? null;
  }

  private publish(next: SessionSnapshot): void {
    this.snapshot = next;
    this.listeners.forEach((listener) => listener());
  }
}

export const sessionManager = new SessionManager();
