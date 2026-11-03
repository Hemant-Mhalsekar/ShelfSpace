-- The audit viewer lists newest first and filters by who did it.
CREATE INDEX idx_audit_created ON audit_log (created_at);
CREATE INDEX idx_audit_actor   ON audit_log (actor_id, created_at);
