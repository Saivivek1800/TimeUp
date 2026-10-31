package com.vivek.reclaim.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InterestsUiStateTest {
    @Test
    fun cannotContinueBelowThreeInterests() {
        assertFalse(InterestsUiState().canContinue)
        assertFalse(InterestsUiState(setOf("Science", "History")).canContinue)
    }

    @Test
    fun canContinueFromThreeInterests() {
        assertTrue(InterestsUiState(setOf("Science", "History", "Health")).canContinue)
        assertTrue(InterestsUiState(setOf("A", "B", "C", "D")).canContinue)
    }

    @Test
    fun remainingCountsDownAndStopsAtZero() {
        assertEquals(3, InterestsUiState().remaining)
        assertEquals(1, InterestsUiState(setOf("A", "B")).remaining)
        assertEquals(0, InterestsUiState(setOf("A", "B", "C", "D")).remaining)
    }
}
