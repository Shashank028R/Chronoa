# 02 — Feature Specification

## Feature F01 — Onboarding

### Purpose
Convert a new installation into a configured tracking system with the minimum necessary setup.

### Flow

Install
→ Welcome
→ Sign up / Login
→ Create/select profile
→ Set default daily target
→ Choose Study Apps
→ Configure focus rules
→ Grant permissions/access
→ Tracking readiness check
→ Home

### Acceptance criteria

- No core feature is silently enabled without explaining it.
- Each permission has a plain-language purpose.
- User can postpone non-critical permissions.
- Core automatic tracking clearly reports what is missing.

## F02 — Home

The Home screen is intentionally minimal.

Primary content:
- greeting
- today's tracked duration
- today's target
- remaining target
- current status
- current Study App, when active

Primary actions:
- Start Today's Study / Focus
- Edit Today's Target

Secondary:
- basic stats
- history
- statistics
- Study Apps
- settings

Avoid a card-heavy dashboard.

## F03 — Focus View

Full-screen focus experience.

V1:
- minimal timer
- optional AMOLED mode
- current state
- exit/back control
- optional display mode selector

V2:
- ambient
- nature
- space
- sounds
- advanced themes

## F04 — Study App Manager

Show:
- app icon
- app name
- selected/unselected state
- optional category

Actions:
- select
- deselect
- search/filter
- review required access

## F05 — Target Editor

Fields:
- today's target
- original target display
- adjusted target
- optional carry-over

Rule:
Original target is immutable for historical record. Adjustments are layered on top.

## F06 — Session Editor

Automatic session:
- start
- end
- duration
- app
- source
- verification

User can:
- edit
- delete

When edited:
Record an audit event.
If the modification invalidates the original automatic values, label it as modified.

## F07 — Manual Session

Required fields:
- date
- start
- end
- optional subject

Show warning:
“Manual time is not automatically verified.”

## F08 — Permission Health

Status rows:
- Usage Access
- Notifications
- Call detection
- Battery restrictions
- Tracking service

Each row provides:
- current status
- why needed
- system settings shortcut where possible

## F09 — Cloud Sync

Visible sync state:
- Synced
- Syncing
- Offline
- Needs attention

Never block local tracking because sync is unavailable.

## F10 — Optional notification blocking

This is separate from timer logic.

When disabled:
- notifications behave normally.

When enabled and technically supported:
- app attempts to suppress configured notifications during an active focus session.
- timer state remains based on app foreground rules.

This feature must include a privacy disclosure and user-controlled enablement.

## F11 — Streaks

A day counts toward streak according to a configurable completion rule.

Recommended V1 rule:
A streak day is achieved when automatic study time reaches the user's adjusted target.

Manual-only completion should be visibly distinct.

## F12 — Future AI chat

FUTURE:
A contextual assistant that can query structured study history and produce:
- progress answers
- trend explanations
- study planning
- remaining workload calculations

No AI code in V1.
