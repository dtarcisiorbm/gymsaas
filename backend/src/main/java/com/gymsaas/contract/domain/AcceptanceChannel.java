package com.gymsaas.contract.domain;

/**
 * IN_PERSON: registered by an operator at the front desk (the only channel
 * exposed in this phase). ELECTRONIC is reserved for a future student portal.
 */
public enum AcceptanceChannel {
    IN_PERSON,
    ELECTRONIC
}
