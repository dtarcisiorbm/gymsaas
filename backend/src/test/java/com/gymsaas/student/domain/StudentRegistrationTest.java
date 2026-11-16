package com.gymsaas.student.domain;

import com.gymsaas.shared.domain.BrDocuments;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentRegistrationTest {

    @Test
    void acceptsValidCpfWithMask() {
        assertTrue(BrDocuments.validCpf("529.982.247-25"));
    }

    @Test
    void acceptsValidCpfWithDigitsOnly() {
        assertTrue(BrDocuments.validCpf("52998224725"));
    }

    @Test
    void rejectsRepeatedDigits() {
        assertFalse(BrDocuments.validCpf("111.111.111-11"));
        assertFalse(BrDocuments.validCpf("00000000000"));
    }

    @Test
    void rejectsInvalidCheckDigits() {
        assertFalse(BrDocuments.validCpf("529.982.247-24"));
        assertFalse(BrDocuments.validCpf("5299822472"));
    }

    @Test
    void rejectsNullAndBlank() {
        assertFalse(BrDocuments.validCpf(null));
        assertFalse(BrDocuments.validCpf("   "));
    }

    @Test
    void normalizesOnlyValidCpf() {
        assertEquals("52998224725", BrDocuments.normalizeCpf("529.982.247-25"));
        assertNull(BrDocuments.normalizeCpf("529.982.247-24"));
        assertNull(BrDocuments.normalizeCpf("123"));
    }

    @Test
    void masksCpfKeepingLastTwoDigits() {
        assertEquals("***.***.***-25", BrDocuments.maskCpf("52998224725"));
        assertEquals("***.***.***-25", BrDocuments.maskCpf("529.982.247-25"));
        assertNull(BrDocuments.maskCpf(null));
    }

    @Test
    void formatsRegistrationAsYearPlusFiveDigits() {
        assertEquals("202600001", StudentRegistrationGenerator.format(2026, 1));
        assertEquals("202600042", StudentRegistrationGenerator.format(2026, 42));
        assertEquals("202612345", StudentRegistrationGenerator.format(2026, 12345));
    }
}
