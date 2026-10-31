package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    private val day = 24L * 60L * 60L * 1000L
    private val now = 1_000_000_000_000L
    private val interests = setOf("Science", "History")

    private fun due(last: Long, cached: Set<String> = interests, selected: Set<String> = interests, at: Long = now) =
        RefreshPolicy.isDue(at, last, selected, cached)

    @Test
    fun theIntervalIsTwoDays() {
        assertEquals(2, RefreshPolicy.INTERVAL_DAYS)
        assertEquals(2 * day, RefreshPolicy.INTERVAL_MILLIS)
    }

    @Test
    fun notDueRightAfterARefresh() {
        assertFalse(due(last = now - 1_000L))
        assertFalse(due(last = now - day))
        assertFalse(due(last = now - 2 * day + 1))
    }

    @Test
    fun dueOnceTwoDaysHavePassed() {
        assertTrue(due(last = now - 2 * day))
        assertTrue(due(last = now - 5 * day))
    }

    @Test
    fun dueWhenNothingWasEverFetched() {
        assertTrue(due(last = 0L))
    }

    @Test
    fun dueWhenANewInterestHasNoSavedArticles() {
        assertTrue(due(last = now - 1_000L, cached = setOf("Science")))
    }

    @Test
    fun notDueWithNoInterestsAtAll() {
        assertFalse(due(last = 0L, selected = emptySet(), cached = emptySet()))
    }

    @Test
    fun dueWhenTheClockWasMovedBackBeforeTheLastRefresh() {
        assertTrue(due(last = now + day))
    }
}
