package com.sivacrackers.user;

import com.sivacrackers.common.exception.InvalidRequestException;
import java.nio.charset.StandardCharsets;

/**
 * Password rules for every account: 8–72 characters (BCrypt uses at most 72 bytes),
 * at least one letter and one digit. The password itself is never included in any message.
 */
public final class PasswordPolicy {

    public static final String RULES = "must be 8–72 characters and contain at least one letter and one number";

    private PasswordPolicy() {
    }

    public static boolean isValid(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            return false;
        }
        boolean letter = password.chars().anyMatch(Character::isLetter);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        return letter && digit;
    }

    public static void require(String field, String password) {
        if (!isValid(password)) {
            throw new InvalidRequestException(field, RULES);
        }
    }
}
