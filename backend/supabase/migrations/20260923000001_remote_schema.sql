-- 20260923000001_remote_schema.sql
-- Production Remote PostgreSQL / Supabase Schema for Study Companion
-- Implements complete data model, change sequences, and Row Level Security (RLS)

-- 1. EXTENSIONS
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- 2. CHANGE TRACKING SEQUENCE FOR PULL SYNC CURSOR
CREATE SEQUENCE IF NOT EXISTS sync_change_seq START WITH 1 INCREMENT BY 1;

-- 3. USERS TABLE
CREATE TABLE IF NOT EXISTS public.users (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    email TEXT NOT NULL,
    auth_provider TEXT NOT NULL DEFAULT 'EMAIL',
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT NULL
);

-- 4. PROFILES TABLE
CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    avatar_ref TEXT NULL,
    pin_verifier TEXT NOT NULL,
    pin_version INT NOT NULL DEFAULT 1,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT NULL
);
CREATE INDEX IF NOT EXISTS idx_profiles_user_id ON public.profiles(user_id);

-- 5. PROFILE SETTINGS TABLE
CREATE TABLE IF NOT EXISTS public.profile_settings (
    profile_id UUID PRIMARY KEY REFERENCES public.profiles(id) ON DELETE CASCADE,
    count_while_locked BOOLEAN NOT NULL DEFAULT TRUE,
    pause_during_calls BOOLEAN NOT NULL DEFAULT TRUE,
    pause_in_multi_window BOOLEAN NOT NULL DEFAULT TRUE,
    pause_in_floating_window BOOLEAN NOT NULL DEFAULT TRUE,
    theme_preference TEXT NOT NULL DEFAULT 'SYSTEM',
    updated_at BIGINT NOT NULL
);

-- 6. DEVICES TABLE
CREATE TABLE IF NOT EXISTS public.devices (
    id TEXT PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    platform TEXT NOT NULL,
    model_label TEXT NOT NULL,
    os_version TEXT NOT NULL,
    app_version TEXT NOT NULL,
    created_at BIGINT NOT NULL,
    last_seen_at BIGINT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX IF NOT EXISTS idx_devices_user_id ON public.devices(user_id);

-- 7. STUDY APPS TABLE
CREATE TABLE IF NOT EXISTS public.study_apps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    package_name TEXT NOT NULL,
    app_label TEXT NOT NULL,
    icon_ref TEXT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    added_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_profile_package UNIQUE (profile_id, package_name)
);
CREATE INDEX IF NOT EXISTS idx_study_apps_profile_id ON public.study_apps(profile_id);

-- 8. DAILY TARGETS TABLE
CREATE TABLE IF NOT EXISTS public.daily_targets (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    date_key TEXT NOT NULL,
    original_target_seconds INT NOT NULL,
    adjusted_target_seconds INT NULL,
    carry_in_seconds INT NOT NULL DEFAULT 0,
    carry_out_seconds INT NOT NULL DEFAULT 0,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    CONSTRAINT uq_profile_date UNIQUE (profile_id, date_key)
);
CREATE INDEX IF NOT EXISTS idx_daily_targets_profile ON public.daily_targets(profile_id, date_key);

-- 9. STUDY SESSIONS TABLE (Immutable intervals)
CREATE TABLE IF NOT EXISTS public.study_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    platform TEXT NOT NULL,
    package_name TEXT NULL,
    start_at BIGINT NOT NULL,
    end_at BIGINT NOT NULL,
    duration_seconds BIGINT NOT NULL,
    tracking_type TEXT NOT NULL,
    verification_status TEXT NOT NULL,
    subject_id TEXT NULL,
    created_at BIGINT NOT NULL,
    updated_at BIGINT NOT NULL,
    deleted_at BIGINT NULL,
    sync_version INT NOT NULL DEFAULT 1,
    sync_state TEXT NOT NULL DEFAULT 'SYNCED'
);
CREATE INDEX IF NOT EXISTS idx_study_sessions_profile_start ON public.study_sessions(profile_id, start_at);

-- 10. SESSION EVENTS TABLE
CREATE TABLE IF NOT EXISTS public.session_events (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID NULL REFERENCES public.study_sessions(id) ON DELETE SET NULL,
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    device_id TEXT NOT NULL,
    event_type TEXT NOT NULL,
    timestamp BIGINT NOT NULL,
    package_name TEXT NULL,
    engine_state TEXT NOT NULL,
    reason_code TEXT NULL,
    source TEXT NOT NULL,
    metadata_json TEXT NULL
);
CREATE INDEX IF NOT EXISTS idx_session_events_session_id ON public.session_events(session_id);
CREATE INDEX IF NOT EXISTS idx_session_events_profile_time ON public.session_events(profile_id, timestamp);

-- 11. DAILY STATS TABLE
CREATE TABLE IF NOT EXISTS public.daily_stats (
    profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    date_key TEXT NOT NULL,
    total_study_seconds BIGINT NOT NULL DEFAULT 0,
    verified_study_seconds BIGINT NOT NULL DEFAULT 0,
    manual_study_seconds BIGINT NOT NULL DEFAULT 0,
    target_seconds INT NOT NULL DEFAULT 0,
    target_achieved BOOLEAN NOT NULL DEFAULT FALSE,
    session_count INT NOT NULL DEFAULT 0,
    updated_at BIGINT NOT NULL,
    PRIMARY KEY (profile_id, date_key)
);

-- 12. SYNC CHANGE LOG (Monotonically increasing cursor for pull sync)
CREATE TABLE IF NOT EXISTS public.sync_change_log (
    cursor_id BIGINT PRIMARY KEY DEFAULT nextval('sync_change_seq'),
    user_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
    profile_id UUID NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    entity_type TEXT NOT NULL,
    entity_id TEXT NOT NULL,
    operation TEXT NOT NULL,
    payload_json JSONB NOT NULL,
    server_timestamp BIGINT NOT NULL DEFAULT (extract(epoch from now()) * 1000)::BIGINT
);
CREATE INDEX IF NOT EXISTS idx_sync_change_log_user_cursor ON public.sync_change_log(user_id, cursor_id);

-- 13. ROW LEVEL SECURITY (RLS) POLICIES
ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.profile_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.devices ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.study_apps ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_targets ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.study_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.session_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.daily_stats ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.sync_change_log ENABLE ROW LEVEL SECURITY;

-- User ownership policies
CREATE POLICY user_isolation_policy ON public.users
    FOR ALL USING (auth.uid() = id);

CREATE POLICY device_isolation_policy ON public.devices
    FOR ALL USING (auth.uid() = user_id);

CREATE POLICY profile_isolation_policy ON public.profiles
    FOR ALL USING (auth.uid() = user_id);

-- Profile-scoped ownership policies (Child tables verify profile belongs to authenticated user)
CREATE POLICY profile_settings_isolation_policy ON public.profile_settings
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY study_apps_isolation_policy ON public.study_apps
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY daily_targets_isolation_policy ON public.daily_targets
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY study_sessions_isolation_policy ON public.study_sessions
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY session_events_isolation_policy ON public.session_events
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY daily_stats_isolation_policy ON public.daily_stats
    FOR ALL USING (profile_id IN (SELECT id FROM public.profiles WHERE user_id = auth.uid()));

CREATE POLICY sync_change_log_isolation_policy ON public.sync_change_log
    FOR ALL USING (auth.uid() = user_id);
