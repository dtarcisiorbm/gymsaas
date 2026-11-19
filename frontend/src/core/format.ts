export function formatDocument(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const digits = value.replace(/\D/g, "");
  if (digits.length === 11) {
    return `${digits.slice(0, 3)}.${digits.slice(3, 6)}.${digits.slice(6, 9)}-${digits.slice(9)}`;
  }
  if (digits.length === 14) {
    return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`;
  }
  return value;
}

export function formatDateTime(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return date.toLocaleString("pt-BR", { dateStyle: "short", timeStyle: "short" });
}

export function formatDate(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "—";
  }
  return date.toLocaleDateString("pt-BR");
}

/**
 * Formats a LocalDate ("YYYY-MM-DD") without going through Date parsing,
 * which would shift the day in negative UTC offsets.
 */
export function formatLocalDate(value: string | null | undefined): string {
  if (!value) {
    return "—";
  }
  const match = /^(\d{4})-(\d{2})-(\d{2})/.exec(value);
  return match ? `${match[3]}/${match[2]}/${match[1]}` : value;
}

/** Formats a decimal string sent by the API (money is never a float on the wire). */
export function formatMoney(value: string | null | undefined, currency = "BRL"): string {
  if (value === null || value === undefined || value === "") {
    return "—";
  }
  const amount = Number(value);
  if (Number.isNaN(amount)) {
    return value;
  }
  try {
    return amount.toLocaleString("pt-BR", { style: "currency", currency });
  } catch {
    return `${currency} ${value}`;
  }
}

/** Today's date as "YYYY-MM-DD" in the browser's local timezone. */
export function todayIso(): string {
  const now = new Date();
  const month = String(now.getMonth() + 1).padStart(2, "0");
  const day = String(now.getDate()).padStart(2, "0");
  return `${now.getFullYear()}-${month}-${day}`;
}
