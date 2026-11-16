package com.gymsaas.shared.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MoneyTest {

    @Test
    void normalizesToTwoDecimals() {
        assertEquals(new BigDecimal("10.00"), Money.normalize(new BigDecimal("10")));
        assertEquals(new BigDecimal("10.50"), Money.normalize(new BigDecimal("10.5")));
        assertEquals(new BigDecimal("10.50"), Money.normalize(new BigDecimal("10.500")));
        assertEquals(Money.zero(), Money.normalizeOrZero(null));
    }

    @Test
    void rejectsInvalidInputInsteadOfSilentlyRounding() {
        assertThrows(IllegalArgumentException.class, () -> Money.normalize(new BigDecimal("10.005")));
        assertThrows(IllegalArgumentException.class, () -> Money.normalize(new BigDecimal("-0.01")));
        assertThrows(IllegalArgumentException.class, () -> Money.normalize(new BigDecimal("10000000000.00")));
    }

    @Test
    void roundsCalculatedValuesWithHalfEven() {
        assertEquals(new BigDecimal("2.34"), Money.round(new BigDecimal("2.345")));
        assertEquals(new BigDecimal("2.36"), Money.round(new BigDecimal("2.355")));
        assertEquals(new BigDecimal("33.33"),
                Money.round(new BigDecimal("100").divide(new BigDecimal("3"), 10, RoundingMode.HALF_EVEN)));
    }

    @Test
    void currencyDefaultsToBrlAndKeepsIsoCode() {
        assertEquals("BRL", Money.currency(null));
        assertEquals("BRL", Money.currency(" brl "));
        assertEquals("USD", Money.currency("usd"));
        assertThrows(IllegalArgumentException.class, () -> Money.currency("XYZW"));
    }
}
