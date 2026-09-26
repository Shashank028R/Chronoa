# 05 — Focus Engine

## Purpose

The Focus Engine is the single authoritative subsystem for deciding whether time qualifies as automatic study time.

No UI component, database repository, or future Live Alert should independently invent timer state.

## Core principle

```text
SYSTEM EVENTS
     ↓
EVENT NORMALIZER
     ↓
CURRENT CONTEXT
     ↓
RULE EVALUATOR
     ↓
FOCUS STATE MACHINE
     ↓
SESSION EVENT WRITER
     ↓
LOCAL DATABASE
     ↓
UI / NOTIFICATION / FUTURE LIVE ALERT
```

## Required inputs

- foreground app package
- activity resumed/paused events
- screen interactive state
- keyguard state
- call state
- multi-window status where available
- floating-window status where available
- active profile
- Study App configuration
- focus settings
- permission readiness
- manual start/stop state

## State model

Recommended states:

```text
IDLE
READY
FOCUSING
LOCKED_FOCUS
PAUSED_UNAPPROVED_APP
PAUSED_HOME
PAUSED_CALL
PAUSED_MULTIWINDOW
PAUSED_FLOATING
PAUSED_USER
WAITING_FOR_PERMISSION
RECOVERING
ERROR
```

Keep the persisted state minimal; derive transient UI state where practical.

## Rule priority

When multiple conditions conflict, explicit interruptions take priority.

Recommended precedence:

1. Engine error / missing critical permission
2. User explicit stop
3. Unsupported/unknown app context
4. Active call when pause-during-calls = true
5. disallowed multi-window/floating state
6. lock state rule
7. foreground package rule
8. otherwise paused

The evaluator should be deterministic.

## Event types

At minimum:

```text
APP_RESUMED
APP_PAUSED
KEYGUARD_SHOWN
KEYGUARD_HIDDEN
SCREEN_INTERACTIVE
SCREEN_NON_INTERACTIVE
CALL_STARTED
CALL_ENDED
WINDOW_MODE_CHANGED
PROFILE_SWITCHED
STUDY_APP_CHANGED
RULES_CHANGED
PERMISSION_CHANGED
USER_START
USER_STOP
DEVICE_RESTART
CLOCK_CHANGED
TIMEZONE_CHANGED
SYNC_COMPLETED
ENGINE_RECOVERED
```

## Session model

A session is an interval during which the engine stays in an eligible automatic state.

When the state changes from eligible → ineligible:

1. close the current interval
2. write a state transition event
3. persist the session
4. recalculate daily statistics asynchronously but locally

When ineligible → eligible:

1. start a new interval/session
2. write event
3. expose live state

Do not keep one huge session across pauses; separate intervals make correction and debugging much easier.

## Timing correctness

Use both:
- wall-clock timestamps for historical meaning
- monotonic elapsed timing for in-process UI rendering where available

Never trust a device clock jump to create impossible durations.

If the wall clock changes:
- persist `CLOCK_CHANGED`
- close/reconcile the current interval
- prevent negative or duplicated duration

## Android usage events

Android provides `ACTIVITY_RESUMED` and `ACTIVITY_PAUSED` usage events on API 29+. The implementation should use recent event queries and calculate transitions from event timestamps. [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)

The system must not assume a callback fires exactly at the moment the UI changes.

Recommended strategy:
- event query loop at a modest cadence while tracking is active
- use the event timestamps as authoritative transition times
- periodically backfill/reconcile recent events
- after process restart, reconstruct the current state from recent events

## “Immediate pause” product behavior

The user-facing requirement is “pause immediately.”

Implementation interpretation:
- resolve the app switch as soon as the system event becomes observable
- timestamp the transition using the originating system event time where valid
- update UI immediately after processing
- if processing is delayed by the OS, do not fabricate elapsed time; reconcile using event timestamps

## Lock-screen rule

If count-while-locked = ON:
- a valid Study App session may remain eligible while the device is locked
- enter `LOCKED_FOCUS`
- close it when unlock reveals an ineligible context

If OFF:
- close/pause the session when lock becomes effective

## Call rule

The engine receives a boolean “call in progress” capability from the call detector.

If pause-during-calls = true:
- call starts → `PAUSED_CALL`
- call ends → re-evaluate all conditions before resuming

The detector should use Telecom/telephony APIs that are available and permitted. No universal proprietary VoIP guarantee should be claimed.

## Notification rule

Notification posted:
- no engine state change.

Notification opened:
- package/activity transition is evaluated as a normal foreground change.

The engine does not need notification content to understand a foreground app switch.

## Split-screen/floating rule

Requirement:
- pause when split-screen or floating window is detected.

Technical constraint:
- `Activity.isInMultiWindowMode()` describes the app's own activity and is not a general cross-app observer. [Activity API](https://developer.android.com/reference/android/app/Activity#isInMultiWindowMode)

V1 implementation:
- build a device matrix and validate what can be reliably observed using supported APIs.
- never claim universal cross-app window-mode detection until proven.

Fallback:
- if strict detection cannot be guaranteed, show a capability limitation and use conservative observable behavior rather than a false promise.

Do not add AccessibilityService simply to “make it work” without a separate policy/security review.

## Process death

On restart:
1. load last known engine state
2. load last persisted event timestamp
3. query recent UsageEvents
4. reconcile from last known point
5. determine current package
6. determine lock/call state
7. create a new session if eligible
8. persist recovery event

## Device reboot

A reboot is a hard boundary for in-memory state.

Persist:
- current session ID
- last processed usage-event timestamp
- last engine state

After boot:
- recover
- never infer study time across an unknown reboot interval
- restart tracking only after capability checks

## Permission loss

Critical:
Usage Access.

If revoked:
- automatic tracking must stop being described as verified
- show recovery prompt
- manual logging remains available

## Idempotency

All session-event writes should be idempotent.

Generate a unique event ID from:
- device ID
- profile ID
- event timestamp
- event type
- source sequence/random suffix

The system should tolerate reprocessing.

## Debugging

Provide a developer-only event timeline:

```text
12:01:04 APP_RESUMED chrome
12:01:04 RULES -> eligible
12:01:04 FOCUSING
12:43:11 APP_PAUSED chrome
12:43:11 APP_RESUMED instagram
12:43:11 RULES -> ineligible
12:43:11 PAUSED_UNAPPROVED_APP
```

This should be disabled/hidden in production unless explicitly exposed as user support tooling.

## Timer display

The display derives from persisted session boundaries + current state.

It must not own authoritative timing data.

## Future adapters

The same engine interface should later accept:
- Android source
- Windows source
- OnePlus Live Alert consumer
- AI analytics consumer

No future component should bypass the engine.
