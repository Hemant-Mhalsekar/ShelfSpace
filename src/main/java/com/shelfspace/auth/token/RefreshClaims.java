package com.shelfspace.auth.token;

/** What a refresh token carries besides the user: its own id and the sign-in (family) it belongs to. */
public record RefreshClaims(Long userId, String tokenId, String familyId, int tokenVersion) {
}
