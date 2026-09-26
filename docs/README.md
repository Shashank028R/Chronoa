# Study Companion — Product & Engineering Specification

**Status:** Pre-implementation master specification  
**Working product name:** TBD  
**Package/application ID:** TBD  
**Primary platform:** Android  
**Future platforms:** Windows/PC companion  
**Audience:** Human developers + AI coding agents (especially Antigravity)

## Purpose

This repository is the implementation blueprint for a study productivity application whose central capability is automatic study-time tracking based on user-selected Study Apps and configurable device conditions.

The repository intentionally separates:

- confirmed product requirements
- technical architecture
- platform constraints
- implementation phases
- testing
- future functionality
- AI-agent operating rules

No application source code is included here.

## Source-of-truth rule

For implementation, Antigravity should treat:

1. `REQUIREMENTS_REVIEW.md`
2. `01_PRODUCT_REQUIREMENTS.md`
3. `02_FEATURE_SPECIFICATION.md`
4. `05_FOCUS_ENGINE.md`
5. `06_ANDROID_ARCHITECTURE.md`
6. `08_DATABASE_SCHEMA.md`
7. `10_AUTHENTICATION_SECURITY.md`
8. `11_OFFLINE_SYNC.md`
9. `15_ERROR_EDGE_CASES.md`
10. `16_DEVELOPMENT_ROADMAP.md`
11. `20_ANTIGRAVITY_IMPLEMENTATION_GUIDE.md`
12. `21_CURRENT_STATUS.md`

as the core implementation references.

Where two documents disagree, stop and reconcile the docs before coding.

## Current scope

V1 is intentionally focused on reliable tracking, profiles, targets, Study App configuration, session history, basic statistics, local/offline behavior, cloud synchronization, and a polished minimal Android UI.

AI chat, PC companion tracking, OnePlus Live Alert, and advanced productivity systems remain future phases.


## Primary references reviewed

- Android `UsageStatsManager` / `UsageEvents`: https://developer.android.com/reference/kotlin/android/app/usage/UsageStatsManager
- Android `UsageEvents.Event`: https://developer.android.com/reference/android/app/usage/UsageEvents.Event
- Android `Manifest.permission.PACKAGE_USAGE_STATS`: https://developer.android.com/reference/android/Manifest.permission#PACKAGE_USAGE_STATS
- Android background/foreground services: https://developer.android.com/develop/background-work/services
- Android foreground-service launch restrictions: https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- Android foreground-service types: https://developer.android.com/develop/background-work/services/fgs/service-types
- Android 14 foreground-service type requirements: https://developer.android.com/about/versions/14/changes/fgs-types-required
- Android `Activity.isInMultiWindowMode()`: https://developer.android.com/reference/android/app/Activity#isInMultiWindowMode()
- Android `TelecomManager.isInCall()`: https://developer.android.com/reference/android/telecom/TelecomManager#isInCall()
- Android notification listener: https://developer.android.com/reference/android/service/notification/NotificationListenerService
- Android 13 notification permission: https://developer.android.com/develop/ui/compose/notifications/notification-permission
- Android battery optimization guidance: https://developer.android.com/training/monitoring-device-state/doze-standby
- Google Play Accessibility API policy: https://support.google.com/googleplay/android-developer/answer/10964491
- Google Play sensitive permissions/data policy: https://support.google.com/googleplay/android-developer/answer/16558241
- Android 16 setup/API 36: https://developer.android.com/about/versions/16/setup-sdk
- Android 16 behavior changes: https://developer.android.com/about/versions/16/behavior-changes-16
- Android Gradle Plugin 9.4.0: https://developer.android.com/build/releases/agp-9-4-0-release-notes

Research date: 2026-09-22.
