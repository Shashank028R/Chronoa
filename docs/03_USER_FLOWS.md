# 03 — User Flows

## Flow 1 — First launch

```text
Splash
  ↓
Welcome
  ↓
Login / Signup
  ↓
Create Profile
  ↓
Set Daily Target
  ↓
Select Study Apps
  ↓
Focus Rules
  ↓
Permissions
  ↓
Readiness Check
  ↓
Home
```

## Flow 2 — Automatic study

```text
Home
  ↓
User starts today's focus (optional UX action)
  ↓
Tracking engine active
  ↓
Approved app detected
  ↓
FOCUSING
  ↓
App switch event
  ├── approved app → continue
  └── unapproved/home → pause
  ↓
Return to approved app
  ↓
FOCUSING
  ↓
End of day / explicit stop
```

## Flow 3 — App switch

```text
FOCUSING
  ↓
ACTIVITY_PAUSED / new ACTIVITY_RESUMED event
  ↓
Resolve current package
  ↓
Is package approved?
  ├── YES → remain FOCUSING
  └── NO → PAUSE
```

The transition timestamp should come from the system event timestamp when possible.

## Flow 4 — Screen lock

```text
FOCUSING
  ↓
KEYGUARD_SHOWN
  ↓
Count-while-locked?
  ├── ON → continue session under LOCKED_FOCUS
  └── OFF → close/pause session
```

On unlock:

```text
KEYGUARD_HIDDEN
  ↓
Resolve current foreground state
  ↓
Approved + valid window → FOCUSING
Otherwise → PAUSED
```

## Flow 5 — Call

```text
FOCUSING
  ↓
Call becomes active
  ↓
Pause-during-calls?
  ├── ON → PAUSED_CALL
  └── OFF → continue if all other rules are valid
```

After call:

```text
Call ends
  ↓
Resolve current app/window/lock state
  ↓
Resume only if all required conditions are true
```

## Flow 6 — Notification

```text
FOCUSING
  ↓
Notification posted
  ↓
NO TIMER CHANGE
```

User opens notification:

```text
Notification action
  ↓
New foreground app event
  ↓
Apply Study App rules
```

## Flow 7 — Manual session

```text
History
  ↓
Add Session
  ↓
Enter date/start/end
  ↓
Optional subject
  ↓
Show verification warning
  ↓
Save
  ↓
Recalculate statistics
  ↓
Mark trackingType = MANUAL
```

## Flow 8 — Target edit

```text
Home
  ↓
Edit Today's Target
  ↓
Display original target
  ↓
User enters new target
  ↓
Save adjustment
  ↓
Keep original target intact
  ↓
Recalculate remaining time
```

## Flow 9 — Profile switch

```text
Settings/Profile
  ↓
Switch Profile
  ↓
Select profile
  ↓
Enter PIN/password
  ↓
Verify
  ├── fail → remain locked
  └── success → load profile context
```

Never expose another profile's study data while authentication is pending.

## Flow 10 — Offline

```text
Network unavailable
  ↓
Local DB continues
  ↓
Track sessions
  ↓
Store pending sync mutations
  ↓
Network returns
  ↓
Sync pending changes
  ↓
Resolve conflicts
  ↓
Mark synchronized
```

## Flow 11 — Permission loss

```text
Required access revoked
  ↓
Tracking engine marks capability unavailable
  ↓
Persist an event
  ↓
Stop automatic verification
  ↓
Display recovery action
  ↓
User grants access again
  ↓
Rehydrate current state
```
