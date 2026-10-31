package com.vivek.reclaim

import android.app.Application
import com.vivek.reclaim.worker.FeedRefreshWorker
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ReclaimApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FeedRefreshWorker.schedule(this)
    }
}
