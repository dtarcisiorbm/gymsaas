import { API_URL } from "@/core/config";
import { ApiError, type ApiErrorPayload } from "@/core/http/ApiError";
import { tokenStore } from "@/core/session/TokenStore";
import { refreshSession } from "@/core/session/refreshSession";

export interface RequestOptions {
  method?: "GET" | "POST" | "PUT" | "PATCH" | "DELETE";
  body?: unknown;
  /** Set to false for public endpoints (login/refresh/logout). */
  auth?: boolean;
  /** Prevents the 401 interceptor (used by login/logout). */
  skipRefresh?: boolean;
}

async function parseError(response: Response): Promise<ApiError> {
  try {
    const payload = (await response.json()) as ApiErrorPayload;
    return ApiError.from(response.status, payload);
  } catch {
    return ApiError.from(response.status, null);
  }
}

async function execute(path: string, options: RequestOptions): Promise<Response> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (options.body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  const token = tokenStore.get();
  if (token && options.auth !== false) {
    headers.Authorization = `Bearer ${token}`;
  }

  try {
    return await fetch(`${API_URL}${path}`, {
      method: options.method ?? "GET",
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      credentials: "include",
      cache: "no-store",
    });
  } catch {
    throw ApiError.network();
  }
}

export const apiClient = {
  async request<T>(path: string, options: RequestOptions = {}): Promise<T> {
    let response = await execute(path, options);

    if (response.status === 401 && options.auth !== false && !options.skipRefresh) {
      const refreshed = await refreshSession();
      if (refreshed) {
        response = await execute(path, options);
      }
    }

    if (!response.ok) {
      throw await parseError(response);
    }
    if (response.status === 204) {
      return undefined as T;
    }
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  },

  /** Authenticated binary download (e.g. contract PDF); the token never goes in the URL. */
  async download(path: string): Promise<Blob> {
    let response = await execute(path, { method: "GET" });
    if (response.status === 401) {
      const refreshed = await refreshSession();
      if (refreshed) {
        response = await execute(path, { method: "GET" });
      }
    }
    if (!response.ok) {
      throw await parseError(response);
    }
    return response.blob();
  },

  get<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "GET" });
  },

  post<T>(path: string, body?: unknown, options: RequestOptions = {}): Promise<T> {
    return this.request<T>(path, { ...options, method: "POST", body });
  },

  put<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, { method: "PUT", body });
  },

  patch<T>(path: string, body?: unknown): Promise<T> {
    return this.request<T>(path, { method: "PATCH", body });
  },

  delete<T>(path: string): Promise<T> {
    return this.request<T>(path, { method: "DELETE" });
  },
};
