# 25 — V1 Acceptance Test Matrix

## Tracking

| ID | Scenario | Expected |
|---|---|---|
| T01 | Approved app opens | FOCUSING |
| T02 | Unapproved app opens | Pause |
| T03 | Home opens | Pause |
| T04 | Return to approved app | Resume |
| T05 | Approved app remains open untouched | Continue |
| T06 | Screen locks, lock tracking ON | Continue |
| T07 | Screen locks, lock tracking OFF | Pause |
| T08 | Call begins, pause calls ON | Pause |
| T09 | Call begins, pause calls OFF | Continue if otherwise eligible |
| T10 | Notification appears | No change |
| T11 | Notification opens unapproved app | Pause |
| T12 | Split-screen detected | Pause |
| T13 | Floating window detected | Pause |
| T14 | Usage Access revoked | Automatic verification unavailable |
| T15 | Process killed during focus | Recover without duplicate time |
| T16 | Device reboots during focus | No invented reboot interval |
| T17 | Network disappears | Local tracking continues |
| T18 | Network returns | Pending sync uploads |

## Target behavior

| ID | Scenario | Expected |
|---|---|---|
| G01 | Set 3h target | Original = 3h |
| G02 | Change today's target to 4h | Original remains 3h, adjusted = 4h |
| G03 | Reduce target | Original remains unchanged |
| G04 | Miss 1h | Tomorrow not auto-increased |
| G05 | Carry-over offer | User chooses whether to apply |
| G06 | Manual time | Stored as manual/unverified |

## Profiles

| ID | Scenario | Expected |
|---|---|---|
| P01 | Create profile | Profile created |
| P02 | Switch profile without PIN | Denied |
| P03 | Correct PIN | Profile unlocked |
| P04 | Wrong PIN | Remain locked |
| P05 | Profile A views history | No Profile B data |
| P06 | Logout | Protected account state cleared |

## Data integrity

| ID | Scenario | Expected |
|---|---|---|
| D01 | Session duration | End-start |
| D02 | Duplicate sync | No duplicate record |
| D03 | Manual edit | Audit/modified state recorded |
| D04 | Delete while offline | Tombstone/queue retained |
| D05 | Timezone change | Historical timestamps preserved |
| D06 | Clock rollback | No negative duration |

## UX

| ID | Scenario | Expected |
|---|---|---|
| U01 | Home loads offline | Immediate local state |
| U02 | Missing permission | Clear recovery message |
| U03 | Focus starts | Clear state transition |
| U04 | Focus pauses | Subtle visual transition |
| U05 | Target complete | Non-intrusive celebration |
| U06 | Reduced motion | Static/low-motion UI |

## Security

| ID | Scenario | Expected |
|---|---|---|
| S01 | Wrong account requests profile | 403/denied |
| S02 | Expired token | Re-auth/refresh |
| S03 | Raw password in logs | Never present |
| S04 | Raw notification text in cloud | Never present |
| S05 | Local PIN in plaintext | Never stored |

## Release gate

All MUST tests pass on the primary physical device and at least one Pixel-class device across the supported Android versions before V1 release.
