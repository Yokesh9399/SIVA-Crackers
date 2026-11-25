package com.sivacrackers.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sivacrackers.common.exception.InvalidRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PasswordAndIdentifierTest {

    @Test
    @DisplayName("Password rules: 8–72 bytes, at least one letter and one digit")
    void passwordPolicy() {
        assertThat(PasswordPolicy.isValid("abcdefg1")).isTrue();
        assertThat(PasswordPolicy.isValid("abc1")).isFalse();             // too short
        assertThat(PasswordPolicy.isValid("abcdefgh")).isFalse();         // no digit
        assertThat(PasswordPolicy.isValid("12345678")).isFalse();         // no letter
        assertThat(PasswordPolicy.isValid("a1" + "x".repeat(71))).isFalse(); // 73 bytes, BCrypt limit is 72
        assertThat(PasswordPolicy.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("Rule violations name the field but never include the password")
    void policyMessage() {
        assertThatThrownBy(() -> PasswordPolicy.require("password", "short1"))
                .isInstanceOf(InvalidRequestException.class)
                .satisfies(e -> assertThat(((InvalidRequestException) e).getFieldErrors().getFirst().message())
                        .doesNotContain("short1"));
    }

    @Test
    @DisplayName("Emails are trimmed and lower-cased; phones normalised to 10 digits")
    void identifiers() {
        assertThat(Identifiers.email("  Asha@Example.TEST ")).isEqualTo("asha@example.test");
        assertThat(Identifiers.phone("+91 98765-43210")).isEqualTo("9876543210");
        assertThat(Identifiers.phone("09876543210")).isEqualTo("9876543210");
        assertThat(Identifiers.phone("9876543210")).isEqualTo("9876543210");
        assertThat(Identifiers.phone("  ")).isNull();
        assertThat(Identifiers.isEmail("a@b.c")).isTrue();
        assertThat(Identifiers.isEmail("9876543210")).isFalse();
    }
}
