package com.vivek.reclaim.domain

import com.vivek.reclaim.domain.model.Interests
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Reads assets/feeds.json as plain text (org.json is not available in JVM tests) and checks it against the interests. */
class InterestFeedsTest {
    private val json: String = listOf("src/main/assets/feeds.json", "app/src/main/assets/feeds.json")
        .map(::File).first { it.exists() }.readText()

    /** interest -> list of feed urls, found by position of each quoted interest name. */
    private fun feedsByInterest(): Map<String, List<String>> {
        val starts = Regex("\"([A-Za-z ]+)\"\\s*:\\s*\\[").findAll(json).toList()
        return starts.mapIndexed { i, m ->
            val end = if (i + 1 < starts.size) starts[i + 1].range.first else json.length
            m.groupValues[1] to Regex("\"url\"\\s*:\\s*\"([^\"]+)\"").findAll(json.substring(m.range.last, end)).map { it.groupValues[1] }.toList()
        }.toMap()
    }

    @Test
    fun everyInterestHasAtLeastTwoFeeds() {
        val feeds = feedsByInterest()
        Interests.all.forEach { interest ->
            assertTrue("$interest has ${feeds[interest]?.size ?: 0} feeds, needs at least 2", (feeds[interest]?.size ?: 0) >= 2)
        }
    }

    @Test
    fun noFeedIsConfiguredForAnInterestThatCannotBePicked() {
        assertEquals(emptySet<String>(), feedsByInterest().keys - Interests.all.toSet())
    }

    @Test
    fun everyFeedUsesHttps() {
        feedsByInterest().values.flatten().forEach { assertTrue("$it is not https", it.startsWith("https://")) }
    }

    @Test
    fun interestNamesAreUnique() {
        assertEquals(Interests.all.size, Interests.all.toSet().size)
    }

    @Test
    fun thereAreTwentyInterests() {
        assertEquals(20, Interests.all.size)
    }
}
