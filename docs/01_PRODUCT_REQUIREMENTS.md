# 01 — Product Requirements

## Priority notation

- **MUST** — required for V1.
- **SHOULD** — strongly recommended for V1 unless blocked by platform limits.
- **MAY** — optional.
- **FUTURE** — do not implement in V1.

## 1. Account

### 1.1 Signup — MUST

Fields:
- email
- password
- password confirmation
- optional display name depending on auth provider

Validation:
- valid email format
- password meets backend policy
- matching passwords

States:
- loading
- validation error
- server error
- success

### 1.2 Login — MUST

Support:
- email/password

Future:
- Google sign-in
- passkeys

Do not make social login a V1 dependency.

### 1.3 Logout — MUST

Logout should:
- invalidate local authenticated state
- preserve only data that policy explicitly allows on device
- cancel or suspend sync
- require re-authentication to access protected account data

## 2. Profiles

A single account may own multiple profiles.

Each profile has:
- profile name
- optional avatar
- profile PIN/password verifier
- target settings
- Study Apps
- focus settings
- sessions
- statistics
- preferences

Switching profiles requires profile authentication.

The profile password/PIN is not the same as the cloud account password unless explicitly chosen by the user.

## 3. Daily targets

The system stores:
- original target
- adjusted target
- actual automatically tracked time
- actual manual time
- remaining target

Example:
Original = 3h
Adjusted = 4h
Automatic = 2h
Manual = 15m

The app must not silently rewrite historical targets.

## 4. Study Apps

The user can:
- browse available/visible installed apps
- mark an app as Study App
- remove it
- rename its display label locally if useful
- optionally assign category/subject metadata later

The system should prefer narrowly scoped package visibility rather than requesting broad installed-app inventory unless the Play-policy review confirms it is necessary.

## 5. Automatic tracking

Timer begins when all required conditions are true.

At minimum:
- current app is an approved Study App
- no active configured pause condition
- no unsupported/unknown tracking state requiring fail-safe pause

Timer pauses when:
- user opens unapproved app
- user goes home
- configured call interruption is active
- configured split-screen/floating state is detected
- user explicitly ends focus
- permission required for reliable tracking disappears

## 6. Manual sessions

Users can create, edit, and delete manual time.

Manual entries display an explicit indicator:
“Not automatically verified.”

## 7. History

Users can browse by:
- day
- week
- month
- calendar date

History should support:
- session detail
- automatic/manual label
- app
- device
- platform
- timestamps
- duration
- subject when present

## 8. Statistics

V1:
- total today
- total week
- total month
- target completion
- session count
- longest session
- most-used Study App
- streak

V2+:
- best study window
- subject allocation
- consistency trends
- device/platform comparisons

## 9. Settings

Focus:
- Study Apps
- locked-device behavior
- call behavior
- notification blocking
- animation preference
- focus display

Account:
- profile
- password/PIN
- logout
- sync

Privacy:
- permissions
- data export
- account deletion

## 10. Future functionality

FUTURE:
- AI study chat
- AI planner
- AI insights
- PC companion
- unified cross-device tracking
- OnePlus Live Alert
- widgets
- soundscapes
- advanced focus blocking
