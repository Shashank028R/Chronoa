# 16 — Development Roadmap

## Phase 0 — Architecture proof of concept

Goal:
Prove the hard parts before polishing UI.

Build:
- minimal Android shell
- Usage Access detection
- UsageEvents ingestion
- current-package resolver
- basic Focus Engine
- Room event storage
- test timeline viewer

Acceptance:
- open approved app → focus
- open unapproved → pause
- return → focus
- process restart → recover

Deliverable:
A small, ugly but technically validated tracker.

## Phase 1 — V1 foundation

Build:
- project structure
- auth
- profile
- local DB
- settings
- basic navigation
- permission health

Do not add visual themes yet.

## Phase 2 — Core tracking

Build:
- Study App picker
- Focus Engine
- session creation
- lock behavior
- call behavior
- notification interaction
- window behavior based on validated capabilities
- recovery

This is the critical phase.

## Phase 3 — Cloud synchronization

Build:
- backend/auth integration
- device registration
- session sync
- target sync
- settings sync
- conflict handling
- sync status

## Phase 4 — Core UX polish

Build:
- Home
- Focus View
- History
- Statistics
- animations
- AMOLED/light/dark
- accessibility

## Phase 5 — Hardening

Test:
- reboot
- process death
- battery optimization
- Android 14/15/16
- OEM devices
- network loss
- permission revocation

## Phase 6 — V1 release candidate

Checklist:
- privacy policy
- data deletion
- permission disclosures
- Play policy review
- crash-free core flows
- battery review
- accessibility review

## V2 — Focus experience

Future:
- rich displays
- ambient themes
- sounds
- goals
- subjects
- Pomodoro/custom break
- widgets
- better analytics
- optional notification blocking

## V3 — Device integration

Future:
- OnePlus Live Alert where technically supported
- deeper widgets
- lock-screen experiences
- improved OEM integrations

## V4 — PC companion

Future:
- Windows agent
- desktop Study Apps
- idle detection
- cloud sync
- unified sessions

## V5 — AI

Future:
- AI study chat
- AI planner
- AI insights
- exam plans
- data-backed conversational analytics

## Implementation gating

Antigravity should not start Phase N+1 if Phase N acceptance tests are failing.

## Definition of done per phase

Each phase requires:
- implementation
- tests
- docs updated
- known issues recorded
- current status updated
