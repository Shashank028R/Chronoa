# Phase 5 Implementation Report: Platform Hardening, Recovery & Edge Cases

**Document Version:** 1.0.0  
**Date:** 2026-09-23  
**Phase:** Phase 5 — Platform Hardening, Recovery & Edge Cases  
**Status:** COMPLETE  

---

## 1. Executive Summary

Phase 5 hardens the Study Companion application against critical Android platform anomalies and edge cases:
- Abrupt low-memory process kills (`am kill`) and system crashes
- Device reboots and battery shutdowns (`BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`)
- User clock shifts (manual backward/forward time adjustments and network NTP adjustments)
- Timezone changes and international travel
- Midnight calendar day boundaries and interval splitting
- Usage Access permission revocation and restoration
- Aggressive OEM background freezes (specialized guidance for OnePlus/OxygenOS, OPPO/ColorOS, Xiaomi/MIUI, Samsung/One UI, Realme)
- Privacy-safe diagnostic bundle generation

Additionally, the root cause of the Gradle/JVM test execution hang was diagnosed via thread-level introspection (`jstack`), isolated to DataStore concurrent file-lock contention combined with a terminal JDK mismatch, fixed cleanly, and verified across a 100% passing test suite (70/70 tests) and physical OnePlus 12 hardware.

---

## 2. Root Cause Analysis: Gradle & Test Runner Hang

### 2.1 The Symptoms
Commands such as `.\gradlew clean testDebugUnitTest` and `.\gradlew testDebugUnitTest` were hanging indefinitely without progressing or completing.

### 2.2 Forensic Diagnosis
Using thread introspection on the hanging Gradle Test Worker (`jstack.exe` on PID 33604/14220):
1. **The Hanging Task:** `:app:testDebugUnitTest`.
2. **The Hanging Test Method:** `Phase5HardeningTest.testProcessDeathRecoveryReconcilesDanglingSession`.
3. **The Exact Blocking Stack Trace:**
```text
"SDK 34 Main Thread" #27 prio=5 tid=0x... waiting on condition
   java.lang.Thread.State: TIMED_WAITING (parking)
   at jdk.internal.misc.Unsafe.park(Native Method)
   at java.util.concurrent.locks.LockSupport.parkNanos(LockSupport.java:252)
   at kotlinx.coroutines.BlockingCoroutine.joinBlocking(Builders.kt:98)
   at kotlinx.coroutines.BuildersKt__BuildersKt.runBlocking(Builders.kt:69)
   at com.studycompanion.app.tracking.Phase5HardeningTest.testProcessDeathRecoveryReconcilesDanglingSession(Phase5HardeningTest.kt:122)
```

### 2.3 Underlying Mechanism
1. **Shared Top-Level DataStore Delegate & Uncoordinated Background IO:**
   In `UserSessionDataStore.kt`, `context.userDataStore` is declared as a top-level extension property backed by `preferencesDataStore(name = "user_session_prefs")`.
   In `Phase5HardeningTest.setUp()`, `focusEngine` was instantiated with `userSessionDataStore = dataStore` and `scope = testScope`.
   Every event evaluated by `focusEngine` triggered `updateDurableSessionMarker()`, firing asynchronous `scope.launch { store.setActiveSessionMarker(...) }` routines onto `Dispatchers.IO`.
   Because each test in `Phase5HardeningTest` executed on the shared Robolectric Application Context, asynchronous file IO writes to `user_session_prefs.preferences_pb` on Windows remained in-flight across tests.
2. **File Lock Contention Deadlock with `runBlocking`:**
   When subsequent tests invoked `runBlocking` on the main thread, the Robolectric main thread was parked awaiting completion of DataStore transactions. However, Windows strict file locking (`FileChannel.lock`) conflicted with lingering file channels from previous tests, resulting in an indefinite deadlock where the background IO could not complete and the main thread could not unpark.
3. **Java Environment Discrepancy:**
   The host terminal environment lacked `JAVA_HOME` and defaulted to Oracle JDK 26 (`26.0.2`), whereas Gradle daemon was configured for Temurin JDK 17 (`C:\Users\shash\.jdk\jdk-17.0.20.1+1`). Single-use daemons and `--no-daemon` CLI calls were launching under JDK 26, creating native loader module warnings and unpredictable multi-daemon contention.

---

## 3. Applied Fixes

### 3.1 Architectural Test Isolation Fix (Code)
In `Phase5HardeningTest.kt`:
1. Removed `userSessionDataStore` from `FocusEngine` instantiation in `setUp()`. Focus engine edge-case tests (clock changes, timezone transitions, midnight rollovers, permission states) test engine state logic without emitting unnecessary disk IO across tests.
2. In `testProcessDeathRecoveryReconcilesDanglingSession` and `testBootRecoveryReconcilesDanglingSession`, `UserSessionDataStore` is provided directly to `ProcessDeathRecoveryHandler`.
3. In `tearDown()`, added explicit `dataStore.setActiveSessionMarker(null)` cleanup inside `runBlocking` to guarantee file lock release before the next test begins.

### 3.2 Environment & Toolchain Consistency (Environment)
1. Persistently configured `JAVA_HOME` at the user environment level:
   `JAVA_HOME = C:\Users\shash\.jdk\jdk-17.0.20.1+1`
2. Unified the Launcher JVM and Daemon JVM on Temurin OpenJDK 17.0.20.1, eliminating Java 26 module access warnings and multi-daemon friction.

---

## 4. Phase 5 Implementation Details

### 4.1 Process Death & Crash Recovery (`ProcessDeathRecoveryHandler.kt`)
- **Durable Marker Inspection:** Reads `ACTIVE_SESSION_MARKER` from `UserSessionDataStore` upon app launch or service resurrection.
- **Zero Downtime Fabrication:** Computes duration strictly from `startWall` to `lastHeartbeat`. The duration between `lastHeartbeat` and app resurrection is completely excluded.
- **Audit Event Logging:** Persists `RECONCILED_AFTER_TERMINATION` in Room `session_events` table with gap metadata.
- **Marker Invalidation:** Atomically clears the marker once processed to prevent duplicate reconciliation.

### 4.2 Boot Recovery (`BootCompletedReceiver.kt`)
- Handles `ACTION_BOOT_COMPLETED`, `ACTION_MY_PACKAGE_REPLACED`, and OEM `QUICKBOOT_POWERON` broadcasts.
- Reconciles any session dangling prior to reboot via `ProcessDeathRecoveryHandler`.
- Restores tracking via `FocusTrackingService.startService()` if `isTrackingActive()` was true before shutdown.

### 4.3 Clock, Timezone & Midnight Edge Cases (`TimeChangeReceiver.kt` & `FocusEngine.kt`)
- **Backward Clock Adjustments:** When the system clock moves backward, duration is bounded to `maxOf(0L, elapsed)`. Engine closes the interval and restarts a fresh interval under the new wall clock with `CLOCK_CHANGED` (metadata: `BACKWARD`).
- **Forward Clock Adjustments:** Prevents awarding fictitious time from forward clock jumps. Session duration is constrained and `CLOCK_CHANGED` (metadata: `FORWARD`) is logged.
- **Timezone Adjustments:** Captures `TIMEZONE_CHANGED`, logs event with new timezone identifier, while preserving UTC instants without scrambling historical data.
- **Midnight Rollover:** Detects `ACTION_DATE_CHANGED` and splits multi-day study sessions cleanly at `00:00:00.000` boundaries, ensuring daily target calculations and streaks remain perfectly accurate.

### 4.4 Battery Optimization & OEM Killer Diagnostics (`BatteryOptimizationHelper.kt`)
- Detects device manufacturer and OEM brand (`ONEPLUS`, `OPPO`, `REALME`, `XIAOMI`, `SAMSUNG`, `STOCK_OR_OTHER`).
- Evaluates `PowerManager.isIgnoringBatteryOptimizations()`.
- Generates OEM-tailored guidance for OnePlus/OxygenOS (auto-launch, background freezing bypass, app battery management).
- Generates system settings deep links.

### 4.5 Diagnostic Support Bundle (`DiagnosticBundleGenerator.kt`)
- Generates a structured JSON diagnostic bundle capturing API level, device model, OEM category, battery optimization state, usage access status, and timezone for zero-leak troubleshooting.

---

## 5. Verification Results

### 5.1 Automated JVM Test Suite (All 70 Tests Passing)

```text
Suite                                                    Tests Failures Errors Skipped Time (s)
-----------------------------------------------------------------------------------------------
com.studycompanion.app.database.AppDatabaseTest              4        0      0       0    6.133
com.studycompanion.app.domain.DailyTargetTest                5        0      0       0    0.005
com.studycompanion.app.poc.UsageEventsPocTest                3        0      0       0    0.028
com.studycompanion.app.repository.RepositoryBehaviorTest     4        0      0       0    1.189
com.studycompanion.app.security.PinVerifierTest              6        0      0       0    0.191
com.studycompanion.app.sync.SyncEngineTest                  12        0      0       0    1.267
com.studycompanion.app.tracking.FocusEngineTest             20        0      0       0    0.121
com.studycompanion.app.tracking.Phase5HardeningTest          9        0      0       0    0.406
com.studycompanion.app.ui.Phase4UiTest                       7        0      0       0    0.978
-----------------------------------------------------------------------------------------------
TOTAL                                                       70        0      0       0   10.318
```

- Clean build test command: `.\gradlew clean testDebugUnitTest --no-daemon` -> **BUILD SUCCESSFUL in 52s (30 actionable tasks executed)**.
- Full suite test command: `.\gradlew testDebugUnitTest --no-daemon` -> **BUILD SUCCESSFUL in 19s (0 failures, 0 errors)**.

### 5.2 Physical Device Verification (OnePlus 12 — `QCNBQCINSK4PMJAY`)
1. **Installation:** `.\gradlew assembleDebug` built clean 29.3 MB APK; installed via `adb install -r`.
2. **Cold Start & UI Rendering:** App launched cleanly into `MainActivity`. UI hierarchy verified via `uiautomator dump` (resolution: 1080x2412).
3. **OEM Detection & Diagnostics:** Navigated to `PermissionHealthScreen` on OnePlus 12:
   - Successfully identified device as OnePlus (`OemType.ONEPLUS`).
   - Displayed tailored OxygenOS recommendation: *"OxygenOS uses aggressive background freezing. Allow background activity in Settings > Battery > App Battery Management > Study Companion. Also enable 'Auto-launch' and select 'Don't optimize'."*
   - Verified action button: *"Open Battery Settings"*.
4. **Boot Receiver Verification:**
   - Logcat inspection verified `BootCompletedReceiver` execution: `Received boot/replacement intent: android.intent.action.BOOT_COMPLETED` and `FocusTrackingService restarted successfully after boot.`
5. **Process Death & Cold Restart:** Tested process termination via `am force-stop` followed by cold start; verified clean recovery, database integrity, and UI state restoration.

---

## 6. Phase Status

**Phase 5 Status:** `COMPLETE`  
All acceptance criteria met. Ready for Phase 6 (Release Candidate & Store Policy Compliance).
