# 24 — Backend Architecture

## Recommended V1 approach

Use a managed PostgreSQL backend behind a narrow API/repository boundary.

Recommended fast-build option:
- Supabase Auth
- Supabase/PostgreSQL
- Row Level Security
- HTTPS API via client SDK/API layer
- Edge/server functions only where business rules should not run on the client

Alternative:
- Node.js + Express + PostgreSQL
- JWT access/refresh tokens
- REST API
- hosted PostgreSQL

The Android app must not depend on provider-specific behavior outside the repository boundary.

## Why relational data

The domain has strong relationships:

```text
User
 └── Profile
      ├── Targets
      ├── Study Apps
      ├── Sessions
      └── Events

User
 └── Devices
```

Transactions are valuable for:
- target updates
- session edits
- sync acknowledgements
- profile deletion

## Server modules

```text
auth
profiles
devices
study-apps
targets
sessions
events
stats
sync
admin/support
```

## Authorization

Every data request must be scoped to the authenticated user.

For managed Postgres:
- enforce ownership with RLS.

For custom API:
- enforce ownership in service/domain layer.
- never trust client-supplied userId.

## Session ingestion

The server should accept automatic sessions as facts from a registered device.

It should not attempt to re-decide Android foreground-app truth.

Example:

```text
Android Focus Engine
      ↓
Automatic session
      ↓
Cloud
```

Server validates:
- schema
- authenticated ownership
- device registration
- timestamp sanity
- idempotency

## Device registration

On first authenticated device use:
- create logical device ID
- store platform
- model label
- OS version
- app version

Do not use raw hardware serials.

## Sync cursor

Use a server-side monotonically increasing change sequence.

Example:

```text
cursor 105
   ↓
client pulls changes > 105
   ↓
server returns up to 142
   ↓
client stores cursor 142
```

## Soft deletion

Use `deleted_at` or tombstones.

This is required for cross-device deletion propagation.

## Rate limits

Protect:
- auth
- sync push
- history fetch
- future AI queries

## Backend observability

Track:
- API latency
- sync failures
- auth failures
- error rate
- per-endpoint counts

Never log:
- passwords
- access/refresh tokens
- notification content

## Server validation

Reject:
- negative durations
- end before start
- unknown profile ownership
- malformed timestamps
- oversized metadata
- invalid package names where validation is appropriate

## AI boundary

Future AI service receives only the minimum user data required to answer the query.

Never provide database admin credentials to an LLM.
