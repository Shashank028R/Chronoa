# Play Store Readiness & Policy Compliance Audit

This document outlines Google Play Store policy compliance, permission justifications, data disclosures, and store readiness for **Study Companion v1.0.0-rc01**.

---

## 1. Permissions Audit & Justification

| Permission | Protection Level | Purpose & Necessity | Policy Risk & Compliance Notes |
|---|---|---|---|
| `android.permission.PACKAGE_USAGE_STATS` | Signature / System / AppOps | Required to detect foreground window transitions to approved study applications (`UsageEvents.Event.ACTIVITY_RESUMED`). | **High Sensitivity**: Must be requested via explicit deep link to `Settings.ACTION_USAGE_ACCESS_SETTINGS`. Must provide prominent in-app disclosure prior to opening settings. Fallback handling implemented if revoked. |
| `android.permission.FOREGROUND_SERVICE` | Normal | Allows `FocusTrackingService` to run continuous tracking session. | Mandatory base permission for any Android 9+ foreground service. |
| `android.permission.FOREGROUND_SERVICE_SPECIAL_USE` | Normal / Declaration Required | Android 14+ (API 34+) specialized FGS type. | Requires declaring `<property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE" .../>` and submitting justification to Play Console during app review. Justification: "Continuous automated study session verification based on active approved educational applications." |
| `android.permission.POST_NOTIFICATIONS` | Runtime (API 33+) | Display ongoing focus timer notification with pause/resume actions and daily target celebration. | Standard runtime permission. Requested gracefully with rationale; app functions even if notification is dismissed or disabled. |
| `android.permission.READ_PHONE_STATE` | Runtime / Dangerous | Used by `CallStateWatcher` to detect incoming/outgoing cellular calls to automatically pause focus when `pauseDuringCalls = true`. | **Sensitive Permission**: Google Play enforces strict usage guidelines for telephony permissions. The app only listens to call state (`TelephonyCallback.CallStateListener` / `EXTRA_STATE_RINGING` / `EXTRA_STATE_OFFHOOK`); it **never** reads phone numbers, call logs, SIM state, or telephony identifiers. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | Normal | Allows `BootCompletedReceiver` to restore tracking state following sudden reboot or system crash. | Standard utility permission. Complies with Android 15 restrictive FGS startup constraints. |
| `android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Normal / Special Policy | Prompts user to exempt app from aggressive OEM battery killer heuristics (e.g. OnePlus/ColorOS, Xiaomi/MIUI). | **Google Play Policy Note**: Google Play restricts direct dialog invocation of `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` unless core functionality requires it. Study Companion directs users via general battery settings intent (`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` / OEM battery manager) to remain fully Play Store compliant. |

---

## 2. Foreground Services Compliance (Android 14 & 15)

1. **Service Type**: `specialUse`
2. **Subtype Property**:
   ```xml
   <property
       android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
       android:value="Automatic study session tracking based on user-approved study apps." />
   ```
3. **Android 15+ Boot Restriction Compliance**:
   Android 15 (targetSdk 35+) restricts background receivers from starting foreground services directly from `BOOT_COMPLETED` unless user initiated. Study Companion handles this gracefully:
   - When device reboots, `BootCompletedReceiver` logs a reboot boundary event, marks unclosed sessions cleanly with zero fabricated interval duration, and sets recovery state in Room.
   - If starting FGS directly fails or is restricted by Android 15 background start restrictions, the receiver posts a high-priority recovery notification allowing the user to tap once to resume tracking.

---

## 3. Usage Access Compliance (`PACKAGE_USAGE_STATS`)

- **Prominent In-App Disclosure**: Displayed in `PermissionHealthScreen` before routing to system settings:
  > *"Study Companion inspects which study app is currently open on your screen to track your study sessions automatically. We never collect what you type, read your screen content, or track unapproved apps."*
- **No Accessibility Service Abuse**: Study Companion strictly relies on Android's native `UsageStatsManager` / `UsageEvents` framework. It does **not** use an `AccessibilityService` (which Google Play strictly bans for non-disability tracking tools).

---

## 4. Google Play Data Safety Disclosures

Google Play Console requires declaring all data types collected, shared, and stored.

| Data Category | Data Type | Collected | Shared with Third Parties | Purpose | Ephemeral / Stored |
|---|---|---|---|---|---|
| **Personal Info** | Email address | Yes | No | Account management, authentication, cross-device sync | Stored in database, user-deletable |
| **Personal Info** | User IDs | Yes | No | Account identification, profile association | Stored |
| **App Activity** | App interactions / Usage | Yes (Approved packages only) | No | Analytics for study session tracking and target computation | Stored locally and synced to user's private cloud schema |
| **App Info & Performance** | Diagnostics / Crash logs | Yes (Optional user export) | No | Device troubleshooting and issue reporting | Locally generated bundle, never auto-uploaded without consent |
| **Device or other IDs** | Device ID | Yes | No | Multi-device sync deduplication and cursor reconciliation | Stored |
| **Financial / Health / Location** | Any | **No** | **No** | N/A | Never collected |

### Security Practices
- **Data Encrypted in Transit**: All network requests use TLS 1.3/HTTPS via OkHttp (`https://api.studycompanion.app`).
- **Data Encrypted at Rest**: Sensitive session credentials and tokens are held in private app storage / encrypted preferences; profile PINs are salted with PBKDF2-HMAC-SHA256 (12,000 iterations) with constant-time verification.
- **Data Deletion Mechanism**: Full account deletion and profile data wiping endpoints are provided.

---

## 5. Privacy Policy

A compliant public Privacy Policy URL must be hosted and linked in the Play Console listing.
Key privacy guarantees verified in code:
1. **Zero Keystroke / Screen Recording**: The app contains zero screen capture, OCR, or accessibility APIs.
2. **Zero Notification Payload Collection**: The app receives notification events but explicitly drops and ignores all text, sender, and payload details.
3. **Zero Plaintext Credential Logging**: All OkHttp interceptors and Timber/Logcat logs redact authorization headers, tokens, passwords, and PIN verifiers.

---

## 6. Account & Data Deletion Compliance

Google Play requires apps that support account creation to allow users to initiate account deletion both in-app and via a web URL.

- **In-App Account Deletion**:
  - Located under `SettingsScreen -> Account & Security -> Delete Account`.
  - Cascades foreign keys: `users` -> `profiles` -> `profile_settings`, `study_apps`, `daily_targets`, `study_sessions`, `session_events`, `daily_stats`.
  - Wipes Room local SQLite database and clears `UserSessionDataStore`.
- **Web-Based Deletion Request**:
  - URL: `https://studycompanion.app/account-deletion`
  - Allows requesting complete account removal and data purging within 30 days in compliance with GDPR/CCPA.

---

## 7. Store Listing Requirements

1. **App Title**: Study Companion — Focus Timer
2. **Short Description** (<= 80 chars): Automatic, distraction-free study session timer and goal tracker for learners.
3. **Full Description**:
   Highlights core benefits:
   - Automatic study tracking based on approved educational apps
   - Anti-fabrication focus engine with sub-second accuracy
   - Profiles with cryptographic PIN isolation
   - Resilient offline-first synchronization
   - Transparent battery and permission health monitoring
4. **App Category**: Education / Productivity
5. **Content Rating**: Everyone (IARC rating questionnaire)
6. **Target Audience**: 13+ (no target age under 13, avoiding COPPA/Designed for Families restrictions).

---

## 8. Known Policy Risks & Mitigations

| Identified Risk | Severity | Implemented Mitigation |
|---|---|---|
| `READ_PHONE_STATE` Play Policy Rejection | Medium | Telephony listener is strictly used to pause timer on active call. If Play Console review flags telephony permission, the permission can be made strictly optional or requested dynamically only when user enables "Pause During Calls". |
| `specialUse` FGS Declaration Review | Medium | Declarations in manifest and Play Console justification match exact usage. Video walkthrough of foreground tracking notification and approved app tracking ready for submission. |
| OnePlus / Aggressive OEM Battery Freezing | Low | `BatteryOptimizationHelper` guides user through OEM specific settings without violating Google Play's battery optimization intent policies. |

---

## 9. Required Actions for Console Submission

- [ ] Register App ID `com.studycompanion.app` on Google Play Console
- [ ] Complete Data Safety form according to Section 4
- [ ] Fill out Government Apps declaration (No)
- [ ] Fill out Financial Features declaration (No)
- [ ] Complete Content Rating (IARC) questionnaire
- [ ] Provide Foreground Service Video Demo Link for `specialUse` type
- [ ] Publish public Privacy Policy at `https://studycompanion.app/privacy`
- [ ] Publish Data Deletion instructions at `https://studycompanion.app/account-deletion`
