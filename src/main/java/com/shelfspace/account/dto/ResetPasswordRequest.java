package com.shelfspace.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** A librarian sets a temporary password for a member who is locked out. */
public record ResetPasswordRequest(
        @NotBlank
        @Size(min = 8, max = 72, message = "must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "must contain at least one letter and one number")
        String newPassword
) {
}
