package com.vivek.reclaim.data.articles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssFetcher @Inject constructor() {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .callTimeout(25, TimeUnit.SECONDS)
        .build()

    /** Downloads a feed as text. Throws on network errors or a non-success response. */
    suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Android) Reclaim/1.0")
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml, */*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("HTTP ${response.code} for $url")
            val body = response.body ?: error("Empty response for $url")
            // Some feeds are megabytes of full-text posts; the first part holds the newest items.
            val source = body.source()
            source.request(MAX_BYTES)
            val size = minOf(source.buffer.size, MAX_BYTES)
            source.buffer.readUtf8(size)
        }
    }

    private companion object {
        const val MAX_BYTES = 1_500_000L
    }
}
