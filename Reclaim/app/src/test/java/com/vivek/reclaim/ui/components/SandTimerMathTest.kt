package com.vivek.reclaim.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SandTimerMathTest {
    private val share = 0.8f

    @Test
    fun drainProgress_startsEmptyAndEndsFull() {
        assertEquals(0f, drainProgress(0f, share), 0f)
        assertEquals(0.5f, drainProgress(0.4f, share), 0.0001f)
        assertEquals(1f, drainProgress(share, share), 0f)
        assertEquals(1f, drainProgress(0.95f, share), 0f)
    }

    @Test
    fun flipAngle_staysUprightWhileDraining() {
        assertEquals(0f, flipAngle(0f, share), 0f)
        assertEquals(0f, flipAngle(0.79f, share), 0f)
    }

    @Test
    fun flipAngle_turnsHalfwayOverDuringFlip() {
        val mid = flipAngle(0.9f, share)
        assertTrue("mid-flip angle should be between 0 and 180 but was $mid", mid > 0f && mid < 180f)
        assertEquals(180f, flipAngle(1f, share), 0.001f)
    }
}
