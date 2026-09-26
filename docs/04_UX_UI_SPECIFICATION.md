# 04 — UX/UI Specification

## Visual direction

Keywords:
- minimal
- premium
- calm
- Apple-inspired
- precise
- spacious
- subtle
- high-quality motion

The app should not look like a productivity SaaS dashboard.

## Home composition

### Top
- lightweight greeting
- profile access

### Center
- large focused time
- target
- remaining
- status indicator

### Primary action
One clear Start/Focus action.

Secondary:
- Edit Today's Target

### Bottom
Basic statistics and simple navigation.

Suggested structure:

```text
Good evening
Shashank

02:14:32
focused today

2h 14m / 3h
46m remaining

● STUDYING
Android Studio

[ Focus View ]

[ Edit Today's Target ]

────────────────────
Today     Sessions     Streak
2h14m        4           7

History   Statistics
Study Apps    Settings
```

The exact visual hierarchy may change during design implementation, but the minimalism requirement must not.

## Status language

Examples:
- `● STUDYING`
- `○ PAUSED`
- `○ NOT STUDYING`
- `! TRACKING NEEDS ATTENTION`

Avoid guilt-oriented text.

Bad:
- “You wasted 45 minutes.”
- “You failed today.”

Good:
- “Tracking paused.”
- “45m remaining.”
- “Manual time is not automatically verified.”

## Focus View

### Minimal AMOLED

Pure/near-black background.
Large time centered.
Minimal status.

### Clock mode

Timer plus current time/date.

### Ambient mode

Subtle low-motion visuals.

### Nature/space modes

Future-first; must remain calm.

## Motion

Use motion to communicate state changes:
- app tracking started
- tracking paused
- target completed
- screen transition

Avoid:
- constant floating objects
- rapid gradients
- excessive bounce
- decorative animation while reading stats

Respect reduced-motion settings.

## Color system

Define semantic tokens rather than scattering hard-coded colors:

- background
- surface
- elevated surface
- primary text
- secondary text
- border
- success/status
- warning
- error
- focus accent

AMOLED mode:
- black as the dominant background
- minimal accent usage

Light mode:
- soft neutral background
- restrained contrast
- no neon-heavy dashboard

## Typography

Use system-friendly typography.
Prefer large numerals for time.
Use clear hierarchy:
- display timer
- title
- body
- metadata

Avoid tiny text for critical states.

## Accessibility

MUST:
- screen-reader labels
- sufficient contrast
- touch targets around 44dp
- reduced-motion support
- no color-only state communication
- dynamic text scaling
- clear focus order

## Empty states

History empty:
> “No study sessions yet.”

Study Apps empty:
> “Choose which apps should count as study time.”

Statistics empty:
> “Study for a little while to build your first trend.”

## Loading states

Prefer skeleton/progressive loading for non-critical cloud content.

The local timer and status should not wait for cloud data.

## Error states

Every error should explain:
- what happened
- whether tracking is affected
- what the user can do

## Navigation

Recommended top-level:
- Home
- History
- Statistics
- Settings

Study Apps can live inside Home/Settings and may be promoted later if usage justifies it.
