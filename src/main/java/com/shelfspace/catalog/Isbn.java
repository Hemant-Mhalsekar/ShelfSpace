package com.shelfspace.catalog;

/** ISBN helpers: strips formatting and verifies ISBN-10 / ISBN-13 check digits. */
public final class Isbn {

    private Isbn() {
    }

    /** "978-0-13-235088-4" -> "9780132350884". Keeps a trailing X for ISBN-10. */
    public static String normalize(String raw) {
        return raw == null ? null : raw.replaceAll("[\\s-]", "").toUpperCase();
    }

    public static boolean isValid(String raw) {
        String isbn = normalize(raw);
        if (isbn == null) {
            return false;
        }
        return switch (isbn.length()) {
            case 10 -> isValidIsbn10(isbn);
            case 13 -> isValidIsbn13(isbn);
            default -> false;
        };
    }

    private static boolean isValidIsbn10(String isbn) {
        if (!isbn.matches("\\d{9}[\\dX]")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn.charAt(i);
            int digit = (c == 'X') ? 10 : c - '0';
            sum += digit * (10 - i);
        }
        return sum % 11 == 0;
    }

    private static boolean isValidIsbn13(String isbn) {
        if (!isbn.matches("\\d{13}")) {
            return false;
        }
        int sum = 0;
        for (int i = 0; i < 13; i++) {
            int digit = isbn.charAt(i) - '0';
            sum += (i % 2 == 0) ? digit : digit * 3;
        }
        return sum % 10 == 0;
    }
}
