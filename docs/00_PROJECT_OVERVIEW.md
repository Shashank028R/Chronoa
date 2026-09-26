# 00 — Project Overview

## Product statement

A personal study companion that automatically tracks study time according to user-defined Study Apps and configurable device conditions.

## Core promise

The application reduces timer babysitting.

Instead of repeatedly tapping Start/Pause:

> Configure what counts as study once, then let the system build the study timeline.

## Problem

Typical study timers measure elapsed time, not actual app-context behavior. This creates friction when:

- the user forgets to stop the timer,
- the user opens social media,
- the user switches to the home screen,
- the user takes an unplanned phone break,
- the user studies across long sessions,
- the user primarily studies on a laptop but uses the phone for reference.

This product addresses the phone side of the problem.

## Important limitation

The Android V1 does not verify cognitive engagement. It verifies configured device/app conditions.

The product should never say:

> “We know you were studying.”

It should say:

> “Automatically tracked according to your Study App and focus rules.”

## Product pillars

1. Automatic
2. User-controlled
3. Offline-first
4. Honest measurement
5. Minimal and calm
6. Privacy-aware
7. Extensible

## V1 success criteria

A new user should be able to:

1. Create an account.
2. Create and secure a profile.
3. Set a daily target.
4. Select Study Apps.
5. Grant required access.
6. Start a study day.
7. Leave a Study App and see tracking pause.
8. Return and see tracking resume.
9. Review exact sessions.
10. Edit/add sessions manually.
11. Use the core app offline.
12. Synchronize once connectivity returns.

## Non-goals for V1

- AI chat
- AI planning
- PC tracking
- OnePlus Live Alert
- social feeds
- complex community features
- advertising-led experience
- automatic judgment of “productivity”
- universal guarantee of all VoIP-call detection
- broad AccessibilityService dependency

## Product vocabulary

**Study App:** An application explicitly marked by the user as eligible to generate automatic study time.

**Tracked focus:** Time that satisfies all active focus rules.

**Automatic session:** A session derived from the tracking engine.

**Manual session:** A user-created or user-corrected session not fully verifiable by automatic rules.

**Verified by rules:** The system observed the conditions required by the configured tracking rules.

**Target:** The intended daily study duration.

**Original target:** The target initially set for that day.

**Adjusted target:** A later user change to that day's target.

**Carry-over offer:** A non-automatic suggestion to add unfinished target time to another day.

**Interruption:** A state that temporarily invalidates automatic tracking.

**Focus Engine:** The authoritative state machine responsible for tracking decisions.

## Long-term vision

Android + PC + cloud should eventually produce one study timeline across devices while preserving clear provenance.
