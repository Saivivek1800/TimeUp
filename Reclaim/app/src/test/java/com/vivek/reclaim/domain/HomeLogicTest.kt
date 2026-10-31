package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HomeLogicTest {
    private val today = LocalDate.of(2026, 10, 30)

    @Test
    fun progress_isUsedOverLimitAndClamped() {
        assertEquals(0f, HomeLogic.progress(0, 120), 0f)
        assertEquals(0.5f, HomeLogic.progress(60, 120), 0.0001f)
        assertEquals(1f, HomeLogic.progress(200, 120), 0f)
        assertEquals(0f, HomeLogic.progress(10, 0), 0f)
    }

    @Test
    fun remaining_neverGoesNegative() {
        assertEquals(40, HomeLogic.remainingMinutes(80, 120))
        assertEquals(0, HomeLogic.remainingMinutes(150, 120))
    }

    @Test
    fun status_switchesAtSeventyFivePercentAndAtTheLimit() {
        assertEquals(LimitStatus.ON_TRACK, HomeLogic.status(89, 120))
        assertEquals(LimitStatus.CLOSE, HomeLogic.status(90, 120))
        assertEquals(LimitStatus.CLOSE, HomeLogic.status(119, 120))
        assertEquals(LimitStatus.REACHED, HomeLogic.status(120, 120))
        assertEquals(LimitStatus.REACHED, HomeLogic.status(300, 120))
    }

    @Test
    fun greeting_followsTheHour() {
        assertEquals("Good morning", HomeLogic.greeting(8))
        assertEquals("Good afternoon", HomeLogic.greeting(13))
        assertEquals("Good evening", HomeLogic.greeting(19))
        assertEquals("Hello", HomeLogic.greeting(2))
    }

    @Test
    fun streak_countsBackFromYesterdayAndStopsAtTheFirstGap() {
        val days = mapOf(
            today.minusDays(1) to true,
            today.minusDays(2) to true,
            today.minusDays(3) to true,
            today.minusDays(4) to false,
            today.minusDays(5) to true,
        )
        assertEquals(3, HomeLogic.currentStreak(days, today))
    }

    @Test
    fun streak_isZeroWhenYesterdayFailedOrIsMissing() {
        assertEquals(0, HomeLogic.currentStreak(mapOf(today.minusDays(1) to false), today))
        assertEquals(0, HomeLogic.currentStreak(emptyMap(), today))
    }

    @Test
    fun streak_ignoresTodayEvenIfItIsWithinLimit() {
        assertEquals(0, HomeLogic.currentStreak(mapOf(today to true), today))
    }

    @Test
    fun datesToFill_startsTheDayAfterTheLimitWasSet() {
        val limitSet = today.minusDays(3)
        assertEquals(
            listOf(today.minusDays(2), today.minusDays(1)),
            HomeLogic.datesToFill(lastStored = null, limitSetDate = limitSet, today = today),
        )
    }

    @Test
    fun datesToFill_skipsDaysAlreadySaved() {
        val result = HomeLogic.datesToFill(
            lastStored = today.minusDays(2),
            limitSetDate = today.minusDays(10),
            today = today,
        )
        assertEquals(listOf(today.minusDays(1)), result)
    }

    @Test
    fun datesToFill_neverGoesBackMoreThanAWeek() {
        val result = HomeLogic.datesToFill(lastStored = null, limitSetDate = today.minusDays(60), today = today)
        assertEquals(7, result.size)
        assertEquals(today.minusDays(7), result.first())
        assertEquals(today.minusDays(1), result.last())
    }

    @Test
    fun datesToFill_isEmptyWhenTheLimitWasSetToday() {
        assertEquals(emptyList<LocalDate>(), HomeLogic.datesToFill(null, today, today))
    }
}
