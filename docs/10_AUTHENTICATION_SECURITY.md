# 10 — Authentication & Security

## Threat model

Protect:
- account credentials
- profile data
- study history
- behavioral usage data
- cloud tokens
- profile PIN/password
- sync records

Potential threats:
- stolen device
- compromised app binary
- network interception
- malicious API client
- leaked logs
- unauthorized profile switching
- broken server authorization
- token theft

## Account authentication

V1:
- email/password through a reputable auth backend

Passwords must be:
- never stored locally in plaintext
- never logged
- never sent over HTTP

Prefer a managed identity system with secure password hashing rather than implementing password hashing from scratch.

Future:
- Credential Manager
- passkeys

Android supports Credential Manager for passwords, passkeys and Google sign-in integrations. [Credential Manager](https://developer.android.com/identity/sign-in/credential-manager-siwg)

## Profile PIN/password

The profile PIN/password is a second boundary inside the account.

Store only:
- salt
- KDF parameters
- verifier
- version

Use:
- Android Keystore for wrapping/protecting local secrets
- a modern password KDF supported by the selected security library

Do not invent cryptography.

For higher assurance, use a vetted library implementation of Argon2id or PBKDF2 depending on the platform/security-library choice.

## Tokens

Access token:
- short-lived

Refresh token:
- encrypted/Keystore-protected where practical
- revocable
- never logged

Use TLS.

## Local database

Room database should not contain raw account passwords.

If the threat model requires encrypted local database content, add SQLCipher or another maintained encryption solution after benchmarking. Do not add heavy encryption without a clear key-management design.

## API authorization

Server MUST validate ownership.

Example:
Client sends:
`profileId = profile_of_someone_else`

Server:
- rejects request
- never returns that profile

## Profile-switch protection

Before showing private profile data:
- require PIN/password
- clear previous profile-specific ViewModels
- refresh repository context

## Privacy by default

Avoid collecting:
- contacts
- exact location
- message contents
- notification contents
- microphone audio
- screen recordings

unless a future feature explicitly requires them.

## Usage data

The app needs app package information to perform its core function.

Document:
- what is observed
- why
- where it is stored
- what is synchronized
- how deletion works

## Notification listener

If implemented:
- disclose notification access
- do not upload notification content
- do not retain notification text
- use the access only to fulfill the user-enabled notification-blocking feature

## Accessibility

Do not use AccessibilityService merely to circumvent platform limitations.

If a later prototype proposes it:
- perform a Play policy review
- document the exact data/actions
- provide prominent in-app disclosure
- verify whether a narrower public API can achieve the same result

## Logs

Production logs must exclude:
- tokens
- passwords
- profile PINs
- notification text
- sensitive backend responses

A developer diagnostic mode may log event IDs/package names only when needed.

## Data deletion

User should be able to:
- delete a session
- delete a profile
- request account deletion
- export study records

Cloud deletion must have a defined lifecycle.

## Account recovery

Password reset belongs to the auth provider/backend.

Profile PIN reset should be treated separately:
- if the profile PIN is unrecoverable, require a secure account-level recovery flow
- never bypass profile protection through an unverified local flag

## Security testing

Include:
- auth bypass tests
- IDOR tests
- token expiration tests
- profile isolation tests
- sync authorization tests
- local secret storage review
- debug-log review
