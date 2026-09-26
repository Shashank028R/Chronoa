# Workspace Status

**Document Version:** 1.3.0  
**Date:** 2026-09-23  
**Current Phase:** PHASE 3 — CLOUD SYNCHRONIZATION & REMOTE BACKEND (COMPLETE)  
**Author:** Antigravity Lead Engineer  

---

## 1. Documentation Status

* **Documentation Reviewed:** Complete specification suite in `/docs` (including README, requirements review, architecture, platform references, database schema, data model, APIs, UX/UI, error edge cases, testing strategy, and roadmap).
* **Number of Specification Files Reviewed:** 30 files total (29 Markdown specifications + `CHECKSUMS.txt`).

---

## 2. Existing Project

* **Project Exists / Does Not Exist:** Android application project initialized and active.
* **Android Project Status:** Complete Android project scaffold created with namespace `com.studycompanion.app`, minSdk 29, targetSdk 36, compileSdk 36.
* **Gradle Status:** Gradle wrapper configured (8.11.1), root and app `build.gradle.kts` configured with Kotlin 2.0.21, AGP 8.8.0, KSP 2.0.21-1.0.28, Room 2.6.1, DataStore 1.1.1, Jetpack Compose, and AndroidX libraries.
* **Current Source Status:** 
  - **Phase 0.2 POC:** Fully intact and testable under `com.studycompanion.app.poc`.
  - **Phase 1 Implementation:** Production architecture, Room database (10 entities, 10 DAOs), PBKDF2 cryptographic PIN security, Repositories, Use Cases, DataStore session preferences, Onboarding & Dashboard UI scaffolding, AppContainer DI, and 22 unit tests passing (100%).
* **Workspace Setup Performed:** Directory junction `docs` created; portable JDK 17 configured in `gradle.properties`. Connected physical device detected (`QCNBQCINSK4PMJAY`).
* **Current Project Structure:**
  ```
  app/src/main/java/com/studycompanion/app/
  ├── MainActivity.kt
  ├── StudyCompanionApplication.kt
  ├── core/
  │   ├── database/
  │   │   ├── AppDatabase.kt
  │   │   ├── dao/
  │   │   │   └── Daos.kt (UserDao, ProfileDao, ProfileSettingsDao, DeviceDao, StudyAppDao, DailyTargetDao, StudySessionDao, SessionEventDao, DailyStatsDao, SyncMutationDao)
  │   │   └── entity/
  │   │       └── Entities.kt (UserEntity, ProfileEntity, ProfileSettingsEntity, DeviceEntity, StudyAppEntity, DailyTargetEntity, StudySessionEntity, SessionEventEntity, DailyStatsEntity, SyncMutationEntity)
  │   ├── datastore/
  │   │   └── UserSessionDataStore.kt
  │   ├── di/
  │   │   └── AppContainer.kt (AppContainer, DefaultAppContainer)
  │   └── security/
  │       └── PinVerifier.kt (PBKDF2WithHmacSHA256, 12k iterations, 16-byte salt, constant-time compare)
  ├── domain/
  │   ├── model/
  │   │   └── DomainModels.kt (User, Profile, ProfileSettings, DailyTarget, StudyApp, StudySession, SessionEvent, DailyStats, AuthSession)
  │   ├── repository/
  │   │   ├── AuthRepository.kt
  │   │   ├── ProfileRepository.kt
  │   │   ├── TargetRepository.kt
  │   │   ├── StudyAppRepository.kt
  │   │   └── SessionRepository.kt
  │   └── usecase/
  │       ├── ProfileUseCases.kt
  │       ├── TargetUseCases.kt
  │       └── StudyAppUseCases.kt
  ├── data/
  │   └── repository/
  │       ├── AuthRepositoryImpl.kt
  │       ├── ProfileRepositoryImpl.kt
  │       ├── TargetRepositoryImpl.kt
  │       ├── StudyAppRepositoryImpl.kt
  │       └── SessionRepositoryImpl.kt
  ├── feature/
  │   └── onboarding/
  │       ├── OnboardingViewModel.kt
  │       └── OnboardingScreen.kt
  └── poc/
      ├── UsageEventsPocEngine.kt
      ├── PocTimelineView.kt
      └── PocModels.kt
  ```

---

## 3. Architecture Understanding

### 3.1 Application Layers
The application adopts an offline-first, clean, unidirectional data-flow architecture:
1. **Presentation Layer (Jetpack Compose + ViewModel):** Observes immutable UI state via Kotlin Coroutines `StateFlow`. UI components never invent or extrapolate timer state independently; they reflect states published by the Focus Engine and Domain repositories.
2. **Domain Layer (Use Cases & Models):** Houses pure business logic, target evaluation, streak computation, and event normalization.
3. **Tracking & Focus Engine Subsystem:** The authoritative state machine controlling study session tracking.
4. **Data Layer (Repositories, Local DB, Remote API):** Single source of operational truth is the local Room SQLite database. Cloud synchronizer operates via an idempotent push/pull queue in the background.

```
       UI / Jetpack Compose
                ↓ (observes StateFlow)
      ViewModel / State Holders
                ↓
            Use Cases
                ↓
    ┌────────────────────────┐
    │      Focus Engine      │ ──> Authoritative Timer State
    └────────────────────────┘
                ↓
           Repositories
          ↙            ↘
      Local DB      Remote API
       (Room)       (Cloud Sync)
          ↘            ↙
           Sync Engine
```

### 3.2 Focus Engine
* **Single Source of Truth:** Decides deterministically whether the device is in an eligible focus state.
* **Inputs:** Foreground package via Android `UsageStatsManager.queryEvents()`, screen interactive state, keyguard state, call state from `TelecomManager`, window mode state, user commands (start/pause/stop), and active profile study rules.
* **State Machine States:** `IDLE`, `READY`, `FOCUSING`, `LOCKED_FOCUS`, `PAUSED_UNAPPROVED_APP`, `PAUSED_HOME`, `PAUSED_CALL`, `PAUSED_MULTIWINDOW`, `PAUSED_FLOATING`, `PAUSED_USER`, `WAITING_FOR_PERMISSION`, `RECOVERING`, `ERROR`.
* **Timing Model:** Uses monotonic elapsed clocks for smooth UI rendering combined with immutable wall-clock/epoch event timestamps for persisted session boundaries. Avoids ticking databases every second; records discrete intervals (`start_at`, `end_at`).

### 3.3 Local Database
* Built on **Room / SQLite**.
* Key entities: `users`, `profiles`, `devices`, `study_apps`, `daily_targets`, `study_sessions`, `session_events`, `daily_stats`, and `sync_mutations`.
* Strict referential integrity: Profile-scoped data; soft deletions with tombstones for cross-device sync propagation; transactional writes.

### 3.4 Backend & Cloud Synchronization
* Relational cloud backend (managed PostgreSQL / Supabase or custom REST with JWT auth).
* Cloud access strictly validates user ownership (Row Level Security or server authorization).
* Synchronizer uses cursor-based change polling (`/sync/pull?cursor=`) and batch mutation pushing (`/sync/push`) with client-generated UUIDs and server-side idempotency.
* Offline resilience: Local tracking is completely detached from network availability; mutations queue locally until connectivity is restored.

### 3.5 Authentication & Security
* Account-level auth: Email and password with TLS transport; securely stored JWT tokens (Android Keystore / EncryptedSharedPreferences).
* Profile-level security: Independent PIN/password protection per profile with salt and cryptographic KDF verifier (e.g. PBKDF2/Argon2id). Profile switching clears memory contexts to prevent data leakage.
* Zero privacy violations: No telemetry, no keystroke logging, no screen recording, no notification text capture.

---

## 4. V1 Scope

The following features form the exact, bounded V1 deliverable:
1. **Account Management:** User registration, email/password login, logout, token refresh, account deletion.
2. **Profiles:** Multi-profile support under a single account; PIN/password verification for profile switching.
3. **Daily Study Targets:** Daily study target setup; immutable original target with layered per-day adjustments and optional carry-over offers.
4. **Study App Management:** Selection, searching, local renaming, and persistence of user-approved Study Apps.
5. **Focus Engine & Automatic Tracking:**
   - Real-time event-driven tracking using `UsageEvents` (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`).
   - Approved app in foreground = timer counts (`FOCUSING`).
   - Unapproved app or Android home screen = immediate pause (`PAUSED_UNAPPROVED_APP` / `PAUSED_HOME`).
   - Resuming approved app restarts active interval.
6. **Device State Handlers:**
   - Screen lock tracking: Configurable (ON by default tracks as `LOCKED_FOCUS`; OFF pauses).
   - Call interruption: Configurable (pause during active calls via `TelecomManager.isInCall()`).
   - Notification behavior: Receiving notifications does NOT pause; tapping a notification and switching apps follows standard app-switch rules.
7. **Manual Sessions & Editing:**
   - Manual session creation and editing with explicit, permanent "Not automatically verified" badge.
   - Automatic session editing and deletion with audit trail logging.
8. **User Interface (Apple-inspired Minimalist Aesthetic):**
   - Home Screen: Greeting, profile selector, large focal timer, target progress, status indicator, primary focus action, quick stats.
   - Focus View: Immersive, distraction-free display with minimal AMOLED black mode and Clock mode.
   - Session History: Daily, weekly, monthly calendar browsing with detailed session cards.
   - Statistics: Total time, streak calculation, target completion rate, longest session, top Study Apps.
   - Settings: Focus rules, profile security, theme selection, data export, sync management.
   - Permission & Health Center: Visual diagnostics for Usage Access, Notifications, Phone State, and Battery Optimization.
9. **Offline-First & Cloud Synchronization:**
   - Complete local operational autonomy without internet.
   - Background push/pull sync via WorkManager when online.
10. **Lifecycle & Crash Recovery:**
    - Seamless session reconciliation across app process death and device reboots.

---

## 5. Future Scope (Strictly Excluded from V1)

The following components are architecturally anticipated but must NOT be implemented in V1:
* **AI Chat, AI Planner, and AI Insights:** No LLM integration or chat UI.
* **PC Companion:** No desktop tracking client, idle detection, or Windows companion app.
* **OnePlus Live Alert:** Expose only a clean `LiveStatePublisher` interface; no OEM-proprietary runtime integration in V1.
* **Notification Blocking:** `NotificationListenerService` suppression deferred to V2 to prevent Google Play policy friction.
* **Social Feeds & Gamification:** No leaderboards, public profiles, or friend feeds.
* **Ambient Soundscapes & Visual Themes:** No audio synthesis or animated 3D nature/space themes.

---

## 6. Technical Risks & Android Platform Validation

| Risk Area | Android Platform Reality | V1 Mitigation & Fallback Strategy |
|---|---|---|
| **Foreground App Detection** | Android does not provide an instant callback when third-party apps open. `UsageStatsManager.queryEvents()` provides historical events. | Run a lightweight polling loop (e.g. 500–1000ms) in an active foreground service while tracking. Use event `timeStamp` as the exact, authoritative transition boundary to prevent timer drift. |
| **Split-Screen & Floating Windows** | `Activity.isInMultiWindowMode()` only reports the calling app's own state; Android provides NO public API to detect if a third-party app is in multi-window/floating mode. | Do NOT pretend universal cross-app split-screen detection exists. For V1, detect multi-window on our own activity, report capability limits honestly, and avoid non-compliant AccessibilityService workarounds. |
| **VoIP & 3rd-Party App Calls** | `TelecomManager.isInCall()` requires `READ_PHONE_STATE` and only detects cellular and `ConnectionService`-integrated calls. Proprietary VoIP calls (WhatsApp, Telegram) are invisible to this API. | Expose standard call detection for cellular/system telecom. Explicitly inform users in the UI that unsupported 3rd-party VoIP calls are not guaranteed to pause the timer. |
| **Android 14+ Foreground Service Restrictions** | Background service starts are restricted (Android 12+); declared FGS types are mandatory (Android 14+). | Only start FGS from user interaction in the UI (`USER_START`). Declare `specialUse` (or `dataSync`) with explicit use-case properties and rationale in manifest. |
| **OEM Battery Optimization & Process Death** | Aggressive OEM managers (ColorOS, MIUI, OneUI) terminate background tasks and delay alarms. | Implement a Permission & Health Center guiding users to disable aggressive battery optimization. Persist active session tokens to Room/DataStore so recovery logic accurately reconstructs state on restart. |
| **Installed App Enumeration** | Google Play heavily restricts `QUERY_ALL_PACKAGES`. | Query only launcher activities using `Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)` via `PackageManager.queryIntentActivities()`. This covers all user-facing study apps without sensitive permission policy violations. |
| **Clock Tampering / Timezone Drift** | Users changing device clock or crossing timezone boundaries could corrupt session durations. | Store all timestamps as UTC epoch instants. Capture `ACTION_TIME_CHANGED` and `ACTION_TIMEZONE_CHANGED`, close in-progress intervals, and enforce `durationSeconds = max(0, endAt - startAt)`. |

---

## 7. Decisions Required

The following engineering decisions must be finalized prior to or during Phase 0/Phase 1:
1. **Product Name & Application ID:** Choose official brand name and package ID (e.g., `com.studycompanion.app`).
2. **Minimum SDK Version:** Recommend `minSdk = 29` (Android 10) because `UsageEvents.Event.ACTIVITY_RESUMED` and `ACTIVITY_PAUSED` were introduced in API 29.
3. **Cloud Provider Selection:** Standardize on Supabase (PostgreSQL + Auth + RLS) vs custom Node.js/PostgreSQL container for backend development.
4. **Settings Persistence:** Store `ProfileSettings` in AndroidX DataStore for lightweight preference storage, or inside a dedicated Room table `profile_settings` for relational consistency during multi-profile switching. (Recommendation: Room table `profile_settings` guarantees transactional atomicity with profile deletions).

---

## 8. Recommended Implementation Order

To ensure architectural integrity, avoid rework, and adhere to gating criteria, the project will execute in 7 distinct, sequential phases:

```
Phase 0: Platform POC & Gradle Scaffold
   ↓
Phase 1: Project Architecture & Local Foundation (Data, Auth, Profiles)
   ↓
Phase 2: Focus Engine & Automatic Tracking (Core Differentiator)
   ↓
Phase 3: Cloud Synchronization & Remote Backend
   ↓
Phase 4: Presentation Layer & Minimalist UI Polish (Compose)
   ↓
Phase 5: Platform Hardening & Edge-Case Verification
   ↓
Phase 6: Release Candidate & Production Readiness
```

1. **Phase 0 — Documentation Audit, Project Initialization & Platform POC:**
   - Initialize Gradle project (Kotlin, AGP, Compose, Room, Coroutines).
   - Build a barebones Android test shell validating `UsageEvents` query loop, permission detection, and state machine transitions.
2. **Phase 1 — Core Architecture, Auth & Local Storage Foundation:**
   - Package structure, Room database, DataStore, encryption primitives (Keystore/Argon2id), Account auth, and Profile management.
3. **Phase 2 — Focus Engine & Tracking Implementation:**
   - Complete state machine, foreground app resolution, lock/call event handlers, session creation, and crash recovery.
4. **Phase 3 — Cloud Synchronization:**
   - Remote repository layer, Supabase/PostgreSQL schema, idempotent push/pull sync engine, offline mutation queue.
5. **Phase 4 — Premium Minimalist User Interface:**
   - Jetpack Compose design system, Home screen, Focus view, Session Editor, History, Statistics, Settings, and Health Center.
6. **Phase 5 — Edge-Case Hardening & Device Testing:**
   - Battery optimization diagnostics, reboot recovery, timezone handling, multi-window fallbacks, and performance profiling.
7. **Phase 6 — Release Candidate Validation:**
   - Acceptance test matrix execution, Play Store compliance audit, security review, and release build verification.

---

## 9. Current Phase
`PHASE 0.2 — ANDROID PROJECT SCAFFOLD & USAGE-EVENTS POC (NEEDS_REVIEW — READY FOR ON-DEVICE RUN)`

## 10. Next Phase
`PHASE 1 — CORE ARCHITECTURE, LOCAL DATABASE, AUTH & PROFILES`
