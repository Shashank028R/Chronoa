# CHRONOA — PRIVACY & DATA INVENTORY

This document provides a technical source-of-truth inventory of all data handled by the Chronoa Android application.

## 1. User Account Information
- **Fields**: Email, cryptographic password salt, PBKDF2 hash, OAuth/auth tokens.
- **Purpose**: Authenticate user and enable multi-device synchronization.
- **Storage**:
  - Local: Room SQLite (`users` table) and Encrypted `UserSessionDataStore`.
  - Remote: Supabase Auth (`auth.users`).
- **Retention**: Retained until user triggers account deletion.
- **Deletion**: Triggered via Settings -> "Delete Account & Data".

## 2. Study Session Records
- **Fields**: Session start timestamp, end timestamp, elapsed active seconds, pause intervals, session status (`COMPLETED`, `ABANDONED`).
- **Purpose**: Calculate study analytics, daily progress, and streak statistics.
- **Storage**:
  - Local: Room SQLite (`study_sessions` and `session_events` tables).
  - Remote: Supabase Database (`study_sessions`).
- **Sharing**: Never shared with any third party.

## 3. Approved Study Applications
- **Fields**: Package name (e.g. `com.google.android.apps.docs`), application display label.
- **Purpose**: Allow FocusEngine to identify when an approved study app is in the foreground.
- **Storage**:
  - Local: Room SQLite (`study_apps` table).
  - Remote: Supabase Database (`study_apps`).
- **Sharing**: Never shared with third parties.

## 4. On-Device Usage Data
- **Fields**: Active foreground package name detected via Android `UsageStatsManager`.
- **Handling**: Inspected strictly in memory on-device to determine study state. Never stored, persisted, or transmitted to any server.

## 5. Diagnostic Data
- **Fields**: OS version, manufacturer, battery optimization state, notification permission status.
- **Handling**: Generated locally on user demand in Health Center. Stored as local JSON export file upon user request. Zero telemetry dispatched to cloud.
