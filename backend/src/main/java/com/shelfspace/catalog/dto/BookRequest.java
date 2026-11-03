package com.shelfspace.catalog.dto;

import com.shelfspace.catalog.validation.ValidIsbn;
import com.shelfspace.common.validation.PastOrNextYear;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;


/** Used for both create and update. initialCopies is only read on create. */
public record BookRequest(
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 255) @Pattern(regexp = "(?s).*\\p{L}.*", message = "must contain a letter") String author,
        @NotBlank @ValidIsbn String isbn,
        @NotBlank @Size(max = 100) @Pattern(regexp = "(?s).*\\p{L}.*", message = "must contain a letter") String category,
        @Min(1450) @PastOrNextYear Integer publicationYear,
        @DecimalMin("0.00") @DecimalMax(value = "1000000.00", message = "must be at most 1000000") @Digits(integer = 8, fraction = 2) BigDecimal replacementCost,
        @Min(0) @Max(50) Integer initialCopies
) {
}
