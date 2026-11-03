package com.shelfspace.catalog;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IsbnTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "9780132350884",        // Clean Code
            "978-0-13-235088-4",    // same, with hyphens
            "0132350882",           // Clean Code, ISBN-10
            "080442957X",           // ISBN-10 with X check digit
            "978 0 596 52068 7"     // spaces
    })
    void acceptsValidIsbns(String isbn) {
        assertThat(Isbn.isValid(isbn)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "9780132350885",   // wrong check digit
            "0132350883",      // wrong ISBN-10 check digit
            "12345",           // too short
            "97801323508AB",   // letters
            "X132350882",      // X only allowed at the end
            ""
    })
    void rejectsInvalidIsbns(String isbn) {
        assertThat(Isbn.isValid(isbn)).isFalse();
    }

    @Test
    void normalizeStripsHyphensAndSpaces() {
        assertThat(Isbn.normalize("978-0 13-235088-4")).isEqualTo("9780132350884");
        assertThat(Isbn.normalize("080442957x")).isEqualTo("080442957X");
    }
}
