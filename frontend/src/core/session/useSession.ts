"use client";

import { useSyncExternalStore } from "react";
import { sessionManager, type SessionSnapshot } from "@/core/session/SessionManager";

export function useSession(): SessionSnapshot {
  return useSyncExternalStore(
    sessionManager.subscribe,
    sessionManager.getSnapshot,
    sessionManager.getSnapshot,
  );
}
