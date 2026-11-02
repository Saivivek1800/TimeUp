package com.vivek.reclaim.data.usage

enum class UsageEventKind { RESUME, PAUSE, SCREEN_OFF }

/** A framework-free view of a UsageEvents event, so the maths can be unit-tested on the JVM. */
data class UsageEventLite(val timestamp: Long, val packageName: String?, val kind: UsageEventKind)

/** Turns foreground/background events into foreground time per app (PRD FR-7, FR-8). */
object UsageCalculator {

    /**
     * The app whose screen is in front at the end of [events] (sorted by time), or null if the screen is off or
     * nothing is known. The most recently resumed screen that has not paused since wins.
     */
    fun frontPackage(events: List<UsageEventLite>): String? {
        val resumed = LinkedHashSet<String>()
        for (event in events) {
            val pkg = event.packageName
            when (event.kind) {
                UsageEventKind.RESUME -> if (pkg != null) { resumed.remove(pkg); resumed.add(pkg) }
                UsageEventKind.PAUSE -> if (pkg != null) resumed.remove(pkg)
                UsageEventKind.SCREEN_OFF -> resumed.clear()
            }
        }
        return resumed.lastOrNull()
    }

    /**
     * Foreground milliseconds per package between [windowStart] and [windowEnd], for [packages] only.
     * [events] must be sorted by time.
     *
     * - An app can have several activities resumed at once; it counts as foreground until the last one pauses.
     * - Screen-off closes every open session.
     * - A pause that is the first event seen for an app means it was already in the foreground when the window began.
     * - Sessions still open at [windowEnd] run until [windowEnd].
     */
    fun foregroundMillis(
        events: List<UsageEventLite>,
        packages: Set<String>,
        windowStart: Long,
        windowEnd: Long,
    ): Map<String, Long> {
        val total = HashMap<String, Long>()
        val depth = HashMap<String, Int>()
        val openSince = HashMap<String, Long>()
        val seen = HashSet<String>()

        fun add(pkg: String, millis: Long) {
            if (millis > 0) total[pkg] = (total[pkg] ?: 0L) + millis
        }

        for (event in events) {
            val t = event.timestamp.coerceIn(windowStart, windowEnd)
            val pkg = event.packageName
            when (event.kind) {
                UsageEventKind.RESUME -> if (pkg != null && pkg in packages) {
                    seen += pkg
                    if ((depth[pkg] ?: 0) == 0) openSince[pkg] = t
                    depth[pkg] = (depth[pkg] ?: 0) + 1
                }

                UsageEventKind.PAUSE -> if (pkg != null && pkg in packages) {
                    val open = depth[pkg] ?: 0
                    if (open > 0) {
                        depth[pkg] = open - 1
                        if (open == 1) add(pkg, t - (openSince[pkg] ?: t))
                    } else if (pkg !in seen) {
                        add(pkg, t - windowStart)
                    }
                    seen += pkg
                }

                UsageEventKind.SCREEN_OFF -> for ((openPkg, count) in depth) {
                    if (count > 0) {
                        add(openPkg, t - (openSince[openPkg] ?: t))
                        depth[openPkg] = 0
                    }
                }
            }
        }
        for ((pkg, count) in depth) {
            if (count > 0) add(pkg, windowEnd - (openSince[pkg] ?: windowEnd))
        }
        return total
    }
}
