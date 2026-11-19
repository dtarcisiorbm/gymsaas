"use client";

import { useSession } from "@/core/session/useSession";

/**
 * Returns a predicate that is true when the session has any of the given
 * permission codes. Platform admins pass every check, matching AppNav. This
 * only hides UI; the backend enforces authorization on every request.
 */
export function usePermission(): (...codes: string[]) => boolean {
  const session = useSession();
  const permissions = new Set(session.permissions);
  return (...codes: string[]) => session.platformAdmin || codes.some((code) => permissions.has(code));
}
