# 08 — Database Schema

## Local database

Recommended: Room/SQLite.

## Core tables

### users

```text
id TEXT PK
email TEXT NOT NULL
auth_provider TEXT NOT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
deleted_at INTEGER NULL
```

### profiles

```text
id TEXT PK
user_id TEXT NOT NULL FK users(id)
name TEXT NOT NULL
avatar_ref TEXT NULL
pin_verifier BLOB/TEXT NOT NULL
pin_version INTEGER NOT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
deleted_at INTEGER NULL
```

Index:
- profiles.user_id

### devices

```text
id TEXT PK
user_id TEXT NOT NULL
platform TEXT NOT NULL
model_label TEXT NOT NULL
os_version TEXT NOT NULL
app_version TEXT NOT NULL
created_at INTEGER NOT NULL
last_seen_at INTEGER NOT NULL
is_active INTEGER NOT NULL
```

### study_apps

```text
id TEXT PK
profile_id TEXT NOT NULL
package_name TEXT NOT NULL
app_label TEXT NOT NULL
icon_ref TEXT NULL
is_enabled INTEGER NOT NULL
added_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

Unique constraint:
- profile_id + package_name

### daily_targets

```text
id TEXT PK
profile_id TEXT NOT NULL
date_key TEXT NOT NULL
original_target_seconds INTEGER NOT NULL
adjusted_target_seconds INTEGER NULL
carry_in_seconds INTEGER NOT NULL DEFAULT 0
carry_out_seconds INTEGER NOT NULL DEFAULT 0
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

Unique:
- profile_id + date_key

### study_sessions

```text
id TEXT PK
profile_id TEXT NOT NULL
device_id TEXT NOT NULL
platform TEXT NOT NULL
package_name TEXT NULL
start_at INTEGER NOT NULL
end_at INTEGER NOT NULL
duration_seconds INTEGER NOT NULL
tracking_type TEXT NOT NULL
verification_status TEXT NOT NULL
subject_id TEXT NULL
created_at INTEGER NOT NULL
updated_at INTEGER NOT NULL
deleted_at INTEGER NULL
sync_version INTEGER NOT NULL
sync_state TEXT NOT NULL
```

Indexes:
- profile_id + start_at
- profile_id + date_key derivation/index strategy
- package_name + start_at
- sync_state

### session_events

```text
id TEXT PK
session_id TEXT NULL
profile_id TEXT NOT NULL
device_id TEXT NOT NULL
event_type TEXT NOT NULL
timestamp INTEGER NOT NULL
package_name TEXT NULL
engine_state TEXT NOT NULL
reason_code TEXT NULL
source TEXT NOT NULL
metadata_json TEXT NULL
created_at INTEGER NOT NULL
```

Indexes:
- profile_id + timestamp
- session_id + timestamp
- event_type + timestamp

### daily_stats

```text
id TEXT PK
profile_id TEXT NOT NULL
date_key TEXT NOT NULL
automatic_seconds INTEGER NOT NULL
manual_seconds INTEGER NOT NULL
total_seconds INTEGER NOT NULL
effective_target_seconds INTEGER NOT NULL
session_count INTEGER NOT NULL
longest_session_seconds INTEGER NOT NULL
streak_contribution INTEGER NOT NULL
updated_at INTEGER NOT NULL
```

## Referential integrity

Deleting a profile should not physically delete data immediately.

Preferred:
- soft-delete
- cloud deletion workflow
- local purge after user confirms/retention window expires

## Derived data

DailyStats is derived data.

The system must support rebuilding statistics from StudySession + DailyTarget.

## Server schema recommendation

Use a relational backend for cloud sync because the domain has:
- users
- profiles
- devices
- sessions
- events
- targets
- sync metadata

A managed PostgreSQL service is recommended for V1 speed; the app must hide provider details behind repository interfaces.

## Server-side access control

Every row containing user/profile data must be scoped to the authenticated user.

Never trust a profileId supplied by the client without server-side ownership validation.

## Migration strategy

Room migrations are mandatory once real test data exists.

Never solve schema changes by destructive reset in production builds.

## Integrity rules

- durationSeconds must equal end-start after allowed rounding policy
- endAt >= startAt
- target seconds >= 0
- profile must belong to authenticated user
- Study App package cannot be empty
- event timestamp must be valid
- sync version must be monotonic per record

## Auditability

Session edits should create an event or audit record so support/debugging can explain why automatic time changed.
