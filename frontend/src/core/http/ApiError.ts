export interface ApiErrorPayload {
  status?: number;
  code?: string;
  message?: string;
  details?: Record<string, string> | null;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly details: Record<string, string> | null;

  constructor(status: number, code: string, message: string, details?: Record<string, string> | null) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.details = details ?? null;
  }

  static network(): ApiError {
    return new ApiError(0, "NETWORK_ERROR", "Não foi possível conectar ao servidor. Verifique sua conexão.");
  }

  static from(status: number, payload: ApiErrorPayload | null): ApiError {
    const message =
      payload?.message ??
      (status === 401
        ? "Sessão expirada. Entre novamente."
        : "Não foi possível concluir a operação.");
    return new ApiError(status, payload?.code ?? "UNKNOWN_ERROR", message, payload?.details);
  }
}

export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Erro inesperado. Tente novamente.";
}
