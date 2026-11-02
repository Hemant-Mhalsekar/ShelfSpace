package com.shelfspace.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank @Size(max = 100) String name,
        @Pattern(regexp = "^$|^\\+?[0-9]{10,15}$", message = "must be 10-15 digits, optionally starting with +")
        String phone
) {
}
