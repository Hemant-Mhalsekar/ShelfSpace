package com.shelfspace.auth.dto;

import com.shelfspace.common.validation.PasswordNotPersonal;
import com.shelfspace.common.validation.PersonName;
import com.shelfspace.common.validation.StrongPassword;
import com.shelfspace.common.validation.ValidEmail;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@PasswordNotPersonal
public record RegisterRequest(
        @NotBlank @Size(max = 100) @PersonName
        String name,

        @NotBlank @ValidEmail @Size(max = 150)
        String email,

        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits, optionally starting with +")
        String phone,

        @NotBlank @StrongPassword
        String password
) {
}
