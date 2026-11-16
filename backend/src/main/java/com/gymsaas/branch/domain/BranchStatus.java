package com.gymsaas.branch.domain;

public enum BranchStatus {

    ACTIVE,
    INACTIVE;

    public static boolean valid(String value) {
        if (value == null) {
            return false;
        }
        try {
            valueOf(value.trim().toUpperCase());
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
