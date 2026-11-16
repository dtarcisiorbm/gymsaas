import { errorMessage } from "@/core/http/ApiError";
import { ViewModel } from "@/core/mvvm/ViewModel";
import { sessionManager } from "@/core/session/SessionManager";
import { authRepository } from "@/features/auth/data/AuthRepository";

export interface LoginState {
  email: string;
  password: string;
  submitting: boolean;
  error: string | null;
}

export class LoginViewModel extends ViewModel<LoginState> {
  constructor() {
    super({ email: "", password: "", submitting: false, error: null });
  }

  setEmail = (email: string): void => {
    this.setState({ email, error: null });
  };

  setPassword = (password: string): void => {
    this.setState({ password, error: null });
  };

  submit = async (): Promise<"dashboard" | "select-tenant" | "admin/tenants" | null> => {
    const { email, password } = this.getSnapshot();
    if (!email.trim() || !password) {
      this.setState({ error: "Informe e-mail e senha." });
      return null;
    }
    this.setState({ submitting: true, error: null });
    try {
      const result = await authRepository.login(email.trim(), password);
      const session = sessionManager.getSnapshot();
      if (session.platformAdmin && !session.activeTenantId) {
        return "admin/tenants";
      }
      return result.needsTenantSelection ? "select-tenant" : "dashboard";
    } catch (error) {
      this.setState({ error: errorMessage(error) });
      return null;
    } finally {
      this.setState({ submitting: false });
    }
  };
}
