package com.gymsaas.shared.domain;

import com.gymsaas.branch.domain.BranchStatus;
import com.gymsaas.tenant.domain.TenantStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BrDocumentsTest {

    @Test
    void stripsFormattingFromDocumentPhoneAndZipCode() {
        assertEquals("12345678000199", BrDocuments.document("12.345.678/0001-99"));
        assertEquals("11999990000", BrDocuments.phone("(11) 99999-0000"));
        assertEquals("01001000", BrDocuments.zipCode("01001-000"));
        assertEquals("12345678901", BrDocuments.document("123.456.789-01"));
    }

    @Test
    void acceptsOnlyElevenOrFourteenDigitDocuments() {
        assertTrue(BrDocuments.validDocument("123.456.789-01"));
        assertTrue(BrDocuments.validDocument("12.345.678/0001-99"));
        assertFalse(BrDocuments.validDocument("123"));
        assertFalse(BrDocuments.validDocument("123456789012"));
        assertFalse(BrDocuments.validDocument(null));
    }

    @Test
    void validatesBrazilianZipCode() {
        assertTrue(BrDocuments.validZipCode("01001-000"));
        assertFalse(BrDocuments.validZipCode("0100100"));
        assertFalse(BrDocuments.validZipCode(null));
    }

    @Test
    void normalizesBranchCodes() {
        assertEquals("CENTRO-1", BrDocuments.branchCode(" centro-1 "));
        assertEquals("UNIDADE_A", BrDocuments.branchCode("unidade_a"));
        assertEquals("ABC123", BrDocuments.branchCode("abc 123"));
        assertEquals(null, BrDocuments.branchCode(null));
    }

    @Test
    void parsesOnlyKnownStatuses() {
        assertEquals(TenantStatus.ACTIVE, TenantStatus.parse(" active ").orElseThrow());
        assertEquals(TenantStatus.SUSPENDED, TenantStatus.parse("SUSPENDED").orElseThrow());
        assertTrue(TenantStatus.parse("CANCELLED").isEmpty());
        assertTrue(TenantStatus.parse(null).isEmpty());

        assertTrue(BranchStatus.valid("active"));
        assertFalse(BranchStatus.valid("SUSPENDED"));
        assertFalse(BranchStatus.valid(null));
    }
}
