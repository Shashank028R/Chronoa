# 19 — Future OnePlus Live Alert

## Status

FUTURE. Do not implement in V1.

## Desired behavior

The user wants a Live Alert representation of the current Focus Engine state on supported OnePlus devices/software.

When focusing:

```text
● STUDYING
01:24:37
```

When paused:

```text
○ PAUSED
01:24:37
```

When resumed:

```text
● STUDYING
01:24:38
```

## Architecture

```text
Focus Engine
     ↓
Live Alert Adapter
     ↓
OnePlus-supported integration
```

Never:

```text
Live Alert
   ↓
Timer source of truth
```

## Failure isolation

If the Live Alert integration fails:
- Focus Engine continues
- local sessions continue
- Home UI continues
- sync continues

Only the secondary representation is degraded.

## Feasibility requirement

Before implementation:
1. identify the actual public API/OEM integration available on the target OnePlus/OxygenOS release
2. document supported devices
3. document whether an app can update the desired Live Alert while not visible
4. document user permissions
5. test lifecycle/reboot behavior
6. verify store-policy implications

## V1 placeholder

V1 should only expose architecture hooks:
- `LiveStatePublisher` interface
- no OnePlus-specific runtime dependency

## Data exposed

Minimum:
- state
- elapsed focus duration
- display label

Do not expose:
- sensitive notification text
- app contents
- account secrets
