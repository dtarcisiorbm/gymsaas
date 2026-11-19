package com.gymsaas.billing.domain;

/** INSTALLMENT and SETUP_FEE come from a membership; MANUAL is launched by an operator. */
public enum ChargeKind {
    INSTALLMENT,
    SETUP_FEE,
    MANUAL
}
