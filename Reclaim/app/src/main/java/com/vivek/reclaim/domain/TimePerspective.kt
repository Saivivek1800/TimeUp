package com.vivek.reclaim.domain

import com.vivek.reclaim.domain.model.DailyLimit
import kotlin.math.roundToInt

/**
 * The "gentle reality check" on the interception screen: what the daily limit adds up to over a week and a year.
 * Everything is calculated from the limit; the two assumptions are stated openly in [WAKING_HOURS_PER_DAY] and
 * [HOURS_PER_BOOK].
 */
data class TimePerspective(
    val limitMinutes: Int,
    val weeklyMinutes: Int,
    /** The week's time as a number of 16-hour waking days. */
    val wakingDays: Double,
    val booksPerYear: Int,
) {
    /** "14 hrs", "3.5 hrs". */
    val weeklyHoursLabel: String
        get() {
            val hours = weeklyMinutes / 60.0
            val text = if (hours % 1.0 == 0.0) hours.toInt().toString() else "%.1f".format(java.util.Locale.US, hours)
            return "$text hrs"
        }

    val dayPhrase: String
        get() = when {
            wakingDays < 0.25 -> "about a quarter of a waking day"
            wakingDays < 0.6 -> "about half a waking day"
            wakingDays < 0.95 -> "nearly 1 full waking day"
            wakingDays < 1.15 -> "about 1 full waking day"
            else -> {
                val halves = (wakingDays * 2).roundToInt() / 2.0
                val text = if (halves % 1.0 == 0.0) halves.toInt().toString() else "%.1f".format(java.util.Locale.US, halves)
                "about $text waking days"
            }
        }

    /** The limit as a short phrase for "What could your future self build with these 2 hours today?". */
    val limitLabel: String get() = DailyLimit.formatCompact(limitMinutes)

    companion object {
        const val WAKING_HOURS_PER_DAY = 16
        const val HOURS_PER_BOOK = 8

        fun of(limitMinutes: Int): TimePerspective {
            val weekly = limitMinutes * 7
            val booksExact = limitMinutes * 365 / 60.0 / HOURS_PER_BOOK
            // Past 20 books, round to the nearest 5 so the figure reads as the estimate it is.
            val books = if (booksExact >= 20) (booksExact / 5).roundToInt() * 5 else booksExact.roundToInt()
            return TimePerspective(
                limitMinutes = limitMinutes,
                weeklyMinutes = weekly,
                wakingDays = weekly / (WAKING_HOURS_PER_DAY * 60.0),
                booksPerYear = books,
            )
        }
    }
}
