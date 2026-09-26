# 12 — Notifications & Interruptions

## Notification categories

1. App's own study/focus notification
2. Other apps' notifications
3. Optional notification blocking

They must be architecturally separate.

## Incoming notification

A notification appearing:
- MUST NOT pause study tracking.

Reason:
The user may simply see it without interacting.

## Notification opened

Opening a notification typically results in a foreground activity change.

The Focus Engine then applies normal rules.

No special notification-content dependency is required.

## Optional blocking

A user setting:

`Block notifications during focus`

When OFF:
- do nothing.

When ON and supported:
- use NotificationListenerService only for the configured blocking feature.
- do not upload notification contents.
- do not store notification text.
- allow user to disable the access at any time.

Android's NotificationListenerService receives notification posted/removed callbacks. [API reference](https://developer.android.com/reference/android/service/notification/NotificationListenerService)

## Permission and disclosure

If notification access is required:
- explain it before opening settings
- state exactly what the service can observe
- state that notification content is not sent to the cloud for the focus feature
- provide a disable path

## Calls

### Required user setting

`Pause timer during calls`

Default:
ON (recommended; can be changed)

### Call states

- incoming/ringing
- dialing
- active
- holding

The engine should treat “call in progress” as a boolean interruption condition for V1 unless the product later needs detailed call state.

### API strategy

Use `TelecomManager.isInCall()` plus telephony callbacks where useful.

`TelecomManager.isInCall()` covers ongoing phone calls from managed or self-managed ConnectionService implementations, with `READ_PHONE_STATE`. [Android TelecomManager](https://developer.android.com/reference/android/telecom/TelecomManager#isInCall)

### VoIP limitation

Do not claim “all possible app calls” are guaranteed.

Some proprietary apps may not expose enough state through public system APIs.

If a required calling app is not observable:
- report the limitation
- do not silently fabricate a pause

## Lock screen

Lock/unlock is not treated as a notification.

If count-while-locked = ON:
- continue eligible session

If OFF:
- pause.

## Focus notification

If the tracker uses a foreground service, its service notification should expose:
- current state
- focused time
- stop action

Avoid a constantly changing notification every second if that harms performance.

## Permission loss notification

If Usage Access is lost:
- show a recovery notification only when appropriate
- do not spam
- include a direct action to Settings where supported
