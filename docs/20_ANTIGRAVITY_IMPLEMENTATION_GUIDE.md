# 20 — Antigravity Implementation Guide

## Operating rule

Antigravity is an implementation agent, not the product decision maker.

Read the relevant docs before changing architecture.

## Required reading order

1. `REQUIREMENTS_REVIEW.md`
2. `00_PROJECT_OVERVIEW.md`
3. `01_PRODUCT_REQUIREMENTS.md`
4. `02_FEATURE_SPECIFICATION.md`
5. `03_USER_FLOWS.md`
6. `04_UX_UI_SPECIFICATION.md`
7. `05_FOCUS_ENGINE.md`
8. `06_ANDROID_ARCHITECTURE.md`
9. `07_DATA_MODEL.md`
10. `08_DATABASE_SCHEMA.md`
11. `09_API_SPECIFICATION.md`
12. `10_AUTHENTICATION_SECURITY.md`
13. `11_OFFLINE_SYNC.md`
14. `12_NOTIFICATIONS_AND_INTERRUPTION.md`
15. `13_STATISTICS_ANALYTICS.md`
16. `14_TESTING_STRATEGY.md`
17. `15_ERROR_EDGE_CASES.md`
18. `16_DEVELOPMENT_ROADMAP.md`
19. Future docs only when the current phase explicitly references them.
20. `21_CURRENT_STATUS.md` every time.

## Prompt style for Antigravity

Each implementation request should contain:

1. phase
2. objective
3. allowed scope
4. source documents
5. constraints
6. acceptance criteria
7. tests required
8. stop condition

### Example

```text
Read:
05_FOCUS_ENGINE.md
06_ANDROID_ARCHITECTURE.md
14_TESTING_STRATEGY.md
21_CURRENT_STATUS.md

Implement only Phase 0 UsageEvents proof of concept.

Do not implement:
auth
cloud sync
AI
visual themes
PC integration

Acceptance:
- approved app -> FOCUSING
- unapproved -> pause
- return -> FOCUSING
- process restart -> recovery
- event timeline is inspectable

Before changing code:
1. inspect current repository
2. identify existing architecture
3. state any contradiction
4. implement
5. run tests
6. update CURRENT_STATUS.md
```

## Change discipline

Before editing:
- inspect current code
- inspect tests
- inspect related docs
- identify dependencies

After editing:
- run formatter
- run static analysis
- run unit tests
- run relevant instrumented tests
- document known failures

Do not:
- rewrite unrelated files
- upgrade every dependency automatically
- change package IDs casually
- add unsupported Android APIs
- use reflection/hidden APIs
- add AccessibilityService as a shortcut
- silently expand V1

## UI implementation

The AI should implement:
- accessible Compose UI
- state-driven screens
- no business logic in Composables
- previews where practical

## Tracking implementation

The AI must treat the Focus Engine as authoritative.

UI timer:
- observes engine state
- does not mutate engine state except through commands

## Database implementation

Use:
- Room transactions
- migrations
- indexed queries
- flow-based observation

Never:
- perform long DB work on main thread
- recalculate all historical stats after every tick
- write one database row every second

## Synchronization implementation

Use:
- idempotency
- offline queue
- explicit sync state
- transactional application of changes

## Security implementation

Never log:
- passwords
- tokens
- notification contents

Use:
- Keystore
- secure transport
- server-side authorization

## Technical uncertainty rule

If the docs say a capability is uncertain:
- do a minimal proof of concept
- do not substitute a risky API without review
- update `REQUIREMENTS_REVIEW.md`
- record the finding in `21_CURRENT_STATUS.md`

## Testing requirement

No phase is complete without acceptance tests.

## Documentation maintenance

Every architectural change must update:
- affected feature docs
- current status
- roadmap if scope changes
