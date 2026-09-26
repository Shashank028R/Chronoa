# Requirements Review

**Review date:** 2026-09-22  
**Status:** Reviewed baseline for V1 planning

## Executive assessment

The product concept is technically feasible as an Android study-time tracker, but some user-visible requirements need carefully defined fallbacks because Android does not expose a single universal API that guarantees all desired conditions.

The strongest foundation is:

- user-selected Study Apps
- Android Usage Access (`PACKAGE_USAGE_STATS`)
- usage events (`ACTIVITY_RESUMED` / `ACTIVITY_PAUSED`)
- a durable event-driven session engine
- Room/local persistence
- cloud synchronization
- explicit interruption rules
- graceful handling of permission loss and OEM power management

The most important technical risks are:

1. **Cross-app foreground detection is event/history based, not a simple universal live callback.** The implementation should query recent usage events frequently enough for the UI while using event timestamps as the source of truth. Android documents `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED` events beginning in API 29. Usage events are retained only for a limited period. [Android UsageEvents](https://developer.android.com/reference/android/app/usage/UsageEvents)
2. **Usage access is special access, not a normal runtime permission.** `PACKAGE_USAGE_STATS` is granted through Settings. [Android permission reference](https://developer.android.com/reference/android/Manifest.permission#PACKAGE_USAGE_STATS)
3. **Split-screen/floating-window detection for another app is not guaranteed through the same public API used for foreground app detection.** `Activity.isInMultiWindowMode()` reports the state of the calling app's own activity. It cannot by itself tell this app that an unrelated app is in multi-window mode. A fallback/validation strategy is therefore required. [Activity API](https://developer.android.com/reference/android/app/Activity#isInMultiWindowMode)
4. **“Every call” is a product requirement, but API coverage varies by calling integration.** `TelecomManager.isInCall()` can report ongoing calls managed by Telecom, including managed or self-managed `ConnectionService` calls, but not every possible proprietary in-app communication scenario is guaranteed to expose an equivalent integration. Treat unsupported VoIP integrations as a documented limitation rather than silently claiming universal detection. [TelecomManager](https://developer.android.com/reference/android/telecom/TelecomManager#isInCall)
5. **AccessibilityService should not be used as a casual workaround.** Android says accessibility services should assist users with disabilities, and Google Play requires declarations/disclosures for other uses. For this product, AccessibilityService is a high-risk dependency and is not the default V1 strategy. [Android AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) · [Google Play Accessibility policy](https://support.google.com/googleplay/android-developer/answer/10964491)
6. **A long-running foreground service is subject to Android rules, including foreground-service type declarations and background-start restrictions.** Android 14+ requires a declared FGS type, while Android 12+ restricts background starts. `specialUse` exists for valid foreground-service use cases not covered by standard types, but Play review applies. [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types) · [FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
7. **Android 13+ notification permission affects app notifications.** A foreground service must still provide a notification even when `POST_NOTIFICATIONS` is denied; the user experience differs. [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)
8. **OEM battery management can affect reliability.** The application should diagnose battery/background restrictions and guide the user, but should not assume it can always bypass them. Android explicitly warns that direct battery-optimization exemptions should be used only for acceptable core use cases. [Doze/App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)

## Confirmed product requirements

These are treated as MUST requirements:

- Android first.
- Login/signup.
- Multiple profiles under an account.
- Profile switching requires a password/PIN.
- Daily study target with per-day edits.
- Original target is preserved when the user adjusts today's target.
- User chooses Study Apps.
- Approved Study App in foreground counts.
- Unapproved app pauses.
- Home screen pauses.
- Lock-screen behavior is ON by default and configurable.
- Calls can be configured to pause.
- Notification appearance does not pause the timer.
- Opening a notification follows normal app-switch rules.
- Split-screen/floating-window use should pause when reliably detectable.
- Manual sessions are allowed but visibly unverified.
- Offline core operation is required.
- Cloud sync is required.
- AI is future scope.
- PC companion is future scope.
- OnePlus Live Alert is future scope.
- UI must be minimal, premium, clean, and Apple-inspired.
- Focus display modes are required.
- Automatic timer tracking is the core differentiator.

## Recommended improvements

### R1 — Treat event timestamps as truth

Do not increment a database “seconds counter” every second and call that authoritative. Instead, persist state transitions and calculate durations from monotonic/epoch timestamps plus verified state.

### R2 — Distinguish “tracking confidence”

Automatic time can be recorded as `AUTOMATIC` and `VERIFIED_BY_RULES`. That means the application observed the configured device/app state; it does not mean the user was intellectually focused.

### R3 — Permission health center

Add a small system-health page showing:

- Usage Access: OK / Missing
- Notifications: Allowed / Disabled
- Call-state capability: Available / Restricted
- Battery optimization: Standard / Restricted
- Tracking engine: Running / Recovering / Disabled

### R4 — Fail visibly, not silently

If a required permission is removed, the home screen should show a clear state such as:

> “Automatic tracking is paused because Usage Access is disabled.”

### R5 — Keep V1 data rich enough for future AI and PC sync

Store source device, platform, package, session type, verification, timestamps, and event provenance.

## Decisions required before implementation

These are not product questions that need the user's imagination; they are engineering selections that should be frozen in the implementation plan:

- Final product name.
- Final Android package/application ID.
- Exact cloud provider.
- Whether cloud sync backend is fully managed (recommended for speed) or a custom API/backend.
- Supported Android minimum SDK after device testing.
- Exact background execution strategy after a proof-of-concept.
- Exact strategy for strict split-screen/floating-window enforcement.
- Whether the first Play Store build will enable optional notification blocking.
- Whether call detection should rely only on Telecom APIs for Play-safe V1 or add optional vendor/app integrations later.

## Research caveat

Platform behavior changes over Android releases and OEM software. Every implementation phase that touches Android background execution should re-check the current official Android documentation and test on physical devices.


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
