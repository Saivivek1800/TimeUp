package com.vivek.reclaim.domain

import java.time.LocalDate

enum class LimitStatus { ON_TRACK, CLOSE, REACHED }

/** Pure rules behind the Home screen, kept free of Android so they can be unit-tested. */
object HomeLogic {

    /** 0..1 share of the daily limit already used. */
    fun progress(usedMinutes: Int, limitMinutes: Int): Float =
        if (limitMinutes <= 0) 0f else (usedMinutes.toFloat() / limitMinutes).coerceIn(0f, 1f)

    fun remainingMinutes(usedMinutes: Int, limitMinutes: Int): Int = (limitMinutes - usedMinutes).coerceAtLeast(0)

    fun status(usedMinutes: Int, limitMinutes: Int): LimitStatus = when {
        usedMinutes >= limitMinutes -> LimitStatus.REACHED
        usedMinutes * 100 >= limitMinutes * 75 -> LimitStatus.CLOSE
        else -> LimitStatus.ON_TRACK
    }

    fun greeting(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..21 -> "Good evening"
        else -> "Hello"
    }

    /** The small label above the greeting: "MORNING BALANCE", "EVENING BALANCE", ... */
    fun dayPartLabel(hourOfDay: Int): String = when (hourOfDay) {
        in 5..11 -> "MORNING BALANCE"
        in 12..16 -> "AFTERNOON BALANCE"
        in 17..21 -> "EVENING BALANCE"
        else -> "NIGHT BALANCE"
    }

    /** True when the label should use a moon instead of a sun. */
    fun isNight(hourOfDay: Int): Boolean = hourOfDay !in 6..17

    /** Friendly and encouraging, never shaming (PRD section 6). */
    fun message(status: LimitStatus): String = when (status) {
        LimitStatus.ON_TRACK -> "You’re doing great."
        LimitStatus.CLOSE -> "Almost there. Take it easy."
        LimitStatus.REACHED -> "You’ve used today’s time. Nice effort."
    }

    /**
     * Consecutive completed days within the limit, counting back from yesterday (FR-31).
     * A missing or failed day ends the streak. Today is not counted until it is over.
     */
    fun currentStreak(withinLimitByDate: Map<LocalDate, Boolean>, today: LocalDate): Int {
        var streak = 0
        var day = today.minusDays(1)
        while (withinLimitByDate[day] == true) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /**
     * Days that still need a usage total saved: after the limit was first set, after the last saved day,
     * no further back than [maxDays], and before today.
     */
    fun datesToFill(lastStored: LocalDate?, limitSetDate: LocalDate, today: LocalDate, maxDays: Int = 7): List<LocalDate> {
        var first = maxOf(limitSetDate.plusDays(1), today.minusDays(maxDays.toLong()))
        if (lastStored != null) first = maxOf(first, lastStored.plusDays(1))
        return generateSequence(first) { it.plusDays(1) }.takeWhile { it.isBefore(today) }.toList()
    }
}
