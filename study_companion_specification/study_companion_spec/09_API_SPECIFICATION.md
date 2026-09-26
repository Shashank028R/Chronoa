# 09 — API Specification

## API style

Recommendation: HTTPS JSON API behind repository interfaces.

The Android app should not scatter direct backend calls across UI.

Recommended layers:

```text
UI
 ↓
Use Case
 ↓
Repository
 ↓
Remote Data Source
 ↓
HTTPS API
```

## Authentication endpoints

### POST /auth/signup

Request:
```json
{
  "email": "user@example.com",
  "password": "..."
}
```

Response:
```json
{
  "userId": "uuid",
  "accessToken": "...",
  "refreshToken": "..."
}
```

### POST /auth/login

### POST /auth/refresh

### POST /auth/logout

### POST /auth/password-reset/request

### POST /auth/password-reset/confirm

## Profile endpoints

### GET /profiles

### POST /profiles

Request:
```json
{
  "name": "Shashank"
}
```

### PATCH /profiles/{profileId}

### DELETE /profiles/{profileId}

Profile PIN/password verification should be a secure local capability; cloud account authorization still controls the account.

## Device endpoints

### POST /devices/register

### PATCH /devices/{deviceId}/heartbeat

### DELETE /devices/{deviceId}

## Study App endpoints

### GET /profiles/{profileId}/study-apps

### PUT /profiles/{profileId}/study-apps/{packageName}

### DELETE /profiles/{profileId}/study-apps/{packageName}

## Daily target endpoints

### GET /profiles/{profileId}/targets/{date}

### PUT /profiles/{profileId}/targets/{date}

Example:
```json
{
  "originalTargetSeconds": 10800,
  "adjustedTargetSeconds": 14400,
  "carryInSeconds": 0
}
```

The server must preserve originalTargetSeconds.

## Session endpoints

### GET /profiles/{profileId}/sessions?from=&to=

### POST /profiles/{profileId}/sessions

### PATCH /profiles/{profileId}/sessions/{sessionId}

### DELETE /profiles/{profileId}/sessions/{sessionId}

## Event sync

### POST /sync/push

Request:
- clientId
- deviceId
- mutations
- idempotency keys

### GET /sync/pull?cursor=

Response:
- changes
- next cursor
- server timestamp

## Sync requirements

All mutations must be idempotent.

Recommended:
- client-generated UUIDs
- server-side unique constraints
- updatedAt/version
- tombstones for deletes

## Authorization

Every endpoint:
1. authenticates user
2. resolves user identity from token
3. checks profile ownership
4. validates request
5. applies operation
6. records server metadata

## Error format

```json
{
  "error": {
    "code": "PROFILE_NOT_FOUND",
    "message": "Profile does not exist.",
    "retryable": false
  }
}
```

Codes should be stable and machine-readable.

## Recommended API errors

- AUTH_INVALID
- AUTH_EXPIRED
- PROFILE_FORBIDDEN
- PROFILE_NOT_FOUND
- VALIDATION_ERROR
- CONFLICT
- RATE_LIMITED
- SERVER_ERROR
- SYNC_CURSOR_INVALID

## Pagination

History endpoints must paginate.

Do not return an entire year's event history in one response.

## Versioning

Start with `/v1`.

Breaking changes require a new API version or compatible migration path.

## Future AI boundary

The AI system should have a separate service boundary from the core mobile API.

Example:

```text
Mobile
  ↓
Core API
  ↓
Study Data

AI Service
  ↓
Authorized study-data read layer
```

Do not give the AI service unrestricted database access.
