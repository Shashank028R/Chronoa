# 26 — Antigravity Prompt Templates

These are templates, not commands to execute all at once.

## Template A — Read and plan

```text
Read REQUIREMENTS_REVIEW.md, 00_PROJECT_OVERVIEW.md, 01_PRODUCT_REQUIREMENTS.md,
05_FOCUS_ENGINE.md, 06_ANDROID_ARCHITECTURE.md and 21_CURRENT_STATUS.md.

Do not change code.

Summarize:
1. current repository architecture
2. relevant existing implementation
3. requirements for this phase
4. technical risks
5. exact files you expect to change

Stop before implementation and surface contradictions.
```

## Template B — Implement one phase

```text
Read:
[relevant docs]

Implement only:
[phase objective]

Do not implement:
[list exclusions]

Requirements:
[list exact behaviors]

Acceptance tests:
[list tests]

After implementation:
1. run formatting
2. run unit tests
3. run relevant integration/instrumented tests
4. report failures
5. update 21_CURRENT_STATUS.md
6. do not modify unrelated architecture
```

## Template C — Debugging

```text
Investigate this bug:
[bug]

Read:
05_FOCUS_ENGINE.md
15_ERROR_EDGE_CASES.md
21_CURRENT_STATUS.md

Reproduce or reason from existing logs/tests first.

Do not apply a speculative workaround.

Find:
- root cause
- smallest safe fix
- regression test

Then implement only the fix and update documentation if behavior changed.
```

## Template D — Android capability proof

```text
This is a platform-risk proof of concept.

Goal:
[capability]

Use only public Android APIs.

Do not:
- use hidden APIs
- use reflection to bypass restrictions
- add AccessibilityService without policy review
- weaken security

Test on:
[device/API versions]

Document:
- what works
- what does not work
- timing/latency
- permission requirements
- OEM differences
- recommended V1 behavior
```

## Template E — Code review

```text
Review the implementation against the specification.

Read:
01_PRODUCT_REQUIREMENTS.md
05_FOCUS_ENGINE.md
06_ANDROID_ARCHITECTURE.md
14_TESTING_STRATEGY.md
15_ERROR_EDGE_CASES.md

Find:
- behavior mismatches
- race conditions
- lifecycle bugs
- duplicate session risk
- offline data loss risk
- security issues
- unnecessary dependencies

Do not rewrite everything.
Recommend the smallest changes first.
```
