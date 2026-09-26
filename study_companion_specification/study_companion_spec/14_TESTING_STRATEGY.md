# 14 — Testing Strategy

## Testing pyramid

```text
        UI / E2E
       /         Integration    Platform
     /                 Domain -------- Local DB
```

## Unit tests

Required:
- target calculations
- remaining time
- carry-over calculations
- state transition rules
- session duration
- date boundaries
- timezone conversion
- clock changes
- sync conflict resolver

## Focus Engine tests

Build a deterministic state-machine test harness.

Example:

```text
Initial: IDLE
Event: USER_START
Context: approved app
Expect: FOCUSING

Event: app switch to Instagram
Expect: PAUSED_UNAPPROVED_APP

Event: app switch to Chrome
Expect: FOCUSING
```

## Lock tests

Cases:
- lock while focusing + count=true → LOCKED_FOCUS
- lock while focusing + count=false → paused
- unlock to approved → focus
- unlock to home → paused

## Call tests

- call begins while focusing + pause=true
- call ends and approved app active
- call ends and home active
- call begins while already paused
- missing call permission

## Notification tests

- notification appears → no state change
- notification opens approved app → focus
- notification opens unapproved app → pause
- listener disabled → core timer still works

## Window tests

Test on:
- normal
- split-screen
- floating
- picture-in-picture where relevant

Because cross-app window-mode detection is platform-sensitive, use device tests and document observed behavior.

## Process death

Scenarios:
- kill app process while focusing
- reopen
- reconcile usage events
- no duplicate sessions

## Reboot

Scenarios:
- active session before reboot
- reboot
- boot
- reconcile without inventing time across unknown interval

## Network

- no network during study
- network returns mid-session
- push success
- push failure
- duplicate mutation
- cloud conflict

## Security

- invalid token
- expired token
- wrong profile PIN
- unauthorized profile ID
- deleted profile
- malicious sync mutation
- replayed mutation ID

## UI tests

- Home renders offline
- target edit
- Study App selection
- history edit
- profile switch
- permission recovery

## Performance

Monitor:
- CPU during tracking
- battery drain
- Room write frequency
- usage-event query frequency
- notification overhead
- memory usage

Avoid a design that runs a high-frequency busy loop.

## Device matrix

Minimum physical devices:
- OnePlus device used by project owner
- a Pixel-class Android device
- one lower/mid-range device
- Android 14
- Android 15
- Android 16

Also test OEM power-management behavior.

## Acceptance test

The V1 is not done until:

1. An approved app produces automatic time.
2. Home/unapproved app pauses.
3. Return resumes.
4. Sessions survive process death.
5. Offline operation works.
6. Cloud catch-up works.
7. Manual sessions remain marked unverified.
8. Profile data is isolated.
9. User can recover from missing permission.
10. No known major battery regression remains.
