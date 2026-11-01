package com.shelfspace.fine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WaiveRequest(@NotBlank @Size(max = 255) String note) {
}
