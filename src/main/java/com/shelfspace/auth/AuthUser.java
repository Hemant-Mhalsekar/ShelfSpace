package com.shelfspace.auth;

import com.shelfspace.member.Role;

/** The logged-in user, as read from the JWT. Available in controllers via @AuthenticationPrincipal. */
public record AuthUser(Long id, String email, Role role) {
}
