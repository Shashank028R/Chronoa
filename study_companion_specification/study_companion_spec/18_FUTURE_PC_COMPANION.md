# 18 — Future PC Companion

## Status

FUTURE. Do not implement V1.

## Purpose

Accurately measure laptop-based studying.

## Core idea

The Android app currently knows what happens on Android.

The PC companion would know what happens on the laptop.

Together:

```text
Android
  ↓
Phone sessions
     CLOUD
  /
 PC
  ↓
Desktop sessions
```

## PC capabilities

- active application tracking
- user-defined Study Apps
- idle detection
- session creation
- device provenance
- offline queue
- cloud sync

## Idle detection

Suggested future rule:

A desktop session may become:
- active
- idle
- resumed

The exact idle threshold should be user-configurable.

## Cross-device overlap

If phone and PC sessions overlap:
- do not simply add both durations together without a policy.

Options:
1. union time (recommended for total study time across simultaneous devices)
2. sum device time (useful as “device-hours” metric)

Store both concepts if useful:

`uniqueFocusedTime`
`deviceFocusedTime`

## Data provenance

Each session stores:
- platform
- device
- application
- start/end
- tracking method

## Future authentication

Use the same user account and device registration.

Pairing:
- login
- device approval
- optional QR pairing

## Sync

PC should reuse the same sync protocol, not create a second incompatible API.

## Security

The desktop agent observes app usage and therefore must be transparent.

It must:
- show when tracking is active
- allow stop
- never capture screen content by default
- never upload keystrokes
- never upload document contents
- never collect unrelated browsing details

## Future unified timeline

Example:

```text
09:00–10:30   VS Code       Windows
10:30–10:45   Instagram     Android  paused
10:45–12:00   Chrome        Windows
12:00–12:30   Break
12:30–13:15   Android Studio Windows

Unique focused time: 3h 15m
```
