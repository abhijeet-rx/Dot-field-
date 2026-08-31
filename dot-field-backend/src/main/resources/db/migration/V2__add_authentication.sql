-- ──────────────────────────────────────────────────────────────
-- DOT Field — Flyway Migration V2: Authentication Integration
-- Links profiles to users (one-to-one ownership).
-- ──────────────────────────────────────────────────────────────

-- 1. Add user_id column to profiles
ALTER TABLE profiles ADD COLUMN user_id BIGINT;

-- 2. Safety guard: fail if multiple unlinked profiles exist
--    (prevents silently attaching multiple candidates to a single user)
DO $$
DECLARE
    unlinked_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO unlinked_count FROM profiles WHERE user_id IS NULL;
    IF unlinked_count > 1 THEN
        RAISE EXCEPTION
            'MIGRATION ABORTED: Found % existing profiles with no user_id. '
            'Cannot safely auto-assign ownership. '
            'Manually assign user_id to each profile before re-running migration.',
            unlinked_count;
    END IF;
END $$;

-- 3. Foreign Key: profiles.user_id → users.id
ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_user_id
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 4. Unique index: one profile per user
CREATE UNIQUE INDEX uk_profiles_user_id ON profiles(user_id);
