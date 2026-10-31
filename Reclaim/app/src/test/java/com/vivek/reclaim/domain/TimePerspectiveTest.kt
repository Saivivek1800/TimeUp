package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class TimePerspectiveTest {
    @Test
    fun twoHoursADay_isFourteenHoursAWeek() {
        val p = TimePerspective.of(120)
        assertEquals(840, p.weeklyMinutes)
        assertEquals("14 hrs", p.weeklyHoursLabel)
    }

    @Test
    fun twoHoursADay_isNearlyOneFullWakingDay() {
        assertEquals("nearly 1 full waking day", TimePerspective.of(120).dayPhrase)
    }

    @Test
    fun booksPerYear_usesEightHoursABookAndRoundsToFive() {
        // 120 min * 365 / 60 = 730 hours; / 8 = 91.25 books; to the nearest 5 is 90.
        assertEquals(90, TimePerspective.of(120).booksPerYear)
    }

    @Test
    fun smallLimits_getHumbleFigures() {
        val p = TimePerspective.of(30)
        assertEquals("3.5 hrs", p.weeklyHoursLabel)
        assertEquals("about a quarter of a waking day", p.dayPhrase)
        // 30 min * 365 / 60 = 182.5 hours; / 8 = 22.8 books; to the nearest 5 is 25.
        assertEquals(25, p.booksPerYear)
    }

    @Test
    fun largeLimits_countWakingDaysInHalves() {
        val p = TimePerspective.of(240)
        assertEquals("28 hrs", p.weeklyHoursLabel)
        assertEquals("about 2 waking days", p.dayPhrase)
    }

    @Test
    fun threeHours_isAboutOneAndAHalfWakingDays() {
        assertEquals("about 1.5 waking days", TimePerspective.of(180).dayPhrase)
    }

    @Test
    fun limitLabel_isShort() {
        assertEquals("2h", TimePerspective.of(120).limitLabel)
        assertEquals("1h 30m", TimePerspective.of(90).limitLabel)
    }
}
