package com.vivek.reclaim.domain

/** The rules the accessibility service applies while a blocked app is on screen (PRD FR-12, FR-15). */
object BlockDecision {
    const val MIN_RECHECK_MILLIS = 1_000L
    const val MAX_RECHECK_MILLIS = 15_000L

    /** The heads-up appears when this much of the daily limit is left. */
    const val WARNING_LEAD_MILLIS = 5 * 60_000L

    fun isLimitReached(usedMillis: Long, limitMinutes: Int): Boolean = usedMillis >= limitMinutes * 60_000L

    /**
     * True once, near the end of the day's limit: less than [WARNING_LEAD_MILLIS] left but not yet used up,
     * and no warning has been shown today. Opening a blocked app with 3 minutes left still shows it right away.
     */
    fun isWarningDue(usedMillis: Long, limitMinutes: Int, alreadyWarnedToday: Boolean): Boolean {
        if (alreadyWarnedToday) return false
        val remaining = limitMinutes * 60_000L - usedMillis
        return remaining in 1..WARNING_LEAD_MILLIS
    }

    /**
     * How long to wait before measuring again while the user stays in a blocked app: every 15 seconds at most,
     * but sooner when the warning point or the limit itself is about to arrive, so both land on time.
     */
    fun nextCheckDelayMillis(usedMillis: Long, limitMinutes: Int, warningAlreadyShown: Boolean = true): Long {
        val remaining = limitMinutes * 60_000L - usedMillis
        val untilNextEvent = if (!warningAlreadyShown && remaining > WARNING_LEAD_MILLIS) remaining - WARNING_LEAD_MILLIS else remaining
        return (untilNextEvent + 500L).coerceIn(MIN_RECHECK_MILLIS, MAX_RECHECK_MILLIS)
    }

    /** How long to wait for our block screen to appear before covering the blocked app with the backup panel. */
    const val BACKUP_PANEL_DELAY_MILLIS = 700L

    /**
     * True when the block screen was asked for but is not in front, while the blocked app still is. Some phones
     * (vivo, iQOO, Xiaomi, Oppo) silently refuse to open screens from the background, so the app must not be left usable.
     */
    fun needsBackupPanel(blockScreenVisible: Boolean, stillInBlockedApp: Boolean): Boolean =
        !blockScreenVisible && stillInBlockedApp

    /** The service only acts once setup is finished and there is something to block. */
    fun isActive(onboardingComplete: Boolean, blockedPackages: Set<String>, limitMinutes: Int): Boolean =
        onboardingComplete && blockedPackages.isNotEmpty() && limitMinutes > 0
}
