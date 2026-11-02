-- Bumping a user's token_version instantly invalidates every token issued before it
-- (password change, librarian reset, suspension, "sign out other devices").
ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0;

-- One row per refresh token. Tokens rotate on every use; a token used twice means it was copied,
-- so its whole family (one sign-in on one device) is revoked.
CREATE TABLE refresh_tokens (
    id         VARCHAR(36) PRIMARY KEY,          -- the token's jti claim
    user_id    BIGINT      NOT NULL,
    family_id  VARCHAR(36) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    used_at    DATETIME(6) NULL,
    revoked_at DATETIME(6) NULL,
    CONSTRAINT fk_refresh_user FOREIGN KEY (user_id) REFERENCES users (id),
    INDEX idx_refresh_user (user_id),
    INDEX idx_refresh_family (family_id),
    INDEX idx_refresh_expires (expires_at)
);
