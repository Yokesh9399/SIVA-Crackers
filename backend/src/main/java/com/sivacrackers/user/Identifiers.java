package com.sivacrackers.user;

import java.util.Locale;

/** Normalises login identifiers so "Asha@Example.com " and "asha@example.com" are the same account. */
public final class Identifiers {

    private Identifiers() {
    }

    public static String email(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    /** 10-digit Indian mobile; accepts spaces, dashes and a leading +91 / 91 / 0. Null if blank. */
    public static String phone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        return digits;
    }

    public static boolean isEmail(String identifier) {
        return identifier != null && identifier.contains("@");
    }
}
