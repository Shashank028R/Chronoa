# 11 — Offline & Cloud Synchronization

## Objective

Core tracking must continue without internet.

## Source-of-truth model

During normal operation:

**Local database = operational source of truth**

Cloud = synchronized copy + cross-device source for future devices.

This keeps the timer functional offline.

## Offline flow

```text
Local event
  ↓
Room transaction
  ↓
Session updated
  ↓
SyncRecord = PENDING
  ↓
Cloud unavailable
  ↓
Continue normally
```

When network returns:

```text
Connectivity restored
  ↓
Sync scheduler
  ↓
Push local mutations
  ↓
Pull remote changes
  ↓
Resolve conflicts
  ↓
Update sync metadata
```

## Sync order

Recommended:
1. Authenticate if needed.
2. Push local changes.
3. Receive server acknowledgements.
4. Pull remote changes since cursor.
5. Apply remote changes in transaction.
6. Recompute affected daily statistics.
7. Persist new cursor.

## Idempotency

Every mutation has:
- mutationId
- deviceId
- recordId
- entityVersion
- operation

Server must ignore duplicate mutation IDs safely.

## Conflicts

Conflicts can happen if:
- same session edited on two devices
- profile settings changed on two devices
- target edited offline on multiple devices

Recommended policy:

### Study sessions
Prefer immutable event/session creation.

Edits become explicit modifications rather than silent overwrites.

### Daily target
Use last-write-wins on adjusted target metadata, while preserving original target and an audit trail.

### Settings
Last-write-wins is acceptable for simple settings.

### Study app selection
Use set semantics:
- add/remove as explicit operations
- conflict resolution by latest operation timestamp/version

## Clock drift

Do not assume client clocks are correct.

Server records:
- serverReceivedAt
- clientOccurredAt

For study sessions, clientOccurredAt remains the historical event time, but sync ordering uses server version metadata.

## Tombstones

Deletes should create tombstones so they are propagated to other devices.

Do not permanently delete a record from the server before all supported sync windows can receive the deletion.

## Retry

Retry:
- transient network failure
- 5xx
- connectivity restored

Do not retry indefinitely on:
- validation error
- authorization error
- malformed request

Backoff:
- exponential with jitter
- capped
- reset after success

## WorkManager

Use WorkManager for deferred synchronization and maintenance where appropriate.

Do not use WorkManager as the real-time timer engine.

## Sync status UI

States:
- Synced just now
- Syncing…
- Offline — changes saved locally
- Sync error — Tap to retry

## Data consistency

A study session should appear immediately in local history.

Cloud sync can lag without affecting the user's dashboard.

## Future PC

The PC app should reuse:
- entity IDs
- session semantics
- event provenance
- sync protocol

It must never create Android-specific fake sessions.

## Disaster recovery

The cloud service should support:
- backups
- point-in-time recovery where available
- audit logs for destructive admin operations

The mobile app should survive cloud outage without losing locally stored sessions.
