package com.shelfspace.member.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspendRequest(@NotBlank @Size(max = 255) String reason) {
}
