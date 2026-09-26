# 07 — Data Model

## Design principles

- Every user-visible record belongs to a profile.
- Device/platform provenance is first-class.
- Automatic and manual tracking are distinct.
- Sync metadata is separate from product semantics.
- Historical values are immutable where necessary.
- IDs are globally unique.

## Entity map

```text
User
 └── Profile
      ├── ProfileSettings
      ├── StudyAppSelection
      ├── DailyTarget
      ├── StudySession
      │     └── SessionEvent
      └── DailyStats

User
 └── Device

User/Profile/Device
       ↓
   SyncRecord
```

## User

Fields:
- id
- email
- authProvider
- createdAt
- updatedAt
- deletedAt nullable

## Profile

Fields:
- id
- userId
- name
- avatarRef nullable
- pinVerifier
- pinVersion
- createdAt
- updatedAt
- deletedAt

Do not store the raw profile password/PIN.

## Device

Fields:
- id
- userId
- platform
- modelLabel
- osVersion
- appVersion
- createdAt
- lastSeenAt
- isActive

Do not use hardware identifiers unless genuinely required.

## StudyAppSelection

Fields:
- id
- profileId
- packageName
- appLabel
- iconReference/local cache reference
- isEnabled
- addedAt
- updatedAt

The package name is the technical identity; display label can change.

## DailyTarget

Fields:
- id
- profileId
- date
- originalTargetSeconds
- adjustedTargetSeconds nullable
- carryInSeconds nullable
- carryOutSeconds nullable
- createdAt
- updatedAt

Target semantics:
Effective target =
originalTarget + approved carry-in + approved adjustments.

Do not erase originalTarget.

## StudySession

Fields:
- id
- profileId
- deviceId
- platform
- packageName nullable for manual sessions
- startAt
- endAt
- durationSeconds
- trackingType
- verificationStatus
- subjectId nullable
- createdAt
- updatedAt
- deletedAt nullable
- syncVersion
- syncState

Enums:

trackingType:
- AUTOMATIC
- MANUAL
- AUTOMATIC_MODIFIED

verificationStatus:
- VERIFIED_BY_RULES
- MANUAL_UNVERIFIED
- NEEDS_REVIEW

## SessionEvent

Fields:
- id
- sessionId nullable
- profileId
- deviceId
- eventType
- timestamp
- packageName nullable
- engineState
- reasonCode
- source
- metadataJson
- createdAt

Examples:
- APP_RESUMED
- APP_PAUSED
- KEYGUARD_SHOWN
- CALL_STARTED
- CALL_ENDED
- USER_START
- USER_STOP
- PERMISSION_LOST
- PERMISSION_RESTORED
- RECOVERY

## DailyStats

Derived and cacheable.

Fields:
- id
- profileId
- date
- automaticSeconds
- manualSeconds
- totalSeconds
- effectiveTargetSeconds
- sessionCount
- longestSessionSeconds
- streakContribution
- updatedAt

DailyStats must be reproducible from sessions.

## ProfileSettings

Fields:
- countWhileLocked
- pauseDuringCalls
- blockNotifications
- pauseInMultiWindow
- pauseInFloatingWindow
- defaultDailyTarget
- focusDisplayMode
- animationLevel
- themeMode

## SyncRecord

Fields:
- localRecordId
- entityType
- operation
- localUpdatedAt
- serverUpdatedAt nullable
- syncVersion
- state
- lastAttemptAt
- attemptCount
- errorCode nullable

## Future extension fields

Keep room for:
- external device/session source
- subject
- goals
- AI insights
- AI conversation references
- Windows application identity
- Live Alert integration identifier
