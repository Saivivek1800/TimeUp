package com.vivek.reclaim.data.articles

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class FeedSource(val name: String, val url: String)

/** The curated feeds per interest, read from assets/feeds.json so they are easy to change (PRD FR-18). */
@Singleton
class FeedConfig @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val feeds: Map<String, List<FeedSource>> by lazy {
        val json = JSONObject(context.assets.open("feeds.json").bufferedReader().use { it.readText() })
        json.keys().asSequence().associateWith { interest ->
            val array = json.getJSONArray(interest)
            (0 until array.length()).map { i ->
                val entry = array.getJSONObject(i)
                FeedSource(entry.getString("name"), entry.getString("url"))
            }
        }
    }

    fun feedsFor(interest: String): List<FeedSource> = feeds[interest].orEmpty()
}
