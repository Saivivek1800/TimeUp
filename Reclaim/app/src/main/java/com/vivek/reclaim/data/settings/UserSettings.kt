package com.vivek.reclaim.data.settings

import com.vivek.reclaim.domain.LockSnapshot
import com.vivek.reclaim.domain.model.ThemeMode

/** Settings persisted in DataStore (PRD section 9). Emergency unlock is v1.1 and intentionally absent. */
data class UserSettings(
    val dailyLimitMinutes: Int = DEFAULT_LIMIT_MINUTES,
    val blockedPackages: Set<String> = emptySet(),
    val interests: Set<String> = emptySet(),
    /** Epoch millis of the last limit change; 0 means the limit was never set. */
    val limitLastChangedAt: Long = 0L,
    /** SystemClock.elapsedRealtime() at the last change, used for clock-tamper detection (FR-28). */
    val lastElapsedRealtimeAtChange: Long = 0L,
    val onboardingComplete: Boolean = false,
    /** Lock state for the limit and for removing apps; see [com.vivek.reclaim.domain.TrustedClock]. */
    val lock: LockSnapshot? = null,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Epoch millis of the last time the article feeds were fetched successfully; 0 means never. */
    val articlesRefreshedAt: Long = 0L,
    /** The local date (yyyy-MM-dd) on which the "almost out of time" heads-up was last shown, so it appears once a day. */
    val limitWarningDate: String? = null,
) {
    /** True while the 30-day lock is on. Before the limit was ever set there is nothing to lock. */
    val isLimitLocked: Boolean get() = lock?.isLocked == true

    companion object {
        const val DEFAULT_LIMIT_MINUTES = 120
        const val MIN_LIMIT_MINUTES = 30
        const val MAX_LIMIT_MINUTES = 240
        const val LIMIT_STEP_MINUTES = 15
        const val MIN_INTERESTS = 3
    }
}
