package com.vivek.reclaim.data.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Reads foreground time from the system's usage events (FR-8: events, not the daily aggregates). */
@Singleton
class UsageStatsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** Foreground milliseconds per package between [start] and [end] (epoch millis). */
    suspend fun foregroundMillis(packages: Set<String>, start: Long, end: Long): Map<String, Long> =
        withContext(Dispatchers.IO) {
            if (packages.isEmpty() || end <= start) return@withContext emptyMap()
            val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val events = manager.queryEvents(start, end)
            val lite = ArrayList<UsageEventLite>()
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                val kind = when (event.eventType) {
                    // Same values as ACTIVITY_RESUMED / ACTIVITY_PAUSED on newer Android versions.
                    @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_FOREGROUND -> UsageEventKind.RESUME
                    @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_BACKGROUND -> UsageEventKind.PAUSE
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE,
                    UsageEvents.Event.DEVICE_SHUTDOWN -> UsageEventKind.SCREEN_OFF
                    else -> null
                } ?: continue
                lite += UsageEventLite(event.timeStamp, event.packageName, kind)
            }
            UsageCalculator.foregroundMillis(lite, packages, start, end)
        }

    /**
     * Average minutes per day across [packages] over the [days] complete days before today, or null if
     * there is nothing to measure. Android only keeps a few days of events, so a new phone may have less.
     */
    suspend fun averageDailyMinutes(packages: Set<String>, days: Int = 7): Int? {
        if (packages.isEmpty()) return null
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val end = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val start = today.minusDays(days.toLong()).atStartOfDay(zone).toInstant().toEpochMilli()
        val totalMillis = foregroundMillis(packages, start, end).values.sum()
        return (totalMillis / days / 60_000L).toInt()
    }
}
