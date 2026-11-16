package com.gymsaas.student.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StudentDomainTest {

    @Test
    void parsesStatusCaseInsensitively() {
        assertEquals(StudentStatus.ACTIVE, StudentStatus.parse("active"));
        assertEquals(StudentStatus.BLOCKED, StudentStatus.parse(" BLOCKED "));
    }

    @Test
    void rejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class, () -> StudentStatus.parse("CANCELLED"));
        assertThrows(IllegalArgumentException.class, () -> StudentStatus.parse(null));
    }

    @Test
    void allowsDocumentedTransitions() {
        assertTrue(StudentStatus.PENDING.canTransitionTo(StudentStatus.ACTIVE));
        assertTrue(StudentStatus.ACTIVE.canTransitionTo(StudentStatus.BLOCKED));
        assertTrue(StudentStatus.BLOCKED.canTransitionTo(StudentStatus.ACTIVE));
        assertTrue(StudentStatus.INACTIVE.canTransitionTo(StudentStatus.ARCHIVED));
        assertTrue(StudentStatus.ARCHIVED.canTransitionTo(StudentStatus.ACTIVE));
    }

    @Test
    void rejectsInvalidTransitions() {
        assertFalse(StudentStatus.ARCHIVED.canTransitionTo(StudentStatus.BLOCKED));
        assertFalse(StudentStatus.ARCHIVED.canTransitionTo(StudentStatus.INACTIVE));
        assertFalse(StudentStatus.ACTIVE.canTransitionTo(StudentStatus.PENDING));
        assertFalse(StudentStatus.ACTIVE.canTransitionTo(StudentStatus.ACTIVE));
    }
}
