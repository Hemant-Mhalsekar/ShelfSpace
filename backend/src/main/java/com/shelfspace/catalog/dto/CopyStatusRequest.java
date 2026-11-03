package com.shelfspace.catalog.dto;

import com.shelfspace.catalog.CopyStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CopyStatusRequest(
        @NotNull CopyStatus status,
        @Size(max = 255) String note
) {
}
