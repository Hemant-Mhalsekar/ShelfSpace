package com.shelfspace.member.dto;

import com.shelfspace.member.MemberStatus;
import com.shelfspace.member.MembershipTier;
import com.shelfspace.member.Role;
import com.shelfspace.member.User;

import java.time.LocalDateTime;

/** Public view of a user. Never exposes the password hash. */
public record UserResponse(
        Long id,
        String name,
        String email,
        String phone,
        Role role,
        MembershipTier membershipTier,
        MemberStatus status,
        LocalDateTime createdAt
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(),
                u.getRole(), u.getMembershipTier(), u.getStatus(), u.getCreatedAt());
    }
}
