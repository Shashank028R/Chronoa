package com.studycompanion.app.poc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageEventsPocTest {

    @Test
    fun testPocStateMappingRules() {
        val approved = setOf("com.android.chrome", "com.adobe.reader")
        val launchers = setOf("com.google.android.apps.nexuslauncher", "com.oneplus.launcher")

        // 1. Approved package -> FOCUSING
        val state1 = mapPackageToState("com.android.chrome", approved, launchers)
        assertEquals(PocFocusState.FOCUSING, state1)

        // 2. Launcher package -> PAUSED_HOME
        val state2 = mapPackageToState("com.oneplus.launcher", approved, launchers)
        assertEquals(PocFocusState.PAUSED_HOME, state2)

        // 3. Unapproved third-party app -> PAUSED_UNAPPROVED
        val state3 = mapPackageToState("com.instagram.android", approved, launchers)
        assertEquals(PocFocusState.PAUSED_UNAPPROVED, state3)

        // 4. Null package -> UNKNOWN
        val state4 = mapPackageToState(null, approved, launchers)
        assertEquals(PocFocusState.UNKNOWN, state4)
    }

    @Test
    fun testTimestampDelayCalculation() {
        val eventTimestamp = 1700000000000L
        val detectedAt = 1700000000750L
        val delay = detectedAt - eventTimestamp
        assertEquals(750L, delay)
        assertTrue("Delay must be non-negative", delay >= 0)
    }

    @Test
    fun testEventDeduplicationSignature() {
        val ts = 1700000000000L
        val pkg = "com.android.chrome"
        val type = 1 // ACTIVITY_RESUMED

        val key1 = "${ts}_${pkg}_${type}"
        val key2 = "${ts}_${pkg}_${type}"
        val key3 = "${ts + 1000}_${pkg}_${type}"

        val set = mutableSetOf<String>()
        assertTrue(set.add(key1))
        // Second identical event should be recognized as duplicate
        assertTrue(!set.add(key2))
        // Different timestamp event should be added
        assertTrue(set.add(key3))
        assertEquals(2, set.size)
    }

    private fun mapPackageToState(
        pkg: String?,
        approved: Set<String>,
        launchers: Set<String>
    ): PocFocusState {
        if (pkg == null) return PocFocusState.UNKNOWN
        return when {
            approved.contains(pkg) -> PocFocusState.FOCUSING
            launchers.contains(pkg) -> PocFocusState.PAUSED_HOME
            else -> PocFocusState.PAUSED_UNAPPROVED
        }
    }
}
