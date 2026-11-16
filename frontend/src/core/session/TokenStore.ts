/** Access token kept only in memory (never in localStorage) to limit XSS impact. */
class TokenStore {
  private token: string | null = null;

  get(): string | null {
    return this.token;
  }

  set(token: string | null): void {
    this.token = token;
  }

  clear(): void {
    this.token = null;
  }
}

export const tokenStore = new TokenStore();
