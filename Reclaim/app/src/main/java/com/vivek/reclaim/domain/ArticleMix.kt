package com.vivek.reclaim.domain

/** Shapes the "All" list so one chatty source or interest doesn't fill the whole feed. */
object ArticleMix {
    /**
     * Round-robins between the interests (in the given order) while keeping each interest's own order.
     * Items whose interest is not in [interestOrder] are dropped.
     */
    fun <T> interleave(items: List<T>, interestOrder: List<String>, interestOf: (T) -> String): List<T> {
        val queues = interestOrder.map { interest -> ArrayDeque(items.filter { interestOf(it) == interest }) }
        val result = ArrayList<T>(items.size)
        while (queues.any { it.isNotEmpty() }) {
            queues.forEach { queue -> queue.removeFirstOrNull()?.let(result::add) }
        }
        return result
    }
}
