package com.shelfspace.auth;

import com.shelfspace.member.Role;

/**
 * The logged-in user, as read from the JWT. Available in controllers via @AuthenticationPrincipal.
 * tokenVersion must match the user's current version, or the token has been revoked.
 */
public record AuthUser(Long id, String email, Role role, int tokenVersion) {
}
