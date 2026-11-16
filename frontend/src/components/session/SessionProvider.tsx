"use client";

import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { authRepository } from "@/features/auth/data/AuthRepository";
import { sessionManager } from "@/core/session/SessionManager";

const RENEW_THRESHOLD_MS = 60_000;
const CHECK_INTERVAL_MS = 15_000;

/**
 * Bootstraps the session from the HttpOnly refresh cookie and renews the
 * access token shortly before it expires.
 */
export function SessionProvider({ children }: { children: React.ReactNode }) {
  const router = useRouter();

  useEffect(() => {
    void authRepository.bootstrap();

    const interval = window.setInterval(() => {
      const session = sessionManager.getSnapshot();
      if (session.status !== "authenticated" || !session.expiresAt) {
        return;
      }
      if (session.expiresAt - Date.now() < RENEW_THRESHOLD_MS) {
        void authRepository.renew().catch(() => {
          sessionManager.clear();
          router.replace("/login");
        });
      }
    }, CHECK_INTERVAL_MS);

    return () => window.clearInterval(interval);
  }, [router]);

  return <>{children}</>;
}
