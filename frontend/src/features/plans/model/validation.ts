import { normalizeMoney } from "@/core/validation";
import type { FeatureInput, PlanInput, PriceInput } from "@/features/plans/model/types";
import { FEATURES_WITH_INT, SINGLETON_FEATURES } from "@/features/plans/model/types";

export type FormErrors = Record<string, string>;

function positiveInt(value: string): boolean {
  return /^\d+$/.test(value.trim()) && Number(value) > 0;
}

function optionalNonNegativeInt(value: string): boolean {
  return !value.trim() || /^\d+$/.test(value.trim());
}

/** Client-side checks only for fast feedback; the backend remains authoritative. */
export function validatePrice(input: PriceInput, prefix = ""): FormErrors {
  const errors: FormErrors = {};
  if (!normalizeMoney(input.amount)) errors[`${prefix}amount`] = "Informe um valor válido (até 2 casas decimais).";
  if (input.setupFee.trim() && !normalizeMoney(input.setupFee)) {
    errors[`${prefix}setupFee`] = "Taxa de adesão inválida.";
  }
  const interval = Number(input.billingInterval);
  if (!positiveInt(input.billingInterval) || interval > 36) {
    errors[`${prefix}billingInterval`] = "Intervalo entre 1 e 36.";
  }
  return errors;
}

export function validatePlan(input: PlanInput, creating: boolean): FormErrors {
  const errors: FormErrors = {};
  if (creating) {
    if (!input.code.trim()) errors.code = "Informe o código.";
    else if (!/^[A-Za-z0-9_-]{1,30}$/.test(input.code.trim())) {
      errors.code = "Use até 30 letras, números, - ou _.";
    }
  }
  if (!input.name.trim()) errors.name = "Informe o nome do plano.";
  if (input.durationType === "CUSTOM_DAYS" || input.durationType === "CUSTOM_MONTHS") {
    if (!positiveInt(input.durationValue)) errors.durationValue = "Informe a duração.";
  }
  if (input.maxFreezeDays.trim() && !positiveInt(input.maxFreezeDays)) errors.maxFreezeDays = "Número positivo.";
  if (input.minFreezeDays.trim() && !positiveInt(input.minFreezeDays)) errors.minFreezeDays = "Número positivo.";
  if (!optionalNonNegativeInt(input.maxFreezes)) errors.maxFreezes = "Número inteiro.";
  if (
    !errors.maxFreezeDays &&
    !errors.minFreezeDays &&
    input.maxFreezeDays.trim() &&
    input.minFreezeDays.trim() &&
    Number(input.minFreezeDays) > Number(input.maxFreezeDays)
  ) {
    errors.minFreezeDays = "O mínimo não pode exceder o máximo.";
  }
  if (creating && input.withInitialPrice) {
    Object.assign(errors, validatePrice(input.initialPrice, "price."));
    if (input.durationType === "CUSTOM_DAYS" && input.initialPrice.billingPeriod !== "ONE_TIME") {
      errors["price.billingPeriod"] = "Planos em dias só aceitam pagamento único.";
    }
  }
  return errors;
}

export function validateFeatures(features: FeatureInput[]): string | null {
  for (const type of SINGLETON_FEATURES) {
    if (features.filter((f) => f.type === type).length > 1) {
      return "Alguns benefícios só podem aparecer uma vez por plano.";
    }
  }
  for (const [index, feature] of features.entries()) {
    const n = index + 1;
    if (feature.type === "BRANCH_ACCESS" && !feature.branchId) return `Benefício ${n}: selecione a unidade.`;
    if (feature.type === "TIME_WINDOW") {
      if (!feature.startTime || !feature.endTime || feature.startTime >= feature.endTime) {
        return `Benefício ${n}: o início deve ser antes do fim.`;
      }
      if (!feature.daysOfWeek) return `Benefício ${n}: selecione ao menos um dia.`;
    }
    if (FEATURES_WITH_INT.includes(feature.type) && !positiveInt(feature.intValue)) {
      return `Benefício ${n}: informe um número positivo.`;
    }
    if (feature.type === "CUSTOM" && !feature.label.trim()) return `Benefício ${n}: informe o rótulo.`;
  }
  return null;
}
