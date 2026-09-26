# 21 — Current Status

**Last updated:** 2026-09-22  
**Status:** Planning/specification complete; implementation not started.

## Product decisions complete

- Android-first
- future PC companion
- cloud sync required
- offline-first core
- user-defined Study Apps
- daily target with editable per-day adjustment
- original target preserved
- optional carry-over offer
- signup/login
- multiple protected profiles
- lock-screen tracking configurable; ON by default
- calls configurable; recommended default pause
- notification appearance does not pause
- notification opening follows app-switch rules
- split-screen/floating should pause when reliably detectable
- manual session editing/addition allowed with unverified warning
- minimal Apple-inspired UI
- multiple focus displays planned
- AI future
- OnePlus Live Alert future

## V1 implementation status

- [ ] Project scaffold
- [ ] Auth
- [ ] Profiles
- [ ] Study App manager
- [ ] Usage Access proof of concept
- [ ] Focus Engine
- [ ] Session persistence
- [ ] Lock behavior
- [ ] Call behavior
- [ ] Notification behavior
- [ ] Window-mode behavior
- [ ] Manual sessions
- [ ] Home
- [ ] History
- [ ] Statistics
- [ ] Cloud sync
- [ ] Security hardening
- [ ] Device matrix testing
- [ ] Release candidate

## Known technical risks

1. Cross-app split-screen/floating detection.
2. Proprietary VoIP call observability.
3. OEM battery management.
4. Foreground-service compliance and lifecycle.
5. Usage Access event timing/reconciliation.
6. Play policy around broad installed-app visibility or optional notification blocking.

## Next implementation action

Start with **Phase 0 — Architecture Proof of Concept**.

Do not start with visual polish.

The first goal is proving that Android can reliably produce the required foreground-app state timeline on target devices.

## Changelog

### 2026-09-22
- Initial specification baseline created.
