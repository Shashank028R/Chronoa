# 22 — Android Platform References

This file records the current platform facts used to shape the architecture.

## Usage Access

`PACKAGE_USAGE_STATS` provides access to component usage statistics and is granted through Settings.  
Reference: https://developer.android.com/reference/android/Manifest.permission#PACKAGE_USAGE_STATS

## UsageEvents

`UsageStatsManager.queryEvents()` exposes recent `UsageEvents`. `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED` identify activity foreground/background transitions on API 29+.  
Reference: https://developer.android.com/reference/kotlin/android/app/usage/UsageEvents.Event

Important:
- Usage events are historical system events, not a guaranteed direct callback API.
- Events are retained for a limited number of days.
- The app should reconcile recent event windows after process recovery.

## Foreground services

Android requires careful use of foreground services. Android 14+ requires an appropriate foreground-service type. Android 12+ restricts background starts.

References:
- https://developer.android.com/develop/background-work/services
- https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start
- https://developer.android.com/develop/background-work/services/fgs/service-types

## Special-use foreground service

Android provides `specialUse` for valid long-running foreground-service cases not covered by other types, with use-case disclosure/review.

Reference:
https://developer.android.com/about/versions/14/changes/fgs-types-required

This does NOT mean the app is automatically entitled to use it. The exact service architecture must be justified and tested.

## Notifications

Android 13+ has runtime notification permission `POST_NOTIFICATIONS`.
Reference:
https://developer.android.com/develop/ui/compose/notifications/notification-permission

A foreground service still requires an ongoing notification even if notification drawer behavior differs.

## Notification access

`NotificationListenerService` receives notification lifecycle events after user-granted access.
Reference:
https://developer.android.com/reference/android/service/notification/NotificationListenerService

## Calls

`TelecomManager.isInCall()` can identify ongoing calls through managed/self-managed ConnectionService integrations and requires `READ_PHONE_STATE`.
Reference:
https://developer.android.com/reference/android/telecom/TelecomManager#isInCall

This is not evidence that every proprietary communication app will be observable in every circumstance.

## Multi-window

`Activity.isInMultiWindowMode()` reports multi-window mode for the calling activity.
Reference:
https://developer.android.com/reference/android/app/Activity#isInMultiWindowMode

This does not provide a universal cross-app “the foreground app is in split-screen” API.

Therefore strict cross-app split-screen/floating enforcement requires a separate proof of concept.

## Accessibility

Android's AccessibilityService API is designed for accessibility services. Google Play requires special disclosure/declaration treatment for many non-accessibility uses.

References:
- https://developer.android.com/reference/android/accessibilityservice/AccessibilityService
- https://support.google.com/googleplay/android-developer/answer/10964491

V1 should avoid this dependency unless later validation establishes a compliant and necessary use case.

## Battery optimization

Android provides battery-optimization APIs, but direct exemption requests are discouraged except for acceptable core use cases because of battery impact and policy concerns.

Reference:
https://developer.android.com/training/monitoring-device-state/doze-standby

## Installed apps / package visibility

Google Play treats installed app inventory as sensitive. Prefer targeted package visibility and avoid `QUERY_ALL_PACKAGES` unless the core feature genuinely requires it and policy allows it.

Reference:
https://support.google.com/googleplay/android-developer/answer/16558241

## Android 16 / API 36

Android 16 is API 36.

Targeting Android 16 requires attention to:
- edge-to-edge
- predictive back
- new behavior changes
- large-screen/window behavior

References:
- https://developer.android.com/about/versions/16/setup-sdk
- https://developer.android.com/about/versions/16/behavior-changes-16

## Build tooling

As of 2026-09-22, Android Gradle Plugin 9.4.0 supports API level 37 and uses Gradle 9.6.0/JDK 17 as the documented baseline.

Reference:
https://developer.android.com/build/releases/agp-9-4-0-release-notes

## Credential Manager

For future authentication improvements:
https://developer.android.com/identity/sign-in/credential-manager-siwg
