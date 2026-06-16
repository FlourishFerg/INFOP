-- =========================================================
-- Onboarding status tracking for the multi-step profile flow
-- (Membership Selection -> Profile Completion)
-- =========================================================
ALTER TABLE profiles
    ADD COLUMN onboarding_completed BOOLEAN NOT NULL DEFAULT FALSE;
