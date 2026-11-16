import { API_URL } from "@/core/config";
import { sessionManager } from "@/core/session/SessionManager";
import type { SessionResponse } from "@/features/auth/model/types";

let refreshInFlight: Promise<boolean> | null = null;

/**
 * Single-flight refresh-token rotation. Refresh tokens are rotated on every
 * use with reuse detection, so parallel 401s MUST share one network call —
 * otherwise the second rotation would be treated as a replay attack.
 */
export function refreshSession(): Promise<boolean> {
  if (!refreshInFlight) {
    refreshInFlight = performRefresh().finally(() => {
      refreshInFlight = null;
    });
  }
  return refreshInFlight;
}

async function performRefresh(): Promise<boolean> {
  try {
    const response = await fetch(`${API_URL}/api/auth/refresh`, {
      method: "POST",
      credentials: "include",
      headers: { Accept: "application/json" },
      cache: "no-store",
    });
    if (!response.ok) {
      sessionManager.clear();
      return false;
    }
    const session = (await response.json()) as SessionResponse;
    sessionManager.setSession(session);
    return true;
  } catch {
    sessionManager.clear();
    return false;
  }
}
