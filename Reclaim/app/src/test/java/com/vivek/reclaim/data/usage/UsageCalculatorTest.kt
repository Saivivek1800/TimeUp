package com.vivek.reclaim.data.usage

import com.vivek.reclaim.data.usage.UsageEventKind.PAUSE
import com.vivek.reclaim.data.usage.UsageEventKind.RESUME
import com.vivek.reclaim.data.usage.UsageEventKind.SCREEN_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageCalculatorTest {
    private val insta = "com.instagram.android"
    private val yt = "com.google.android.youtube"
    private val both = setOf(insta, yt)
    private val minute = 60_000L

    private fun ev(minutes: Long, pkg: String?, kind: UsageEventKind) = UsageEventLite(minutes * minute, pkg, kind)

    private fun calc(events: List<UsageEventLite>, packages: Set<String> = both, end: Long = 100) =
        UsageCalculator.foregroundMillis(events, packages, windowStart = 0L, windowEnd = end * minute)

    @Test
    fun singleSession_isPauseMinusResume() {
        val result = calc(listOf(ev(10, insta, RESUME), ev(25, insta, PAUSE)))
        assertEquals(15 * minute, result[insta])
    }

    @Test
    fun sessionsAcrossApps_areAddedPerApp() {
        val result = calc(
            listOf(
                ev(0, insta, RESUME), ev(10, insta, PAUSE),
                ev(10, yt, RESUME), ev(40, yt, PAUSE),
                ev(50, insta, RESUME), ev(55, insta, PAUSE),
            ),
        )
        assertEquals(15 * minute, result[insta])
        assertEquals(30 * minute, result[yt])
    }

    @Test
    fun appsNotSelected_areIgnored() {
        val result = calc(listOf(ev(0, "com.other", RESUME), ev(30, "com.other", PAUSE)))
        assertEquals(emptyMap<String, Long>(), result)
    }

    @Test
    fun twoActivitiesOfOneApp_countOnceUntilTheLastPauses() {
        val result = calc(
            listOf(
                ev(0, insta, RESUME),
                ev(5, insta, RESUME),
                ev(6, insta, PAUSE),
                ev(10, insta, PAUSE),
            ),
        )
        assertEquals(10 * minute, result[insta])
    }

    @Test
    fun screenOff_closesOpenSessions() {
        val result = calc(listOf(ev(10, insta, RESUME), ev(20, null, SCREEN_OFF)))
        assertEquals(10 * minute, result[insta])
    }

    @Test
    fun pauseAfterScreenOff_doesNotCountAgain() {
        val result = calc(listOf(ev(10, insta, RESUME), ev(20, null, SCREEN_OFF), ev(20, insta, PAUSE)))
        assertEquals(10 * minute, result[insta])
    }

    @Test
    fun firstEventBeingAPause_meansForegroundSinceWindowStart() {
        val result = calc(listOf(ev(7, insta, PAUSE)))
        assertEquals(7 * minute, result[insta])
    }

    @Test
    fun sessionStillOpenAtTheEnd_runsToWindowEnd() {
        val result = calc(listOf(ev(90, insta, RESUME)), end = 100)
        assertEquals(10 * minute, result[insta])
    }

    @Test
    fun noEvents_meansNoUsage() {
        assertNull(calc(emptyList())[insta])
    }
}
