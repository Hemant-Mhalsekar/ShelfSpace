package com.shelfspace.account.dto;

import com.shelfspace.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

/** A librarian sets a temporary password for a member who is locked out. */
public record ResetPasswordRequest(
        @NotBlank @StrongPassword
        String newPassword
) {
}
