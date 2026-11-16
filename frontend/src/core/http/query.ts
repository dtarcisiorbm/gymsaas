export type QueryValue = string | number | boolean | null | undefined;

/** Builds a query string, skipping empty values and URL-encoding the rest. */
export function toQuery(params: Record<string, QueryValue>): string {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === null || value === undefined) {
      return;
    }
    const text = String(value).trim();
    if (!text) {
      return;
    }
    search.set(key, text);
  });
  const query = search.toString();
  return query ? `?${query}` : "";
}
