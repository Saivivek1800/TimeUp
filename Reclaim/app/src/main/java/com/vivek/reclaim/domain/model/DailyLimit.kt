package com.vivek.reclaim.domain.model

import com.vivek.reclaim.data.settings.UserSettings

/** Rules and formatting for the daily limit: 30 minutes to 4 hours in 15-minute steps (PRD FR-5). */
object DailyLimit {
    const val MIN = UserSettings.MIN_LIMIT_MINUTES
    const val MAX = UserSettings.MAX_LIMIT_MINUTES
    const val STEP = UserSettings.LIMIT_STEP_MINUTES

    /** Slider "steps" value: the number of stops strictly between MIN and MAX. */
    const val SLIDER_STEPS = (MAX - MIN) / STEP - 1

    /** Quick-pick values shown under the slider. */
    val quickPicks = listOf(60, 90, 120, 150)

    /** Clamps into range and rounds to the nearest allowed step. */
    fun snap(minutes: Int): Int {
        val clamped = minutes.coerceIn(MIN, MAX)
        return ((clamped - MIN + STEP / 2) / STEP) * STEP + MIN
    }

    /** "2h 00m", "1h 15m", "45m". */
    fun format(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return if (hours == 0) "${rest}m" else "${hours}h ${rest.toString().padStart(2, '0')}m"
    }

    /** "2h", "2h 20m", "45m". Used for labels and the baseline. */
    fun formatCompact(minutes: Int): String {
        val hours = minutes / 60
        val rest = minutes % 60
        return when {
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }
}
