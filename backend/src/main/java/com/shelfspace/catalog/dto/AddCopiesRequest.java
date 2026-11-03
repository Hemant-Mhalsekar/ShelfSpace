package com.shelfspace.catalog.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddCopiesRequest(@NotNull @Min(1) @Max(50) Integer count) {
}
