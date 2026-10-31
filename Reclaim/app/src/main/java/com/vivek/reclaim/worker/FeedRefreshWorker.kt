package com.vivek.reclaim.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.vivek.reclaim.data.articles.ArticleRepository
import com.vivek.reclaim.data.settings.SettingsRepository
import com.vivek.reclaim.domain.RefreshPolicy
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@EntryPoint
@InstallIn(SingletonComponent::class)
interface FeedWorkerEntryPoint {
    fun articles(): ArticleRepository
    fun settings(): SettingsRepository
}

/** Adds new articles to the cache in the background every 2 days, so the Block screen opens instantly (FR-21). */
class FeedRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val entry = EntryPointAccessors.fromApplication(applicationContext, FeedWorkerEntryPoint::class.java)
        val interests = entry.settings().settings.first().interests
        if (interests.isEmpty()) return Result.success()
        val reachedAnyFeed = entry.articles().refresh(interests)
        return if (reachedAnyFeed) Result.success() else Result.retry()
    }

    companion object {
        private const val NAME = "feed-refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<FeedRefreshWorker>(RefreshPolicy.INTERVAL_DAYS.toLong(), TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            // UPDATE, not KEEP: phones that already scheduled the old 6-hour job switch to the new interval.
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }
    }
}
