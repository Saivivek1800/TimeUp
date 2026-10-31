package com.vivek.reclaim.domain

/** The lock rules from PRD 5.5, kept in one place so the length and the tamper protection are easy to find. */
object LimitLock {
    /** How long the daily limit (and removing a blocked app) stays locked after the limit is set or changed. */
    const val LOCK_DAYS = 30

    const val LOCK_MILLIS: Long = LOCK_DAYS * 24L * 60L * 60L * 1000L

    /** A limit change starts a fresh lock: the clock is trusted from this moment on. */
    fun startLock(wallNowMillis: Long, elapsedNowMillis: Long): LockSnapshot = LockSnapshot(
        trustedNow = wallNowMillis,
        lockEndsAt = wallNowMillis + LOCK_MILLIS,
        lastWall = wallNowMillis,
        lastElapsed = elapsedNowMillis,
    )
}

/** Everything needed to know whether the lock is still on, and to notice a tampered system clock. */
data class LockSnapshot(
    /** Our own clock, advanced only by time we believe really passed. Compared against [lockEndsAt]. */
    val trustedNow: Long,
    val lockEndsAt: Long,
    /** The system wall clock and elapsed-realtime at the last time we looked. */
    val lastWall: Long,
    val lastElapsed: Long,
) {
    val isLocked: Boolean get() = trustedNow < lockEndsAt

    /** The date to show as "next change allowed", using the real wall clock for display. */
    fun nextChangeAtWallMillis(wallNowMillis: Long): Long = wallNowMillis + (lockEndsAt - trustedNow).coerceAtLeast(0L)
}

/**
 * Basic clock-tamper protection (FR-28). Moving the phone's clock forward must not unlock the limit early.
 *
 * - Within one boot, elapsed-realtime cannot be changed by the user, so only it advances our clock.
 *   A wall clock that jumped forward or back is ignored.
 * - After a reboot elapsed-realtime restarts, so it cannot be compared. We accept the wall clock moving
 *   forward, but by at most [MAX_REBOOT_ADVANCE_MILLIS] per reboot, so one clock change plus a reboot cannot
 *   skip the whole lock. A long power-off then simply keeps the lock on a little longer, which is the safe side.
 */
object TrustedClock {
    const val MAX_REBOOT_ADVANCE_MILLIS: Long = 24L * 60L * 60L * 1000L

    fun advance(snapshot: LockSnapshot, wallNowMillis: Long, elapsedNowMillis: Long): LockSnapshot {
        val elapsedDelta = elapsedNowMillis - snapshot.lastElapsed
        val advanceBy = if (elapsedDelta >= 0L) {
            elapsedDelta
        } else {
            (wallNowMillis - snapshot.lastWall).coerceIn(0L, MAX_REBOOT_ADVANCE_MILLIS)
        }
        return snapshot.copy(
            trustedNow = snapshot.trustedNow + advanceBy,
            lastWall = wallNowMillis,
            lastElapsed = elapsedNowMillis,
        )
    }
}
