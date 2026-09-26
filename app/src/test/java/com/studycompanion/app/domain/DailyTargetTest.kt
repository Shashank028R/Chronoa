package com.studycompanion.app.domain

import com.studycompanion.app.domain.model.DailyTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.UUID

class DailyTargetTest {

    @Test
    fun `originalTargetSeconds is preserved when adjustedTargetSeconds is set`() {
        val original = 7200L // 2 hours
        val adjusted = 10800L // 3 hours

        val target = DailyTarget(
            id = UUID.randomUUID().toString(),
            profileId = "profile-1",
            dateKey = "2026-09-22",
            originalTargetSeconds = original,
            adjustedTargetSeconds = adjusted
        )

        assertEquals("Original target must remain 7200", original, target.originalTargetSeconds)
        assertEquals("Adjusted target must be 10800", adjusted, target.adjustedTargetSeconds)
        assertEquals("Effective target should use adjustedTarget", adjusted, target.effectiveTargetSeconds)
    }

    @Test
    fun `effectiveTargetSeconds uses originalTargetSeconds when no adjustment is made`() {
        val original = 7200L

        val target = DailyTarget(
            id = UUID.randomUUID().toString(),
            profileId = "profile-1",
            dateKey = "2026-09-22",
            originalTargetSeconds = original,
            adjustedTargetSeconds = null
        )

        assertNull(target.adjustedTargetSeconds)
        assertEquals("Effective target should equal original target", original, target.effectiveTargetSeconds)
    }

    @Test
    fun `effectiveTargetSeconds correctly incorporates carryInSeconds`() {
        val original = 7200L
        val carryIn = 1800L // 30 mins

        val target = DailyTarget(
            id = UUID.randomUUID().toString(),
            profileId = "profile-1",
            dateKey = "2026-09-22",
            originalTargetSeconds = original,
            adjustedTargetSeconds = null,
            carryInSeconds = carryIn
        )

        assertEquals("Effective target must be original + carryIn", 9000L, target.effectiveTargetSeconds)
    }

    @Test
    fun `calculateRemainingSeconds calculates correct deficit and floors at zero`() {
        val target = DailyTarget(
            id = UUID.randomUUID().toString(),
            profileId = "profile-1",
            dateKey = "2026-09-22",
            originalTargetSeconds = 7200L // 2 hours
        )

        // 30 minutes studied -> 1.5 hours remaining
        assertEquals(5400L, target.calculateRemainingSeconds(1800L))

        // Exactly 2 hours studied -> 0 remaining
        assertEquals(0L, target.calculateRemainingSeconds(7200L))

        // Exceeded target (3 hours studied) -> 0 remaining (never negative)
        assertEquals(0L, target.calculateRemainingSeconds(10800L))
    }

    @Test
    fun `proposeCarryOverSeconds calculates proposed deficit without auto-applying`() {
        val target = DailyTarget(
            id = UUID.randomUUID().toString(),
            profileId = "profile-1",
            dateKey = "2026-09-22",
            originalTargetSeconds = 7200L // 2 hours
        )

        // User studied only 3600s (1h) out of 7200s
        val proposedCarryOver = target.proposeCarryOverSeconds(actualStudiedSeconds = 3600L)
        assertEquals("Proposed carry-over should be uncompleted 3600s", 3600L, proposedCarryOver)

        // The target itself is not modified (carryOutSeconds remains 0 until explicit user decision)
        assertEquals(0L, target.carryOutSeconds)

        // If goal met or exceeded, proposed carry-over is 0
        assertEquals(0L, target.proposeCarryOverSeconds(actualStudiedSeconds = 7200L))
        assertEquals(0L, target.proposeCarryOverSeconds(actualStudiedSeconds = 9000L))
    }
}
