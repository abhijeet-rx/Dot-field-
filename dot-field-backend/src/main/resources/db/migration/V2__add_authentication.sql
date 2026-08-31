-- ──────────────────────────────────────────────────────────────
-- DOT Field — Flyway Migration V2: Authentication Integration
-- Links profiles to users (one-to-one ownership).
-- ──────────────────────────────────────────────────────────────

-- 1. Add user_id column to profiles
ALTER TABLE profiles ADD COLUMN user_id BIGINT;

-- 2. Foreign Key: profiles.user_id → users.id
ALTER TABLE profiles
    ADD CONSTRAINT fk_profiles_user_id
    FOREIGN KEY (user_id) REFERENCES users(id);

-- 3. Unique index: one profile per user
CREATE UNIQUE INDEX uk_profiles_user_id ON profiles(user_id);
