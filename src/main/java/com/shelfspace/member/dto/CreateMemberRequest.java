package com.shelfspace.member.dto;

import com.shelfspace.common.validation.PasswordNotPersonal;
import com.shelfspace.common.validation.PersonName;
import com.shelfspace.common.validation.StrongPassword;
import com.shelfspace.common.validation.ValidEmail;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin adds someone at the desk. role defaults to MEMBER; ADMIN creates another librarian. */
@PasswordNotPersonal
public record CreateMemberRequest(
        @NotBlank @Size(max = 100) @PersonName String name,
        @NotBlank @ValidEmail @Size(max = 150) String email,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits, optionally starting with +")
        String phone,
        @NotBlank @StrongPassword
        String password,
        @NotNull MembershipTier membershipTier,
        Role role
) {
}
