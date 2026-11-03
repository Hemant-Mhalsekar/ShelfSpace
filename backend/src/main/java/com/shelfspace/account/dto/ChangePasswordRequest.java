package com.shelfspace.account.dto;

import com.shelfspace.common.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ChangePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @StrongPassword
        String newPassword
) {
}
