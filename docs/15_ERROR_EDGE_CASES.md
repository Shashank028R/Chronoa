# 15 — Error & Edge Cases

## Category A — Time

### Clock moves backward
Action:
- close/reconcile current interval
- record CLOCK_CHANGED
- prevent negative duration

### Clock moves forward
Do not automatically award the gap unless supported by verified usage events.

### Midnight while focusing
Close current day interval at midnight.
Start a new day context without losing continuous wall-clock history.

### Daylight saving/timezone change
Store UTC instants; derive local dates using stored timezone rules.

## Category B — Apps

### Study App uninstalled
Keep historical sessions.
Mark the app unavailable in the manager.

### App renamed
Historical package identity remains stable.

### Multiple activities in one Study App
Do not treat activity switch within the same approved package as a pause by default.

### System UI
System Settings, launcher/home, quick settings:
- generally ineligible unless explicitly designed otherwise
- quick settings panel alone should not pause unless the resulting foreground/system state indicates it should

## Category C — Permissions

### Usage Access revoked
- pause automatic verification
- manual logging remains
- show recovery

### Notification permission denied
Core timer still functions.
Only notification UX is affected.

### Notification listener revoked
Core timer still functions.
Blocking feature becomes unavailable.

### Phone permission unavailable
Core timer follows call-detection capability state and tells the user what is not guaranteed.

## Category D — Service/process

### Process killed
Recover from durable state + usage events.

### Service stopped
Show tracking-health state.
Recover when allowed.

### Device reboot
Do not invent elapsed time during the reboot window.

## Category E — Profiles

### Delete current profile
Require confirmation.
Switch to profile chooser/onboarding state.

### Wrong PIN
Do not reveal whether another profile's password is correct.
Rate-limit attempts locally.

### Rapid profile switching
Cancel old profile listeners/repositories before loading new context.

## Category F — Sessions

### Zero-duration session
Reject unless used internally for a state boundary.

### Overlapping manual session
Warn and require resolution or clearly show overlap.

### Manual edit overlaps automatic session
Preserve both but warn.
Do not silently double count.

Recommendation:
Provide a conflict UI:
“Two sessions overlap by 20 minutes.”

### User deletes automatic session
Delete from product history but keep a soft-delete/audit signal until sync completes.

## Category G — Sync

### Duplicate upload
Must be idempotent.

### Server rejects record
Store error.
Do not discard local data.

### Conflict
Apply documented entity-specific rule.

### User logs out offline
Decide whether pending sync must be blocked until re-auth.
Recommendation: protect the queue; do not silently upload under a different account.

## Category H — Background restrictions

### OEM kills background process
Show tracking health.
Guide user toward supported background/battery settings.

### Battery saver
Prefer accurate capability state over false continuous tracking.

### Doze
Test. Do not assume timers update every second in background.

## Category I — Unsupported platform capability

When a requested condition cannot be reliably observed:
- never fake success
- expose limitation
- choose conservative behavior where practical
- keep the core timer functional

## Error copy

Prefer:

> “Automatic tracking is unavailable because Usage Access was turned off.”

Avoid:

> “Something went wrong.”

## Diagnostics

Provide an internal support bundle containing:
- app version
- Android version
- capability status
- last engine state
- recent non-sensitive event codes

Never include secrets.
