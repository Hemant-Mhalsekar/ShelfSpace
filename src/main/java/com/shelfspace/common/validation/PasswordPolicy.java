package com.shelfspace.common.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The one set of password rules, used by sign-up, "add member", password change and librarian reset.
 * The web app shows the same rules as a checklist while typing (frontend/src/lib/validation.ts).
 */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** bcrypt only looks at the first 72 bytes. */
    public static final int MAX_LENGTH = 72;

    private static final Set<String> COMMON = loadCommon();

    private PasswordPolicy() {
    }

    /** First rule the password breaks, as a message that follows the field name ("password ..."), or empty if it's fine. */
    public static Optional<String> problem(String password) {
        if (password == null || password.isEmpty()) {
            return Optional.empty();   // @NotBlank reports missing values
        }
        if (password.length() < MIN_LENGTH || password.length() > MAX_LENGTH) {
            return Optional.of("must be " + MIN_LENGTH + "-" + MAX_LENGTH + " characters");
        }
        if (!password.equals(password.strip())) {
            return Optional.of("must not start or end with a space");
        }
        boolean upper = password.chars().anyMatch(Character::isUpperCase);
        boolean lower = password.chars().anyMatch(Character::isLowerCase);
        boolean digit = password.chars().anyMatch(Character::isDigit);
        if (!upper || !lower) {
            return Optional.of("needs both an uppercase and a lowercase letter");
        }
        if (!digit) {
            return Optional.of("needs at least one number");
        }
        if (isCommon(password)) {
            return Optional.of("is too common. Choose something harder to guess");
        }
        return Optional.empty();
    }

    /** Refuses passwords containing the person's email name or a part of their name (3+ letters). */
    public static Optional<String> personalProblem(String password, String name, String email) {
        if (password == null || password.isEmpty()) {
            return Optional.empty();
        }
        String pw = password.toLowerCase(Locale.ROOT);
        String emailName = email == null ? "" : email.strip().toLowerCase(Locale.ROOT).split("@", 2)[0];
        boolean personal = (emailName.length() >= 3 && pw.contains(emailName))
                || (name != null && Arrays.stream(name.toLowerCase(Locale.ROOT).split("[^\\p{L}]+"))
                        .filter(part -> part.length() >= 3)
                        .anyMatch(pw::contains));
        return personal ? Optional.of("must not contain your name or email") : Optional.empty();
    }

    static boolean isCommon(String password) {
        String base = password.toLowerCase(Locale.ROOT).replaceAll("^[^\\p{L}]+|[^\\p{L}]+$", "");
        return COMMON.contains(base) || COMMON.contains(password.toLowerCase(Locale.ROOT));
    }

    private static Set<String> loadCommon() {
        var in = Objects.requireNonNull(PasswordPolicy.class.getResourceAsStream("/validation/common-passwords.txt"),
                "validation/common-passwords.txt is missing");
        try (var reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            return reader.lines().map(String::strip)
                    .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                    .map(l -> l.toLowerCase(Locale.ROOT))
                    .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
