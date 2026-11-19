"use client";

import { Field } from "@/components/ui/Field";
import { inputClass } from "@/components/ui/formStyles";
import type { FormErrors } from "@/features/plans/model/validation";
import {
  BILLING_LABELS,
  BILLING_PERIODS,
  DURATION_LABELS,
  DURATION_TYPES,
  type PlanInput,
  type PriceInput,
} from "@/features/plans/model/types";

export function PriceFields({
  value,
  errors,
  prefix = "",
  onChange,
  showValidFrom = true,
}: {
  value: PriceInput;
  errors: FormErrors;
  prefix?: string;
  onChange: (patch: Partial<PriceInput>) => void;
  showValidFrom?: boolean;
}) {
  const id = (name: string) => `price-${prefix.replace(".", "")}${name}`;
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <Field id={id("amount")} label="Valor (R$) *" error={errors[`${prefix}amount`]}>
        <input
          id={id("amount")}
          className={inputClass}
          inputMode="decimal"
          placeholder="149,90"
          value={value.amount}
          onChange={(e) => onChange({ amount: e.target.value })}
        />
      </Field>
      <Field id={id("setupFee")} label="Taxa de adesão (R$)" error={errors[`${prefix}setupFee`]}>
        <input
          id={id("setupFee")}
          className={inputClass}
          inputMode="decimal"
          placeholder="0,00"
          value={value.setupFee}
          onChange={(e) => onChange({ setupFee: e.target.value })}
        />
      </Field>
      <Field id={id("billingPeriod")} label="Cobrança" error={errors[`${prefix}billingPeriod`]}>
        <select
          id={id("billingPeriod")}
          className={inputClass}
          value={value.billingPeriod}
          onChange={(e) => onChange({ billingPeriod: e.target.value as PriceInput["billingPeriod"] })}
        >
          {BILLING_PERIODS.map((p) => (
            <option key={p} value={p}>{BILLING_LABELS[p]}</option>
          ))}
        </select>
      </Field>
      <Field
        id={id("billingInterval")}
        label="Intervalo"
        error={errors[`${prefix}billingInterval`]}
        hint="Ex.: 2 = a cada 2 períodos."
      >
        <input
          id={id("billingInterval")}
          className={inputClass}
          type="number"
          min={1}
          max={36}
          value={value.billingInterval}
          onChange={(e) => onChange({ billingInterval: e.target.value })}
        />
      </Field>
      {showValidFrom ? (
        <Field id={id("validFrom")} label="Vigente a partir de" hint="Vazio = hoje. Nunca retroativo.">
          <input
            id={id("validFrom")}
            className={inputClass}
            type="date"
            value={value.validFrom}
            onChange={(e) => onChange({ validFrom: e.target.value })}
          />
        </Field>
      ) : null}
      <Field id={id("notes")} label="Observações">
        <input
          id={id("notes")}
          className={inputClass}
          value={value.notes}
          onChange={(e) => onChange({ notes: e.target.value })}
        />
      </Field>
    </div>
  );
}

export function PlanFormFields({
  value,
  errors,
  creating,
  onChange,
  onPriceChange,
}: {
  value: PlanInput;
  errors: FormErrors;
  creating: boolean;
  onChange: (patch: Partial<PlanInput>) => void;
  onPriceChange?: (patch: Partial<PriceInput>) => void;
}) {
  const custom = value.durationType === "CUSTOM_DAYS" || value.durationType === "CUSTOM_MONTHS";
  return (
    <div className="space-y-5">
      <div className="grid gap-3 sm:grid-cols-3">
        <Field id="plan-code" label="Código *" error={errors.code} hint={creating ? undefined : "Não pode ser alterado."}>
          <input
            id="plan-code"
            className={inputClass}
            value={value.code}
            disabled={!creating}
            placeholder="MENSAL-BASICO"
            onChange={(e) => onChange({ code: e.target.value })}
          />
        </Field>
        <Field id="plan-name" label="Nome *" error={errors.name} className="sm:col-span-2">
          <input
            id="plan-name"
            className={inputClass}
            value={value.name}
            onChange={(e) => onChange({ name: e.target.value })}
          />
        </Field>
      </div>
      <Field id="plan-description" label="Descrição">
        <textarea
          id="plan-description"
          className={inputClass}
          rows={2}
          value={value.description}
          onChange={(e) => onChange({ description: e.target.value })}
        />
      </Field>

      <fieldset className="space-y-3">
        <legend className="text-sm font-semibold text-white">Duração do contrato</legend>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field id="plan-duration-type" label="Tipo">
            <select
              id="plan-duration-type"
              className={inputClass}
              value={value.durationType}
              onChange={(e) => onChange({ durationType: e.target.value as PlanInput["durationType"] })}
            >
              {DURATION_TYPES.map((t) => (
                <option key={t} value={t}>{DURATION_LABELS[t]}</option>
              ))}
            </select>
          </Field>
          {custom ? (
            <Field
              id="plan-duration-value"
              label={value.durationType === "CUSTOM_DAYS" ? "Quantidade de dias *" : "Quantidade de meses *"}
              error={errors.durationValue}
            >
              <input
                id="plan-duration-value"
                className={inputClass}
                type="number"
                min={1}
                value={value.durationValue}
                onChange={(e) => onChange({ durationValue: e.target.value })}
              />
            </Field>
          ) : null}
        </div>
        {!creating ? (
          <p className="text-xs text-slate-500">Alterações não afetam matrículas existentes (condições ficam congeladas no contrato).</p>
        ) : null}
      </fieldset>

      <fieldset className="space-y-3">
        <legend className="text-sm font-semibold text-white">Política de congelamento</legend>
        <p className="text-xs text-slate-500">Deixe em branco para não limitar.</p>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field id="plan-max-freezes" label="Máx. congelamentos" error={errors.maxFreezes}>
            <input id="plan-max-freezes" className={inputClass} type="number" min={0} value={value.maxFreezes}
              onChange={(e) => onChange({ maxFreezes: e.target.value })} />
          </Field>
          <Field id="plan-max-freeze-days" label="Máx. dias (total)" error={errors.maxFreezeDays}>
            <input id="plan-max-freeze-days" className={inputClass} type="number" min={1} value={value.maxFreezeDays}
              onChange={(e) => onChange({ maxFreezeDays: e.target.value })} />
          </Field>
          <Field id="plan-min-freeze-days" label="Mín. dias por congelamento" error={errors.minFreezeDays}>
            <input id="plan-min-freeze-days" className={inputClass} type="number" min={1} value={value.minFreezeDays}
              onChange={(e) => onChange({ minFreezeDays: e.target.value })} />
          </Field>
        </div>
      </fieldset>

      <Field id="plan-terms" label="Cláusulas específicas do contrato" hint="Entram no texto do contrato gerado para cada matrícula.">
        <textarea
          id="plan-terms"
          className={inputClass}
          rows={4}
          value={value.contractTerms}
          onChange={(e) => onChange({ contractTerms: e.target.value })}
        />
      </Field>

      {creating && onPriceChange ? (
        <fieldset className="space-y-3">
          <legend className="flex items-center gap-2 text-sm font-semibold text-white">
            <input
              id="plan-with-price"
              type="checkbox"
              checked={value.withInitialPrice}
              onChange={(e) => onChange({ withInitialPrice: e.target.checked })}
              className="h-4 w-4 accent-lime-500"
            />
            <label htmlFor="plan-with-price">Definir preço inicial</label>
          </legend>
          {value.withInitialPrice ? (
            <PriceFields value={value.initialPrice} errors={errors} prefix="price." onChange={onPriceChange} />
          ) : (
            <p className="text-xs text-slate-500">Sem preço vigente o plano não aceita matrículas.</p>
          )}
        </fieldset>
      ) : null}
    </div>
  );
}
