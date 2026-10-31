package com.vivek.reclaim.domain

import com.vivek.reclaim.data.settings.UserSettings
import com.vivek.reclaim.domain.model.DailyLimit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyLimitTest {
    @Test
    fun snap_clampsToTheAllowedRange() {
        assertEquals(30, DailyLimit.snap(0))
        assertEquals(240, DailyLimit.snap(1000))
    }

    @Test
    fun snap_roundsToTheNearestFifteenMinutes() {
        assertEquals(120, DailyLimit.snap(120))
        assertEquals(120, DailyLimit.snap(125))
        assertEquals(135, DailyLimit.snap(128))
    }

    @Test
    fun sliderSteps_matchTheFifteenMinuteGrid() {
        // 30..240 in 15-minute steps is 15 stops, so 13 between the two ends.
        assertEquals(13, DailyLimit.SLIDER_STEPS)
    }

    @Test
    fun format_usesHoursAndPaddedMinutes() {
        assertEquals("2h 00m", DailyLimit.format(120))
        assertEquals("1h 15m", DailyLimit.format(75))
        assertEquals("45m", DailyLimit.format(45))
    }

    @Test
    fun formatCompact_dropsZeroMinutes() {
        assertEquals("4h", DailyLimit.formatCompact(240))
        assertEquals("2h 20m", DailyLimit.formatCompact(140))
        assertEquals("30m", DailyLimit.formatCompact(30))
    }
}

class OnboardingRequirementsTest {
    private val complete = UserSettings(
        interests = setOf("Science", "History", "Health"),
        blockedPackages = setOf("com.instagram.android"),
    )

    @Test
    fun nothingMissing_whenEverythingIsDone() {
        assertNull(OnboardingRequirements.firstMissing(complete, true, true))
    }

    @Test
    fun permissionsComeFirst() {
        assertEquals(MissingRequirement.PERMISSIONS, OnboardingRequirements.firstMissing(complete, true, false))
        assertEquals(MissingRequirement.PERMISSIONS, OnboardingRequirements.firstMissing(complete, false, true))
    }

    @Test
    fun needsThreeInterests() {
        val few = complete.copy(interests = setOf("Science", "History"))
        assertEquals(MissingRequirement.INTERESTS, OnboardingRequirements.firstMissing(few, true, true))
    }

    @Test
    fun needsAtLeastOneApp() {
        val none = complete.copy(blockedPackages = emptySet())
        assertEquals(MissingRequirement.APPS, OnboardingRequirements.firstMissing(none, true, true))
    }
}
