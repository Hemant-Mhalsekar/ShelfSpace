package com.shelfspace.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Shelf2Read", "Brand1New", "Correct-Horse-9", "Reading4Life"})
    void acceptsStrongPasswords(String password) {
        assertThat(PasswordPolicy.problem(password)).isEmpty();
    }

    @Test
    void explainsWhichRuleFailed() {
        assertThat(PasswordPolicy.problem("Ab1")).contains("must be 8-72 characters");
        assertThat(PasswordPolicy.problem("A".repeat(70) + "bc1")).contains("must be 8-72 characters");
        assertThat(PasswordPolicy.problem(" Shelf2Read")).contains("must not start or end with a space");
        assertThat(PasswordPolicy.problem("shelf2read")).contains("needs both an uppercase and a lowercase letter");
        assertThat(PasswordPolicy.problem("SHELF2READ")).contains("needs both an uppercase and a lowercase letter");
        assertThat(PasswordPolicy.problem("ShelfReads")).contains("needs at least one number");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password1", "Password123!", "Welcome123", "Admin@12345", "Qwerty123", "123Letmein!", "Passw0rd1"})
    void refusesCommonPasswords(String password) {
        assertThat(PasswordPolicy.problem(password)).hasValueSatisfying(m -> assertThat(m).contains("too common"));
    }

    @Test
    void refusesPasswordsContainingTheNameOrEmail() {
        assertThat(PasswordPolicy.personalProblem("Ananya2026x", "Ananya Iyer", "a.iyer@example.com")).isPresent();
        assertThat(PasswordPolicy.personalProblem("Xiyer12345", "Ananya Iyer", "someone@example.com")).isPresent();
        assertThat(PasswordPolicy.personalProblem("My-aiyer-77", "Someone Else", "aiyer@example.com")).isPresent();
        // Short name parts (under 3 letters) don't count
        assertThat(PasswordPolicy.personalProblem("Joyful-Al-9", "Al Ng", "x@example.com")).isEmpty();
        assertThat(PasswordPolicy.personalProblem("Shelf2Read", "Ananya Iyer", "ananya@example.com")).isEmpty();
    }
}
