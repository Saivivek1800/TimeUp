package com.vivek.reclaim.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitLockTest {
    private val hour = 60L * 60L * 1000L
    private val day = 24L * hour
    private val start = 1_000_000_000_000L // an arbitrary wall-clock moment
    private val elapsed0 = 5 * hour // phone has been up for 5 hours when the limit is set

    private fun fresh() = LimitLock.startLock(start, elapsed0)

    @Test
    fun startLock_locksForThirtyDays() {
        val lock = fresh()
        assertTrue(lock.isLocked)
        assertEquals(start + 30 * day, lock.lockEndsAt)
        assertEquals(30 * day, LimitLock.LOCK_MILLIS)
    }

    @Test
    fun normalTime_unlocksAfterThirtyDays() {
        val almost = TrustedClock.advance(fresh(), wallNowMillis = start + 29 * day, elapsedNowMillis = elapsed0 + 29 * day)
        assertTrue(almost.isLocked)
        val done = TrustedClock.advance(almost, wallNowMillis = start + 30 * day, elapsedNowMillis = elapsed0 + 30 * day)
        assertFalse(done.isLocked)
    }

    @Test
    fun movingTheClockForward_doesNotUnlock() {
        // Same boot: the user sets the wall clock 40 days ahead, but only an hour really passed.
        val tampered = TrustedClock.advance(fresh(), wallNowMillis = start + 40 * day, elapsedNowMillis = elapsed0 + hour)
        assertTrue(tampered.isLocked)
        assertEquals(start + hour, tampered.trustedNow)
    }

    @Test
    fun movingTheClockBack_doesNotExtendOrBreakAnything() {
        val back = TrustedClock.advance(fresh(), wallNowMillis = start - 10 * day, elapsedNowMillis = elapsed0 + hour)
        assertEquals(start + hour, back.trustedNow)
        // Restoring the clock later still only counts real elapsed time.
        val restored = TrustedClock.advance(back, wallNowMillis = start + 2 * hour, elapsedNowMillis = elapsed0 + 2 * hour)
        assertEquals(start + 2 * hour, restored.trustedNow)
    }

    @Test
    fun afterReboot_aSmallForwardWallMoveIsAccepted() {
        // Elapsed-realtime went backwards, so the phone rebooted. 3 hours of wall time passed.
        val afterBoot = TrustedClock.advance(fresh(), wallNowMillis = start + 3 * hour, elapsedNowMillis = 60_000L)
        assertEquals(start + 3 * hour, afterBoot.trustedNow)
    }

    @Test
    fun afterReboot_aHugeClockJumpIsCappedToOneDay() {
        val afterBoot = TrustedClock.advance(fresh(), wallNowMillis = start + 40 * day, elapsedNowMillis = 60_000L)
        assertTrue(afterBoot.isLocked)
        assertEquals(start + TrustedClock.MAX_REBOOT_ADVANCE_MILLIS, afterBoot.trustedNow)
    }

    @Test
    fun afterReboot_aBackwardsWallClockAddsNothing() {
        val afterBoot = TrustedClock.advance(fresh(), wallNowMillis = start - day, elapsedNowMillis = 60_000L)
        assertEquals(start, afterBoot.trustedNow)
    }

    @Test
    fun nextChangeDate_isWallNowPlusWhatIsLeft() {
        val later = TrustedClock.advance(fresh(), wallNowMillis = start + 10 * day, elapsedNowMillis = elapsed0 + 10 * day)
        assertEquals(start + 10 * day + 20 * day, later.nextChangeAtWallMillis(start + 10 * day))
    }

    @Test
    fun nextChangeDate_isNowOnceUnlocked() {
        val done = TrustedClock.advance(fresh(), wallNowMillis = start + 31 * day, elapsedNowMillis = elapsed0 + 31 * day)
        assertEquals(start + 31 * day, done.nextChangeAtWallMillis(start + 31 * day))
    }
}
