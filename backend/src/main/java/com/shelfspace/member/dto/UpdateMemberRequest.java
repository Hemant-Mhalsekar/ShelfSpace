package com.shelfspace.member.dto;

import com.shelfspace.common.validation.PersonName;
import com.shelfspace.member.MembershipTier;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateMemberRequest(
        @NotBlank @Size(max = 100) @PersonName String name,
        @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "must be 10-15 digits, optionally starting with +")
        String phone,
        @NotNull MembershipTier membershipTier
) {
}
