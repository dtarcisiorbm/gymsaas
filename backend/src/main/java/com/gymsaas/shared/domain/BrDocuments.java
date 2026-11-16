package com.gymsaas.shared.domain;

/** Normalization helpers shared by domain services and unit tests. */
public final class BrDocuments {

    private BrDocuments() {
    }

    public static String document(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    public static String phone(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    public static String zipCode(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\D", "");
    }

    public static String branchCode(String value) {
        if (value == null) {
            return null;
        }
        return value.trim().toUpperCase().replaceAll("[^A-Z0-9_-]", "");
    }

    public static boolean validDocument(String value) {
        String digits = document(value);
        return digits != null && (digits.length() == 11 || digits.length() == 14);
    }

    /** Validates a Brazilian CPF using the official check-digit algorithm. */
    public static boolean validCpf(String value) {
        String digits = document(value);
        if (digits == null || digits.length() != 11) {
            return false;
        }
        if (digits.chars().distinct().count() == 1) {
            return false;
        }
        int first = checkDigit(digits, 9, 10);
        if (first != digits.charAt(9) - '0') {
            return false;
        }
        int second = checkDigit(digits, 10, 11);
        return second == digits.charAt(10) - '0';
    }

    private static int checkDigit(String digits, int length, int weightStart) {
        int sum = 0;
        for (int index = 0; index < length; index++) {
            sum += (digits.charAt(index) - '0') * (weightStart - index);
        }
        int digit = 11 - (sum % 11);
        return digit >= 10 ? 0 : digit;
    }

    /** Returns the 11 digits of a valid CPF, or null when the value is invalid. */
    public static String normalizeCpf(String value) {
        return validCpf(value) ? document(value) : null;
    }

    /** Masks a CPF, keeping only the last two digits visible. */
    public static String maskCpf(String value) {
        String digits = document(value);
        if (digits == null || digits.length() != 11) {
            return null;
        }
        return "***.***.***-" + digits.substring(9);
    }

    public static boolean validZipCode(String value) {
        String digits = zipCode(value);
        return digits != null && digits.length() == 8;
    }
}
