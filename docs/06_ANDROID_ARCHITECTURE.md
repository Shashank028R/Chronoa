# 06 — Android Architecture

## Reference stack

Recommended V1 baseline as of 2026-09-22:

- Kotlin
- Jetpack Compose
- AndroidX
- Coroutines + Flow
- Room for local persistence
- DataStore for small settings
- WorkManager for deferred sync/maintenance
- Credential Manager where passwordless auth is later added
- Cloud backend behind repository interfaces
- Foreground service only where platform review validates that it is justified for the core tracking behavior

Build baseline:
- compileSdk 36
- targetSdk 36
- JDK 17
- AGP 9.4.0
- Gradle 9.6.0
- Kotlin 2.2.10

These versions are a planning baseline verified against Android's September 2026 documentation; update if the project's checked-in toolchain is intentionally newer and compatible. Android 16 is API level 36. [Android 16 SDK](https://developer.android.com/about/versions/16/setup-sdk) [AGP 9.4.0](https://developer.android.com/build/releases/agp-9-4-0-release-notes)

## Architecture style

Use a pragmatic layered architecture:

```text
UI / Compose
     ↓
ViewModel / State holders
     ↓
Use Cases
     ↓
Domain
     ↓
Repositories
  ↙       ↘
Local      Cloud
Room       Remote API
     ↘    ↙
   Sync Engine
```

Platform adapters sit below repositories/domain boundaries.

## Suggested package structure

```text
com.<company>.<app>
├── app
├── core
│   ├── common
│   ├── time
│   ├── security
│   ├── database
│   ├── network
│   └── platform
├── feature
│   ├── auth
│   ├── profile
│   ├── onboarding
│   ├── home
│   ├── focus
│   ├── history
│   ├── statistics
│   ├── studyapps
│   └── settings
├── tracking
│   ├── engine
│   ├── usage
│   ├── calls
│   ├── lockstate
│   └── windowstate
└── sync
```

Avoid premature micro-modules. A single Android app module is acceptable initially.

## Local-first rule

The app should render from local state first.

Do not:
- fetch today's dashboard from the cloud before rendering
- block the timer while a sync request is pending
- require cloud availability to persist sessions

## Usage Access

Declare `PACKAGE_USAGE_STATS` and guide the user to the appropriate Settings screen. The permission is a special access managed by the system, not a normal runtime permission. [Manifest permission](https://developer.android.com/reference/android/Manifest.permission#PACKAGE_USAGE_STATS)

## Foreground service decision

A long-running foreground service may be necessary for resilient tracking, but the exact service design must be validated during the proof-of-concept.

Android 14+ requires an appropriate foreground-service type and permission. A `specialUse` service is available for valid cases not covered by the standard types, with use-case disclosure/review. [FGS types](https://developer.android.com/develop/background-work/services/fgs/service-types)

Android 12+ also restricts starting a foreground service from the background. [FGS start restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)

Do not create an FGS merely because “background work is needed.” Document why the tracking use case needs it.

## Notification requirement

A foreground service must provide a notification. Android 13+ also has `POST_NOTIFICATIONS` runtime permission behavior, but denial does not eliminate the underlying requirement for an FGS notification. [Notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission)

Design the notification to be:
- low-noise
- truthful
- user-visible
- easy to stop where appropriate

## Battery management

Do not blindly request `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.

Instead:
1. detect aggressive restriction
2. explain why reliability can degrade
3. guide the user to vendor/system settings
4. make the tracking state honest

Android states that direct battery-optimization exemption is for unusual cases and may increase battery use. [Doze/App Standby](https://developer.android.com/training/monitoring-device-state/doze-standby)

## Screen/lock detection

Use supported system state signals and reconcile them with UsageEvents.

Do not treat a single signal as the entire truth.

## Call detection

Use Telecom/telephony APIs where available.

`TelecomManager.isInCall()` can indicate an ongoing call for managed or self-managed `ConnectionService` calls and requires `READ_PHONE_STATE`. [TelecomManager](https://developer.android.com/reference/android/telecom/TelecomManager#isInCall)

Do not promise universal proprietary app detection.

## Notification blocking

Optional feature:
`NotificationListenerService`.

This requires user-granted notification access and is separate from `POST_NOTIFICATIONS`.

The service can receive notification events. [NotificationListenerService](https://developer.android.com/reference/android/service/notification/NotificationListenerService)

This capability requires strong user disclosure and privacy handling.

## Accessibility

Do not use AccessibilityService as the default foreground-app tracking mechanism.

Android states accessibility services are intended to assist users with disabilities, and Google Play has separate policy requirements for non-accessibility uses. [AccessibilityService](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService) [Play policy](https://support.google.com/googleplay/android-developer/answer/10964491)

## Package visibility

Minimize package visibility.

The app should ideally query only what is necessary to present selectable Study Apps. Avoid `QUERY_ALL_PACKAGES` unless policy and core-functionality review genuinely require it. Google Play treats installed app inventory as sensitive. [Play policy](https://support.google.com/googleplay/android-developer/answer/16558241)

## Lifecycle

The app must survive:
- Activity recreation
- process death
- device rotation
- app UI closed
- device reboot
- network loss
- permission changes

Core state belongs in durable storage, not ViewModel memory.

## Time

Store UTC instants internally.
Convert to local time only at UI boundaries.

Store the user's timezone identifier for calendar semantics.

On timezone change:
- write a `TIMEZONE_CHANGED` event
- do not silently move historical sessions between calendar days

## Android 16 compatibility

Target API 36.
Support edge-to-edge.
Migrate to predictive back.
Test large-screen/window behavior.

Android 16 requires edge-to-edge for target 36 and changes back navigation behavior. [Android 16 behavior changes](https://developer.android.com/about/versions/16/behavior-changes-16)

## Security

- Keystore for local secrets
- no plaintext passwords
- HTTPS for network
- short-lived access tokens
- refresh token protection
- server-side authorization
- no sensitive logs in release
- encrypted backup/export where secrets are involved
