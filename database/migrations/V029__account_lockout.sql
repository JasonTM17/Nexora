-- M6-DB01: account lockout after consecutive failed authentication attempts.
-- Application-layer tracking: failed_login_count increments on auth failure,
-- resets on success. locked_until gates re-authentication attempts.

BEGIN;
SET LOCAL ROLE nexora_migrator;
SET LOCAL search_path = pg_catalog, nexora, public;

ALTER TABLE nexora.profiles
    ADD COLUMN IF NOT EXISTS failed_login_count integer NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS locked_until timestamptz;

CREATE INDEX idx_profiles_locked
    ON nexora.profiles (locked_until)
    WHERE locked_until IS NOT NULL;

COMMENT ON COLUMN nexora.profiles.failed_login_count IS
    'Consecutive failed authentication attempts. Reset on success.';
COMMENT ON COLUMN nexora.profiles.locked_until IS
    'Account locked until this timestamp. NULL when not locked.';

COMMIT;
