package com.vivek.reclaim.domain

import java.util.concurrent.TimeUnit

/** When the article cache should be topped up from the feeds. */
object RefreshPolicy {
    /** New articles are fetched about once every this many days. */
    const val INTERVAL_DAYS = 2

    val INTERVAL_MILLIS: Long = TimeUnit.DAYS.toMillis(INTERVAL_DAYS.toLong())

    /**
     * True when a fetch is due: nothing was ever fetched, the last fetch was [INTERVAL_DAYS] or more days ago,
     * or the user picked an interest that has no saved articles yet. A saved time in the future (the phone's clock
     * was moved back) counts as due, so the list can never get stuck.
     */
    fun isDue(
        nowMillis: Long,
        lastRefreshAt: Long,
        selectedInterests: Set<String>,
        cachedInterests: Set<String>,
    ): Boolean = when {
        selectedInterests.isEmpty() -> false
        (selectedInterests - cachedInterests).isNotEmpty() -> true
        lastRefreshAt <= 0L -> true
        nowMillis < lastRefreshAt -> true
        else -> nowMillis - lastRefreshAt >= INTERVAL_MILLIS
    }
}
