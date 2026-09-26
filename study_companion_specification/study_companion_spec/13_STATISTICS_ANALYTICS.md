# 13 — Statistics & Analytics

## Philosophy

Statistics should describe behavior, not judge the user.

## V1 metrics

### Daily
- automatic time
- manual time
- total time
- target
- adjusted target
- remaining
- session count
- longest session

### Weekly
- total
- daily breakdown
- target completion by day
- streak
- most-used Study App

### Monthly
- total focused time
- average/day
- best day
- number of sessions
- target completion trend

## Automatic vs manual

Display separately.

Example:

```text
Today
2h 40m automatically tracked
+30m manually added
-------------------
3h 10m total
```

This maintains honesty.

## Target completion

Suggested:

`completion = eligible automatic time / effective target`

Manual time can be shown in total study but should not silently convert into “verified completion” unless the product explicitly chooses that policy later.

## Longest session

Based on contiguous automatic tracked intervals.

## App breakdown

Aggregate automatic time by packageName.

Do not assume the app label is stable.

## Streak

V1 recommendation:
- achieved when automatic time >= effective target
- optionally configurable later

Rest days and partial-day semantics should be added in V2 after user feedback.

## Derived statistics

DailyStats is cacheable.

When sessions change:
- recompute affected days
- recompute streak boundaries
- update visible statistics

## Data accuracy

Use session boundaries, not repeated timer ticks.

## Future metrics

- best focus hour
- consistency
- subject distribution
- device/platform breakdown
- distraction events
- monthly comparisons
- AI-generated summaries

## AI readiness

Store clean structured facts:
- session
- target
- source app
- subject
- platform
- timestamps
- verification status

Do not store pre-generated AI claims as the source of truth.
