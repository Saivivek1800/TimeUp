package com.vivek.reclaim.data.usage

import com.vivek.reclaim.data.db.DailyUsageDao
import com.vivek.reclaim.data.db.DailyUsageEntity
import com.vivek.reclaim.domain.HomeLogic
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** Saves one total per finished day, so streaks survive even though Android only keeps a few days of events. */
@Singleton
class DailyUsageRepository @Inject constructor(
    private val dao: DailyUsageDao,
    private val usage: UsageStatsRepository,
) {
    /** Fills in any finished days we have not saved yet (the app may not have been opened for a while). */
    suspend fun rollover(limitMinutes: Int, packages: Set<String>, limitSetAtMillis: Long) {
        if (limitSetAtMillis <= 0L || packages.isEmpty()) return
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val limitSetDate = Instant.ofEpochMilli(limitSetAtMillis).atZone(zone).toLocalDate()
        val lastStored = dao.recent(1).firstOrNull()?.let { LocalDate.parse(it.date) }

        HomeLogic.datesToFill(lastStored, limitSetDate, today).forEach { date ->
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            val minutes = (usage.foregroundMillis(packages, start, end).values.sum() / 60_000L).toInt()
            dao.upsert(DailyUsageEntity(date.toString(), minutes, withinLimit = minutes <= limitMinutes))
        }
    }

    suspend fun currentStreak(): Int {
        val byDate = dao.recent(60).associate { LocalDate.parse(it.date) to it.withinLimit }
        return HomeLogic.currentStreak(byDate, LocalDate.now())
    }
}
