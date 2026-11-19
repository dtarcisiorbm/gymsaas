export function onlyDigits(value: string): string {
  return value.replace(/\D/g, "");
}

export function isValidDocument(value: string): boolean {
  const digits = onlyDigits(value);
  return digits.length === 11 || digits.length === 14;
}

export function isValidEmail(value: string): boolean {
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value.trim());
}

export function required(value: string): boolean {
  return value.trim().length > 0;
}

/** Accepts "123", "123.4", "123.45" and the pt-BR comma form "123,45". */
export function normalizeMoney(value: string): string | null {
  const text = value.trim().replace(/\s/g, "").replace(",", ".");
  return /^\d{1,10}(\.\d{1,2})?$/.test(text) ? text : null;
}
