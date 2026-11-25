-- =====================================================================
-- SIVA Crackers – V3 session versioning (Phase 4.5)
--
-- Every login token carries the user's token_version. Increasing it (for example
-- after a password change) makes every older session token invalid at once.
-- =====================================================================

ALTER TABLE users
    ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER last_login_at,
    ADD CONSTRAINT ck_users_token_version CHECK (token_version >= 0);
