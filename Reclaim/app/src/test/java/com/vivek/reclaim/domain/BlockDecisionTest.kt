package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BlockDecisionTest {
    private val minute = 60_000L

    @Test
    fun limitIsReachedAtOrAboveTheLimit() {
        assertFalse(BlockDecision.isLimitReached(119 * minute, 120))
        assertTrue(BlockDecision.isLimitReached(120 * minute, 120))
        assertTrue(BlockDecision.isLimitReached(200 * minute, 120))
    }

    @Test
    fun recheck_isAtMostEveryFifteenSecondsWhenFarFromTheLimit() {
        assertEquals(15_000L, BlockDecision.nextCheckDelayMillis(10 * minute, 120))
    }

    @Test
    fun recheck_isSoonerWhenTheLimitIsAboutToRunOut() {
        // 5 seconds left: check just after it runs out.
        assertEquals(5_500L, BlockDecision.nextCheckDelayMillis(120 * minute - 5_000L, 120))
    }

    @Test
    fun recheck_neverBusyLoops() {
        assertEquals(1_000L, BlockDecision.nextCheckDelayMillis(120 * minute, 120))
        assertEquals(1_000L, BlockDecision.nextCheckDelayMillis(500 * minute, 120))
    }

    @Test
    fun warning_isDueOnlyInTheLastFiveMinutes() {
        assertFalse(BlockDecision.isWarningDue(114 * minute, 120, alreadyWarnedToday = false)) // 6 min left
        assertTrue(BlockDecision.isWarningDue(115 * minute, 120, alreadyWarnedToday = false)) // exactly 5 left
        assertTrue(BlockDecision.isWarningDue(119 * minute, 120, alreadyWarnedToday = false))
    }

    @Test
    fun warning_isNotDueOnceTheLimitIsUsedUp() {
        assertFalse(BlockDecision.isWarningDue(120 * minute, 120, alreadyWarnedToday = false))
        assertFalse(BlockDecision.isWarningDue(130 * minute, 120, alreadyWarnedToday = false))
    }

    @Test
    fun warning_isShownOnlyOncePerDay() {
        assertFalse(BlockDecision.isWarningDue(118 * minute, 120, alreadyWarnedToday = true))
    }

    @Test
    fun recheck_wakesUpAtTheWarningPointWhenNoWarningWasShownYet() {
        // 20 minutes left: the warning is due in 15 minutes, so check at most every 15 s; 6 minutes left: in 1 minute.
        assertEquals(15_000L, BlockDecision.nextCheckDelayMillis(100 * minute, 120, warningAlreadyShown = false))
        assertEquals(15_000L, BlockDecision.nextCheckDelayMillis(114 * minute, 120, warningAlreadyShown = false))
        // 5m10s left: the warning point is 10 s away.
        assertEquals(10_500L, BlockDecision.nextCheckDelayMillis(120 * minute - (5 * minute + 10_000L), 120, warningAlreadyShown = false))
    }

    @Test
    fun serviceIsOnlyActiveAfterSetupWithSomethingToBlock() {
        assertTrue(BlockDecision.isActive(true, setOf("a"), 60))
        assertFalse(BlockDecision.isActive(false, setOf("a"), 60))
        assertFalse(BlockDecision.isActive(true, emptySet(), 60))
        assertFalse(BlockDecision.isActive(true, setOf("a"), 0))
    }
}
