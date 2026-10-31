package com.vivek.reclaim.data.articles

import com.vivek.reclaim.data.db.ArticleDao
import com.vivek.reclaim.data.db.ArticleEntity
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.domain.RefreshPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Articles for the Block screen and the Articles tab: cached in Room, refreshed from the curated feeds. */
@Singleton
class ArticleRepository @Inject constructor(
    private val dao: ArticleDao,
    private val feeds: FeedConfig,
    private val fetcher: RssFetcher,
    private val settings: SettingsRepository,
) {
    /** Unread first, then newest. Works offline because it only reads the local cache (FR-21, FR-22). */
    fun observe(interests: Set<String>): Flow<List<ArticleEntity>> = dao.observeByInterests(interests)

    suspend fun markRead(id: String) = dao.markRead(id)

    suspend fun hasCachedArticles(): Boolean = dao.count() > 0

    /**
     * Fetches new articles only when [RefreshPolicy] says it is time (about every 2 days, or when an interest
     * has nothing saved yet). Returns false only if a fetch was needed and no feed could be reached.
     */
    suspend fun refreshIfDue(interests: Set<String>, nowMillis: Long = System.currentTimeMillis()): Boolean {
        val due = RefreshPolicy.isDue(
            nowMillis = nowMillis,
            lastRefreshAt = settings.settings.first().articlesRefreshedAt,
            selectedInterests = interests,
            cachedInterests = dao.cachedInterests().toSet(),
        )
        return if (due) refresh(interests, nowMillis) else true
    }

    /**
     * Downloads every feed for [interests] and stores new articles, and remembers when. Returns true if at least
     * one feed answered, so callers can tell "no network" apart from "nothing new".
     */
    suspend fun refresh(interests: Set<String>, nowMillis: Long = System.currentTimeMillis()): Boolean =
        withContext(Dispatchers.IO) {
            val sources = interests.flatMap { interest -> feeds.feedsFor(interest).map { interest to it } }
            val results: List<List<ArticleEntity>?> = coroutineScope {
                sources.map { (interest, source) ->
                    async {
                        runCatching { RssParser.parse(fetcher.fetch(source.url)) }
                            .map { parsed -> parsed.map { toEntity(it, interest, source.name, nowMillis) } }
                            .getOrNull()
                    }
                }.awaitAll()
            }
            val fresh = results.filterNotNull().flatten()
            if (fresh.isNotEmpty()) dao.insertNew(fresh)
            // Keep the cache small: nothing older than a month, and read items go after two weeks.
            dao.deleteOlderThan(nowMillis - TimeUnit.DAYS.toMillis(30))
            dao.deleteReadOlderThan(nowMillis - TimeUnit.DAYS.toMillis(14))
            val reachedAnyFeed = results.any { it != null }
            if (reachedAnyFeed) settings.setArticlesRefreshedAt(nowMillis)
            reachedAnyFeed
        }

    private fun toEntity(parsed: ParsedArticle, interest: String, source: String, nowMillis: Long) = ArticleEntity(
        id = idFor(parsed.url),
        title = parsed.title,
        source = source,
        url = parsed.url,
        imageUrl = parsed.imageUrl,
        interest = interest,
        publishedAt = parsed.publishedAt?.coerceAtMost(nowMillis) ?: nowMillis,
        readMinutes = RssParser.readMinutes(parsed.wordCount),
        summary = parsed.summary,
    )

    private fun idFor(url: String): String =
        MessageDigest.getInstance("SHA-256").digest(url.toByteArray()).joinToString("") { "%02x".format(it) }.take(24)
}
