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
