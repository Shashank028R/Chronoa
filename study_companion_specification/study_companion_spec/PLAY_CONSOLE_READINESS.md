# CHRONOA — PLAY CONSOLE READINESS GUIDE

## 1. App Basics
- **App Name**: Chronoa: Focus & Study Timer
- **Short Description**: Intelligent focus timer and study companion that tracks real learning habits.
- **Full Description**: Chronoa helps students and lifelong learners build disciplined study routines. Designed with an AMOLED-black interface, anti-distraction app monitoring, and anti-burn-in clock modes, Chronoa guarantees that only real, uninterrupted study sessions are credited towards your goals.
- **Default Language**: English (United States)
- **App Category**: Education / Productivity

## 2. Target API & Android 15 Compliance
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Minimum SDK**: 29 (Android 10)
- **16 KB Memory Page Compatibility**: Verified compatible with 64-bit architecture.

## 3. Foreground Service Declaration (Play Console)
- **FGS Type**: `specialUse`
- **FGS Permission**: `android.permission.FOREGROUND_SERVICE_SPECIAL_USE`
- **Console Declaration Justification**:
  > Chronoa is an active study tracking application. The foreground service is initiated exclusively when the user taps "Start Study" to monitor eligible study applications on the device. It displays an ongoing user-visible notification showing real-time focus duration and state, and immediately stops when the user finishes or pauses their study session.
- **Video Demonstration URL**: Provide link showing user starting session, notification appearing, switching to approved study app, and stopping session.

## 4. Account Deletion Compliance
- **Requirement**: Apps allowing account creation must provide a mechanism to delete accounts and associated data.
- **In-App Deletion**: Available in Settings -> "Delete Account & Data" (permanently wipes local Room database, user credentials in DataStore, and notifies cloud backend).
- **Web Deletion URL**: Provide link to web deletion request form (e.g., `https://chronoa.app/account-deletion`).

## 5. Data Safety Form Responses
- **Data Collected**: Email (for optional account backup), user study session records, approved app package names.
- **Data Shared with Third Parties**: None.
- **Security Practices**: Data encrypted in transit (TLS 1.3), user can request data deletion.
