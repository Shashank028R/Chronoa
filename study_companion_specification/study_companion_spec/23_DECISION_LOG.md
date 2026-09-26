# 23 — Decision Log

## D-001 — Product scope

Decision:
Android-first, PC later.

Reason:
The first product value is mobile app-context tracking; laptop accuracy is a later companion capability.

## D-002 — Timer authority

Decision:
Focus Engine is authoritative.

Reason:
Prevents UI components, notifications, and future integrations from drifting apart.

## D-003 — Manual time honesty

Decision:
Manual time is allowed but flagged unverified.

Reason:
The product is designed around automatic tracking; manual data must not be indistinguishable from automatically verified time.

## D-004 — Offline-first

Decision:
Local DB remains operational during network loss.

Reason:
Study tracking is time-sensitive and should not depend on internet.

## D-005 — AI later

Decision:
No AI in V1.

Reason:
Core tracking and data quality come first; AI can be layered over structured historical data later.

## D-006 — Live Alert later

Decision:
Live Alert is a consumer of Focus Engine state.

Reason:
The timer must remain functional if the OEM integration fails.

## D-007 — AccessibilityService

Decision:
Not default V1 strategy.

Reason:
Platform/Play policy risk; prefer supported narrower APIs.

## D-008 — Installed app visibility

Decision:
Use minimum visibility necessary.

Reason:
Privacy and Play policy.

## D-009 — Statistics

Decision:
Derived from sessions/targets.

Reason:
Keeps analytics reproducible and repairable.
