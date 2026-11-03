package com.shelfspace.circulation.dto;

import jakarta.validation.constraints.NotNull;

public record LoanRequest(@NotNull Long bookId) {
}
